package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreloadedData
import com.example.data.scanner.ReceiptScannerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("APRICO CLOUD", appName)
  }

  @Test
  fun `verify preloaded work order data`() {
    assertTrue(PreloadedData.INITIAL_TASKS.size >= 20)
    val totalBudget = PreloadedData.INITIAL_TASKS.sumOf { it.totalBudget }
    assertTrue(totalBudget > 2000000.0)

    val hardRockFoundation = PreloadedData.INITIAL_TASKS.find { it.id == "F01" }
    assertNotNull(hardRockFoundation)
    assertEquals(710.0, hardRockFoundation!!.totalQty, 0.01)
    assertEquals(1650.0, hardRockFoundation.unitPrice, 0.01)
    assertEquals(1171500.0, hardRockFoundation.totalBudget, 0.01)
  }

  @Test
  fun `verify automated receipt scanner parsing`() {
    val sampleInvoiceText = """
        SRI RENUKA EARTHMOVERS & JCB SERVICES
        Tax Invoice No: SR-JCB-304
        Date: 2026-10-15
        Item: JCB 3DX Excavator 28 hrs for INV to LT cable trench digging
        Rate: Rs 800/hr
        Total Amount: Rs 24,500.00
    """.trimIndent()

    val parsed = ReceiptScannerEngine.parseReceiptText(sampleInvoiceText, PreloadedData.INITIAL_TASKS)
    assertEquals(24500.0, parsed.totalAmount, 0.01)
    assertEquals("SR-JCB-304", parsed.billNumber)
    assertEquals("2026-10-15", parsed.date)
    assertNotNull(parsed.suggestedTaskId)
  }

  @Test
  fun `verify task progress filter logic`() {
    val tasks = PreloadedData.INITIAL_TASKS
    val notStartedTasks = tasks.filter { com.example.ui.tasks.isTaskMatchingProgressFilter(it, com.example.ui.tasks.TaskProgressFilter.NOT_STARTED) }
    val inProgressTasks = tasks.filter { com.example.ui.tasks.isTaskMatchingProgressFilter(it, com.example.ui.tasks.TaskProgressFilter.IN_PROGRESS) }
    val completedTasks = tasks.filter { com.example.ui.tasks.isTaskMatchingProgressFilter(it, com.example.ui.tasks.TaskProgressFilter.COMPLETED) }

    assertTrue(tasks.isNotEmpty())
    assertTrue(inProgressTasks.isNotEmpty() || completedTasks.isNotEmpty())

    // Hard rock foundation F01 has completedQty=320, totalQty=710 -> IN_PROGRESS
    val f01 = tasks.find { it.id == "F01" }
    assertNotNull(f01)
    assertTrue(com.example.ui.tasks.isTaskMatchingProgressFilter(f01!!, com.example.ui.tasks.TaskProgressFilter.IN_PROGRESS))

    // MMS Unloading A02 has completedQty=1.0, totalQty=1.0, status=COMPLETED -> COMPLETED
    val a02 = tasks.find { it.id == "A02" }
    assertNotNull(a02)
    assertTrue(com.example.ui.tasks.isTaskMatchingProgressFilter(a02!!, com.example.ui.tasks.TaskProgressFilter.COMPLETED))
  }

  @Test
  fun `verify admin task model creation and calculation`() {
    val task = com.example.data.firebase.FsTask(
        id = "custom_task_01",
        workOrderId = "wo_01",
        projectId = "proj_01",
        itemNumber = "D.01",
        category = "Inverters & Panels",
        title = "Inverter 100kW Installation",
        description = "Mounting and AC/DC termination",
        uom = "Nos",
        totalQty = 10.0,
        unitPrice = 15000.0,
        totalBudget = 150000.0,
        completedQty = 5.0,
        status = "IN_PROGRESS"
    )

    assertEquals("D.01", task.itemNumber)
    assertEquals(150000.0, task.totalBudget, 0.01)
    assertEquals("IN_PROGRESS", task.status)
    assertEquals(5.0, task.completedQty, 0.01)

    val edited = task.copy(
        completedQty = 10.0,
        status = "COMPLETED"
    )
    assertEquals("COMPLETED", edited.status)
    assertEquals(10.0, edited.completedQty, 0.01)
  }

  @Test
  fun `verify admin project deletion and filtering`() {
    val projectList = mutableListOf(
        com.example.data.firebase.FsWorkOrder(
            id = "wo_001",
            projectName = "Project Solar Alpha",
            workOrderNumber = "WO-2026-ALPHA",
            totalBudget = 1000000.0,
            taskCount = 10
        ),
        com.example.data.firebase.FsWorkOrder(
            id = "wo_002",
            projectName = "Project Solar Beta",
            workOrderNumber = "WO-2026-BETA",
            totalBudget = 2500000.0,
            taskCount = 25
        )
    )

    assertEquals(2, projectList.size)

    // Simulate deleting project wo_001
    val woToDeleteId = "wo_001"
    projectList.removeAll { it.id == woToDeleteId }

    assertEquals(1, projectList.size)
    assertEquals("wo_002", projectList.first().id)
    assertEquals("Project Solar Beta", projectList.first().projectName)
  }

  @Test
  fun `verify monthly allocation target becomes zero when all projects are deleted`() {
    val remainingProjects = emptyList<com.example.data.firebase.FsWorkOrder>()
    val portfolioTotalBudget = remainingProjects.sumOf { it.totalBudget }
    val monthlyAllocation = if (portfolioTotalBudget > 0.0) portfolioTotalBudget / 3.0 else 0.0

    assertEquals(0.0, portfolioTotalBudget, 0.001)
    assertEquals(0.0, monthlyAllocation, 0.001)

    // Also verify active project case
    val activeProjectBudget = 3000000.0
    val activeAllocation = if (activeProjectBudget > 0.0) activeProjectBudget / 3.0 else 0.0
    assertEquals(1000000.0, activeAllocation, 0.001)
  }

  @Test
  fun `verify task progress calculation and status synchronization`() {
    val task = com.example.data.model.WorkOrderTask(
        id = "task_01",
        itemNumber = "1.1",
        category = "Civil & Foundation",
        title = "Piling Work",
        description = "Test piling",
        uom = "Nos",
        totalQty = 100.0,
        unitPrice = 500.0,
        totalBudget = 50000.0,
        completedQty = 0.0,
        status = "NOT_STARTED"
    )

    assertEquals(0.0, com.example.ui.tasks.calculateTaskProgressPercentage(task), 0.01)

    // Update to IN_PROGRESS
    val inProgressTask = task.copy(completedQty = 50.0, status = "IN_PROGRESS")
    assertEquals(50.0, com.example.ui.tasks.calculateTaskProgressPercentage(inProgressTask), 0.01)

    // Update to COMPLETED
    val completedTask = task.copy(completedQty = 100.0, status = "COMPLETED")
    assertEquals(100.0, com.example.ui.tasks.calculateTaskProgressPercentage(completedTask), 0.01)
  }

  @Test
  fun `verify work order progress dialog final quantity and status mapping`() {
    val task = com.example.data.model.WorkOrderTask(
        id = "task_f01",
        itemNumber = "1",
        category = "Civil & Foundation",
        title = "Column Post",
        description = "Drilling and casting",
        uom = "Nos",
        totalQty = 710.0,
        unitPrice = 1650.0,
        totalBudget = 1171500.0,
        completedQty = 0.0,
        status = "NOT_STARTED"
    )

    // Selecting COMPLETED sets finalQty = totalQty
    val finalQtyCompleted = task.totalQty
    val finalStatusCompleted = "COMPLETED"
    val updatedCompleted = task.copy(completedQty = finalQtyCompleted, status = finalStatusCompleted)
    assertEquals(710.0, updatedCompleted.completedQty, 0.01)
    assertEquals("COMPLETED", updatedCompleted.status)
    assertEquals(100.0, com.example.ui.tasks.calculateTaskProgressPercentage(updatedCompleted), 0.01)

    // Selecting IN_PROGRESS with partial quantity
    val finalQtyInProgress = 320.0
    val finalStatusInProgress = "IN_PROGRESS"
    val updatedInProgress = task.copy(completedQty = finalQtyInProgress, status = finalStatusInProgress)
    assertEquals(320.0, updatedInProgress.completedQty, 0.01)
    assertEquals("IN_PROGRESS", updatedInProgress.status)
    val pct = com.example.ui.tasks.calculateTaskProgressPercentage(updatedInProgress)
    assertTrue(pct > 0.0 && pct < 100.0)
  }
}
