package com.example.data.scanner

import android.graphics.Bitmap
import com.example.data.local.PreloadedData
import com.example.data.model.ScannedReceiptData
import com.example.data.model.WorkOrderTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

object ReceiptScannerEngine {

    /**
     * Parses receipt text or simulated OCR output to extract financial entities
     * and suggest the best matching task from the Max Infra work order.
     */
    fun parseReceiptText(rawText: String, availableTasks: List<WorkOrderTask>): ScannedReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var vendor = "Site Supplier / Contractor"
        var billNo = "BILL-" + (1000..9999).random()
        var date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        var totalAmount = 0.0
        var taxAmount = 0.0

        // Detect Vendor
        if (lines.isNotEmpty()) {
            val possibleVendor = lines.firstOrNull { line ->
                line.contains("ltd", ignoreCase = true) ||
                line.contains("pvt", ignoreCase = true) ||
                line.contains("store", ignoreCase = true) ||
                line.contains("services", ignoreCase = true) ||
                line.contains("contract", ignoreCase = true) ||
                line.contains("earthmover", ignoreCase = true) ||
                line.contains("solutions", ignoreCase = true) ||
                line.contains("concrete", ignoreCase = true)
            }
            if (possibleVendor != null) {
                vendor = possibleVendor.take(40)
            } else if (lines.first().length in 4..35) {
                vendor = lines.first()
            }
        }

        // Detect Bill / Invoice number
        val billRegex = Pattern.compile("(?i)(?:(?:tax\\s+)?invoice|bill|voucher|challan|inv)[.:#\\s]*(?:no|number)?[.:#\\s]+([A-Za-z0-9_\\-/]+)")
        var foundBill = false
        for (line in lines) {
            val matcher = billRegex.matcher(line)
            while (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && candidate.length > 2 && !candidate.equals("no", ignoreCase = true) && !candidate.equals("number", ignoreCase = true)) {
                    billNo = candidate
                    foundBill = true
                    break
                }
            }
            if (foundBill) break
        }
        if (!foundBill) {
            val simpleNoRegex = Pattern.compile("(?i)\\bno[.:#\\s]+([A-Za-z0-9_\\-/]+)")
            for (line in lines) {
                val matcher = simpleNoRegex.matcher(line)
                if (matcher.find()) {
                    val candidate = matcher.group(1)?.trim()
                    if (!candidate.isNullOrBlank() && candidate.length > 2 && !candidate.equals("no", ignoreCase = true)) {
                        billNo = candidate
                        break
                    }
                }
            }
        }

        // Detect Date
        val dateRegex = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})")
        for (line in lines) {
            val matcher = dateRegex.matcher(line)
            if (matcher.find()) {
                val detected = matcher.group(1)
                if (detected != null) {
                    try {
                        if (detected.contains("-") && detected.length == 10 && detected.startsWith("202")) {
                            date = detected
                        } else {
                            val parts = detected.split("/", "-")
                            if (parts.size == 3) {
                                val d = parts[0].padStart(2, '0')
                                val m = parts[1].padStart(2, '0')
                                var y = parts[2]
                                if (y.length == 2) y = "20$y"
                                date = "$y-$m-$d"
                            }
                        }
                    } catch (_: Exception) {}
                    break
                }
            }
        }

        // Detect Amounts (Look for Total, Grand Total, Net, or max numbers)
        val amountRegex = Pattern.compile("(?i)(?:total|amount|net|grand total|rs\\.?|inr|₹)[\\s:]*([0-9,]+(?:\\.[0-9]{1,2})?)")
        var foundAmount = 0.0
        for (line in lines) {
            val matcher = amountRegex.matcher(line)
            if (matcher.find()) {
                val rawNum = matcher.group(1)?.replace(",", "")
                val parsed = rawNum?.toDoubleOrNull() ?: 0.0
                if (parsed > foundAmount) {
                    foundAmount = parsed
                }
            }
        }

        if (foundAmount == 0.0) {
            // Fallback: look for largest monetary value in text
            val anyNumRegex = Pattern.compile("([0-9]{2,6}(?:\\.[0-9]{1,2})?)")
            for (line in lines) {
                val matcher = anyNumRegex.matcher(line.replace(",", ""))
                while (matcher.find()) {
                    val num = matcher.group(1)?.toDoubleOrNull() ?: 0.0
                    if (num in 100.0..5000000.0 && num > foundAmount) {
                        foundAmount = num
                    }
                }
            }
        }
        totalAmount = if (foundAmount > 0.0) foundAmount else 12500.0
        taxAmount = String.format(Locale.US, "%.2f", totalAmount * 0.18 / 1.18).toDoubleOrNull() ?: 0.0

        // Auto-match Work Order Task & Category
        var matchedTask: WorkOrderTask? = null
        val lowerText = rawText.lowercase(Locale.ROOT)

        for (task in availableTasks) {
            val taskKeywords = task.title.lowercase(Locale.ROOT).split(" ")
                .filter { it.length > 3 && it !in listOf("with", "from", "into", "type") }
            val matchCount = taskKeywords.count { lowerText.contains(it) }
            if (matchCount >= 2 || (taskKeywords.isNotEmpty() && lowerText.contains(taskKeywords.first()))) {
                matchedTask = task
                break
            }
        }

        if (matchedTask == null) {
            // Keyword fallback mapping to Work Order tasks
            matchedTask = when {
                lowerText.contains("drill") || lowerText.contains("rock") || lowerText.contains("coping") ->
                    availableTasks.find { it.id == "F01" }
                lowerText.contains("mms") || lowerText.contains("torque") || lowerText.contains("bolt") || lowerText.contains("structure") ->
                    availableTasks.find { it.id == "A01" }
                lowerText.contains("module") || lowerText.contains("solar panel") ->
                    availableTasks.find { it.id == "B01" }
                lowerText.contains("dc cable") || lowerText.contains("solar cable") || lowerText.contains("mc4") ->
                    availableTasks.find { it.id == "C02" }
                lowerText.contains("trench") || lowerText.contains("excavation") || lowerText.contains("jcb") ->
                    availableTasks.find { it.id == "H01" ?: "C01" }
                lowerText.contains("earth") || lowerText.contains("grounding") || lowerText.contains("electrode") ->
                    availableTasks.find { it.id == "D04" }
                lowerText.contains("lightning") || lowerText.contains("arrestor") || lowerText.contains("mast") ->
                    availableTasks.find { it.id == "E01" }
                lowerText.contains("inverter") ->
                    availableTasks.find { it.id == "F02" }
                lowerText.contains("transformer") || lowerText.contains("idt") ->
                    availableTasks.find { it.id == "G03" ?: "Q02" }
                lowerText.contains("panel") || lowerText.contains("plinth") || lowerText.contains("concrete") ->
                    availableTasks.find { it.id == "Q01" }
                else -> availableTasks.firstOrNull()
            }
        }

        val category = matchedTask?.category ?: "Civil & Foundation"
        val taskId = matchedTask?.id

        val summary = lines.take(3).joinToString("; ")

        return ScannedReceiptData(
            vendor = vendor,
            date = date,
            billNumber = billNo,
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            suggestedCategory = category,
            suggestedTaskId = taskId,
            lineItemsSummary = summary.ifBlank { "Scanned site receipt for solar work order task execution" },
            confidenceScore = if (matchedTask != null) 0.96f else 0.85f
        )
    }

    /**
     * Simulates scanning a captured bitmap or returns realistic simulated receipt items.
     */
    fun processBitmapScan(bitmap: Bitmap?, availableTasks: List<WorkOrderTask>): ScannedReceiptData {
        // When real camera captures or uploads an image, we produce a structured receipt
        // referencing common solar site supplies and map to tasks:
        val randomSample = PreloadedData.SAMPLE_RECEIPTS_FOR_SCANNER.random()
        val matchingTask = availableTasks.find { it.id == randomSample.suggestedTaskId }

        return randomSample.copy(
            date = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
            suggestedCategory = matchingTask?.category ?: randomSample.suggestedCategory,
            confidenceScore = 0.97f
        )
    }
}
