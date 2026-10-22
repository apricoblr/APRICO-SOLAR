package com.example.data.local

import com.example.data.model.Expense
import com.example.data.model.ScannedReceiptData
import com.example.data.model.WorkOrderTask

object PreloadedData {

    const val WORK_ORDER_NO = "Maxinfra/KUSUM Solar/Devarahipparagi/2026-27/0003"
    const val WORK_ORDER_DATE = "13-08-2026"
    const val CLIENT_NAME = "M/s MAX INFRA (I) PRIVATE LTD"
    const val CONTRACTOR_NAME = "APRICOR (Arun Ravulapalli Kumar)"
    const val PROJECT_NAME = "KUSUM Solar / Devarahipparagi 1.88 MW Solar Plant"
    const val PROJECT_LOCATION = "Devarahipparagi, Karnataka (SS Level Component C)"
    const val TOTAL_BASE_BUDGET = 3243160.0
    const val GST_PERCENTAGE = 18.0
    const val GST_AMOUNT = 583768.80
    const val GRAND_TOTAL_BUDGET = 3826928.80
    const val CONTRACT_DURATION_DAYS = 45

    val INITIAL_TASKS = listOf(
        // Foundation
        WorkOrderTask(
            id = "F01",
            itemNumber = "1",
            category = "Civil & Foundation",
            title = "Column Post in Hard Rock",
            description = "Drilling of hole 300mm dia x 1.5m depth in hard rock, casting column post, coping, alignment & C25 grade concrete",
            uom = "Nos",
            totalQty = 710.0,
            unitPrice = 1650.0,
            totalBudget = 1171500.0,
            completedQty = 320.0,
            status = "IN_PROGRESS"
        ),
        // MMS Erection
        WorkOrderTask(
            id = "A01",
            itemNumber = "A.1",
            category = "MMS & Modules",
            title = "MMS Erection & Assembly",
            description = "Fixing MMS structure (Full table 60 modules / Half table 30 modules, 2Px30 / 2Px15), torque tightening & marking",
            uom = "MT",
            totalQty = 30.0,
            unitPrice = 4200.0,
            totalBudget = 126000.0,
            completedQty = 12.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "A02",
            itemNumber = "A.2",
            category = "MMS & Modules",
            title = "MMS Unloading & Shifting",
            description = "Unloading & shifting MMS structure from central store to location inside yard",
            uom = "Lot",
            totalQty = 1.0,
            unitPrice = 15000.0,
            totalBudget = 15000.0,
            completedQty = 1.0,
            status = "COMPLETED"
        ),
        // PV Modules Erection
        WorkOrderTask(
            id = "B01",
            itemNumber = "B.1",
            category = "MMS & Modules",
            title = "PV Modules Erection",
            description = "Installation of PV modules on MMS as per approved drawing and torque check",
            uom = "Nos",
            totalQty = 3584.0,
            unitPrice = 32.0,
            totalBudget = 114688.0,
            completedQty = 800.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "B02",
            itemNumber = "B.2",
            category = "MMS & Modules",
            title = "PV Modules Unloading & Shifting",
            description = "Careful unloading from container & movement of PV modules at site without microcracks",
            uom = "Lot",
            totalQty = 1.0,
            unitPrice = 18000.0,
            totalBudget = 18000.0,
            completedQty = 1.0,
            status = "COMPLETED"
        ),
        // DC Cabling
        WorkOrderTask(
            id = "C01",
            itemNumber = "C.5",
            category = "DC Cabling",
            title = "DC Cable Trench Excavation",
            description = "Excavation of soil for DC cable trench up to depth 500mm x 600mm width, dressing & backfilling",
            uom = "Mtr",
            totalQty = 510.0,
            unitPrice = 150.0,
            totalBudget = 76500.0,
            completedQty = 260.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "C02",
            itemNumber = "C.6",
            category = "DC Cabling",
            title = "DC Cable Laying (1Cx4 / 1Cx6 mm²)",
            description = "Laying string cable 1Cx4/1Cx6 Cu FRLS Solar Cable in DWC conduit / buried as per site condition",
            uom = "Mtr",
            totalQty = 9500.0,
            unitPrice = 6.0,
            totalBudget = 57000.0,
            completedQty = 3000.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "C03",
            itemNumber = "C.7",
            category = "DC Cabling",
            title = "MDPE / DWC Conduit Fixing",
            description = "Fixing of MDPE / DWC conduit clamps, rings, and string cable ties",
            uom = "Mtr",
            totalQty = 1200.0,
            unitPrice = 4.0,
            totalBudget = 4800.0,
            completedQty = 400.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "C04",
            itemNumber = "C.8",
            category = "DC Cabling",
            title = "RCC Hume Pipe for Road Crossing",
            description = "Supply & installation of RCC Hume pipe 300mm dia NP3 for road drain crossings",
            uom = "Mtr",
            totalQty = 4.0,
            unitPrice = 2100.0,
            totalBudget = 8400.0,
            completedQty = 4.0,
            status = "COMPLETED"
        ),
        WorkOrderTask(
            id = "C05",
            itemNumber = "C.9",
            category = "DC Cabling",
            title = "DC Cable Route Markers",
            description = "Erection of Cable route markers with 18 SWG engraved plate '1.5KV DC CABLES'",
            uom = "Nos",
            totalQty = 30.0,
            unitPrice = 70.0,
            totalBudget = 2100.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "C06",
            itemNumber = "C.10",
            category = "DC Cabling",
            title = "String Numbering & Identification",
            description = "String numbering (paint / punching) as per electrical single line diagram",
            uom = "Nos",
            totalQty = 300.0,
            unitPrice = 45.0,
            totalBudget = 13500.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // Earthing
        WorkOrderTask(
            id = "D01",
            itemNumber = "D.11",
            category = "Earthing & LA",
            title = "GI Earth Strip 25x6mm (Soil Excv)",
            description = "Laying & connectivity of GI Earth strip 25x6mm inclusive of excavation & backfill",
            uom = "Mtr",
            totalQty = 550.0,
            unitPrice = 70.0,
            totalBudget = 38500.0,
            completedQty = 200.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "D02",
            itemNumber = "D.12",
            category = "Earthing & LA",
            title = "GI Earth Strip 25x6mm (Trench)",
            description = "GI 25x6mm Earthing strip laid above ground / along trench layout",
            uom = "Mtr",
            totalQty = 950.0,
            unitPrice = 70.0,
            totalBudget = 66500.0,
            completedQty = 350.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "D03",
            itemNumber = "D.13",
            category = "Earthing & LA",
            title = "GI Earth Strip 25x3mm",
            description = "GI 25x3mm Earthing strip laid above ground / along yard perimeter",
            uom = "Mtr",
            totalQty = 510.0,
            unitPrice = 60.0,
            totalBudget = 30600.0,
            completedQty = 150.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "D04",
            itemNumber = "D.14",
            category = "Earthing & LA",
            title = "DC Earthing Pit Installation",
            description = "Installation of DC Earthing Pit: 17.2mm dia 3000mm Cu bonded electrode + chemical compound + 450x450mm chamber",
            uom = "Nos",
            totalQty = 23.0,
            unitPrice = 5000.0,
            totalBudget = 115000.0,
            completedQty = 10.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "D05",
            itemNumber = "D.15",
            category = "Earthing & LA",
            title = "Earthing Name Plates & Tags",
            description = "Engraved brass/acrylic earth pit identification tags and warning signages",
            uom = "Nos",
            totalQty = 30.0,
            unitPrice = 39.5,
            totalBudget = 1185.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "D06",
            itemNumber = "D.16",
            category = "Earthing & LA",
            title = "11KV 1Cx70mm² Inverter Grounding Cable",
            description = "Yellow-green insulated Cu cable connecting String Inverters to earth bus",
            uom = "Mtr",
            totalQty = 58.0,
            unitPrice = 44.1,
            totalBudget = 2560.0,
            completedQty = 20.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "D07",
            itemNumber = "D.17",
            category = "Earthing & LA",
            title = "Module to Pit Earthing Links",
            description = "Flexible Cu links with SS serrated washers & bolts for PV module frame bonding",
            uom = "Sets",
            totalQty = 120.0,
            unitPrice = 10.0,
            totalBudget = 1200.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // Lightning Protection
        WorkOrderTask(
            id = "E01",
            itemNumber = "E.18",
            category = "Earthing & LA",
            title = "ESE Lightning Arrestors & 8m Mast",
            description = "ESE type lightning arrestors with guy wire kit, 8m GI pipe mast, test clamp & down conductor",
            uom = "Nos",
            totalQty = 2.0,
            unitPrice = 3000.0,
            totalBudget = 6000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "E02",
            itemNumber = "E.19",
            category = "Earthing & LA",
            title = "LA Dedicated Earthing Pits",
            description = "Installation of LA Earthing Pit 17.2mm dia 3000mm Cu electrode + chamber + chemical bag",
            uom = "Nos",
            totalQty = 4.0,
            unitPrice = 5000.0,
            totalBudget = 20000.0,
            completedQty = 2.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "E03",
            itemNumber = "E.21",
            category = "Earthing & LA",
            title = "LA Mast & Guy Wire Foundations",
            description = "M25 concrete foundation for LA mast and guy wire anchor points",
            uom = "Sets",
            totalQty = 2.0,
            unitPrice = 6000.0,
            totalBudget = 12000.0,
            completedQty = 1.0,
            status = "IN_PROGRESS"
        ),
        // String Inverter
        WorkOrderTask(
            id = "F01",
            itemNumber = "F.22",
            category = "Inverters & Panels",
            title = "String Inverter Pile Foundation",
            description = "Construction of pile foundations 300mm dia x 1000mm depth M25 concrete with pedestal cap",
            uom = "Nos",
            totalQty = 14.0,
            unitPrice = 1500.0,
            totalBudget = 21000.0,
            completedQty = 8.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "F02",
            itemNumber = "F.23",
            category = "Inverters & Panels",
            title = "String Inverter Installation & Mounting",
            description = "Installation of 7 String Inverters on structure, torque checking & removal of lifting bars",
            uom = "Nos",
            totalQty = 7.0,
            unitPrice = 2000.0,
            totalBudget = 14000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "F03",
            itemNumber = "F.24",
            category = "Inverters & Panels",
            title = "Inverter Communication Box Mounting",
            description = "Installation of String Inverter communication box & termination accessories",
            uom = "Nos",
            totalQty = 1.0,
            unitPrice = 1500.0,
            totalBudget = 1500.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // HT & IDT Panels
        WorkOrderTask(
            id = "G01",
            itemNumber = "G.25",
            category = "Inverters & Panels",
            title = "Main LT Panel Erection & Commissioning",
            description = "Installation & commissioning of Main LT panel with ACB panel / inverter entry",
            uom = "Nos",
            totalQty = 1.0,
            unitPrice = 15000.0,
            totalBudget = 15000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "G02",
            itemNumber = "G.26",
            category = "Inverters & Panels",
            title = "Main HT Panel Installation (11kV 630A)",
            description = "Installation & testing of Main HT Panel 1COG outdoor type IP55 11kV 630A VCB",
            uom = "Nos",
            totalQty = 1.0,
            unitPrice = 15000.0,
            totalBudget = 15000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "G03",
            itemNumber = "G.27",
            category = "Inverters & Panels",
            title = "6.5 MVA 11kV/0.8kV IDT Transformer",
            description = "Erection of 6.5 MVA ONAN Inverter Duty Transformer with OLTC, LV cable box & oil testing",
            uom = "Nos",
            totalQty = 1.0,
            unitPrice = 45000.0,
            totalBudget = 45000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "G04",
            itemNumber = "G.28",
            category = "Inverters & Panels",
            title = "UPS, UPSDB & Battery Bank",
            description = "Installation of auxiliary backup UPS, distribution board & battery rack",
            uom = "Nos",
            totalQty = 1.0,
            unitPrice = 5000.0,
            totalBudget = 5000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "G05",
            itemNumber = "G.29",
            category = "Inverters & Panels",
            title = "ACDB Panel Installation",
            description = "Mounting and connection of AC distribution board with all accessories",
            uom = "Nos",
            totalQty = 1.0,
            unitPrice = 8000.0,
            totalBudget = 8000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "G06",
            itemNumber = "G.30",
            category = "Inverters & Panels",
            title = "Metering Cubical Installation",
            description = "Installation of DISCOM grid export metering cubicle and CT/PT connections",
            uom = "Nos",
            totalQty = 1.0,
            unitPrice = 10000.0,
            totalBudget = 10000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // Cable Trench between INV to LT
        WorkOrderTask(
            id = "H01",
            itemNumber = "H.31",
            category = "AC Cabling & Trench",
            title = "INV to LT Cable Trench Soil Excavation",
            description = "Excavation 1200mm depth x 600mm width, 100mm sand bed, warning tapes, brick cover & backfilling",
            uom = "Rmtr",
            totalQty = 570.0,
            unitPrice = 320.0,
            totalBudget = 182400.0,
            completedQty = 180.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "H02",
            itemNumber = "H.32",
            category = "AC Cabling & Trench",
            title = "Cable Trench Hard Rock Laying",
            description = "Trench cutting in hard rock zones, protective concrete bedding & dressing",
            uom = "Nos",
            totalQty = 100.0,
            unitPrice = 100.0,
            totalBudget = 10000.0,
            completedQty = 40.0,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "H03",
            itemNumber = "H.33",
            category = "AC Cabling & Trench",
            title = "3C x 300 mm² 1.1/3.3kV AC Cable Laying",
            description = "Laying 3C x 300 mm² XLPE AL armored cable in trench between inverter stations and LT panel",
            uom = "Mtr",
            totalQty = 1000.0,
            unitPrice = 110.0,
            totalBudget = 110000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "H04",
            itemNumber = "H.34",
            category = "AC Cabling & Trench",
            title = "1C x 300 mm² Cable & Trefoil Clamps",
            description = "Laying single core 300 mm² XLPE cables in trefoil formation with non-magnetic clamps",
            uom = "Mtr",
            totalQty = 216.0,
            unitPrice = 30.0,
            totalBudget = 6480.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "H05",
            itemNumber = "H.35",
            category = "AC Cabling & Trench",
            title = "RCC Hume Pipe for INV-LT Trench",
            description = "300mm dia NP3 pipe for drainage and road cross sections",
            uom = "Mtr",
            totalQty = 4.0,
            unitPrice = 2100.0,
            totalBudget = 8400.0,
            completedQty = 4.0,
            status = "COMPLETED"
        ),
        // HT Cable
        WorkOrderTask(
            id = "K01",
            itemNumber = "K.40",
            category = "HT Cabling",
            title = "3C x 240 mm² 11kV Cable Laying",
            description = "Laying & pulling of 11kV grade 3C x 240 mm² XLPE armored cable to evacuation switchyard",
            uom = "Mtr",
            totalQty = 46.0,
            unitPrice = 150.0,
            totalBudget = 6900.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "K02",
            itemNumber = "K.41",
            category = "HT Cabling",
            title = "11kV Indoor & Outdoor Termination Kits",
            description = "Heat shrinkable termination kits (5 indoor @ 2500, 1 outdoor @ 3000) with route markers",
            uom = "Sets",
            totalQty = 6.0,
            unitPrice = 2900.0,
            totalBudget = 17400.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // Aux & Control Cables
        WorkOrderTask(
            id = "L01",
            itemNumber = "L.46",
            category = "Aux & Control Cables",
            title = "LT Auxiliary Power & Yard Light Cables",
            description = "Laying and glanding 4C x 16 mm² & 3C x 2.5 mm² cables for perimeter lighting and auxiliary supply",
            uom = "Mtr",
            totalQty = 1461.0,
            unitPrice = 24.07,
            totalBudget = 35177.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "N01",
            itemNumber = "N.52",
            category = "Aux & Control Cables",
            title = "Control & Communication Cables",
            description = "Laying CAT6 Ethernet, RS485 shielded, and 19C x 2.5 mm² multicore control cables for SCADA",
            uom = "Mtr",
            totalQty = 460.0,
            unitPrice = 11.2,
            totalBudget = 5150.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // Outdoor Earthing System
        WorkOrderTask(
            id = "O01",
            itemNumber = "O.55",
            category = "Earthing & LA",
            title = "Yard & Transformer Outdoor Earthing Grid",
            description = "GI strip 75x8mm & 50x6mm, Cu strip 50x6mm, 16 Nos chemical earth pipes with chambers",
            uom = "Lot",
            totalQty = 1.0,
            unitPrice = 88420.0,
            totalBudget = 88420.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // WMS Installation
        WorkOrderTask(
            id = "P01",
            itemNumber = "P.62",
            category = "WMS & Ancillary",
            title = "Weather Monitoring Station (WMS)",
            description = "Installation of Pyranometers, Module temp sensor, Ambient sensor, Anemometer, Wind vane, Data logger & civil pedestal",
            uom = "Lot",
            totalQty = 1.0,
            unitPrice = 10000.0,
            totalBudget = 10000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        // Major Civil Foundations
        WorkOrderTask(
            id = "Q01",
            itemNumber = "Q.1",
            category = "Civil & Foundation",
            title = "LT & HT Panel Foundation",
            description = "RCC foundation including civil excavation, shuttering, reinforcement & plinth beam as per drawing",
            uom = "No",
            totalQty = 1.0,
            unitPrice = 430000.0,
            totalBudget = 430000.0,
            completedQty = 0.5,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "Q02",
            itemNumber = "Q.2",
            category = "Civil & Foundation",
            title = "IDT Transformer Foundation",
            description = "Heavy equipment RCC foundation with oil soak pit & gravel filling",
            uom = "No",
            totalQty = 1.0,
            unitPrice = 160000.0,
            totalBudget = 160000.0,
            completedQty = 0.3,
            status = "IN_PROGRESS"
        ),
        WorkOrderTask(
            id = "Q03",
            itemNumber = "Q.3",
            category = "Civil & Foundation",
            title = "Fencing Around IDT with Gate",
            description = "Brick boundary wall (3 ft height x 0.1m) + chainlink mesh (1.5m) and steel gate",
            uom = "Lot",
            totalQty = 1.0,
            unitPrice = 95000.0,
            totalBudget = 95000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        ),
        WorkOrderTask(
            id = "Q04",
            itemNumber = "Q.4",
            category = "Civil & Foundation",
            title = "Metering Cubical & Container Foundations",
            description = "Civil foundation for metering cubical (₹12k) and control room container foundation (₹50k)",
            uom = "Lot",
            totalQty = 1.0,
            unitPrice = 62000.0,
            totalBudget = 62000.0,
            completedQty = 0.0,
            status = "NOT_STARTED"
        )
    )

    val INITIAL_EXPENSES = listOf(
        Expense(
            taskId = "F01",
            taskTitle = "Column Post in Hard Rock",
            category = "Civil & Foundation",
            amount = 145000.0,
            date = "2026-08-18",
            monthYear = "2026-08",
            vendorName = "Bhavani DTH Drilling Rig Services",
            billNumber = "BDR-2026/041",
            expenseType = "Machinery/Fuel",
            paymentMode = "Bank Transfer",
            notes = "DTH drilling 120 holes in hard rock zone A. Diesel included."
        ),
        Expense(
            taskId = "F01",
            taskTitle = "Column Post in Hard Rock",
            category = "Civil & Foundation",
            amount = 188500.0,
            date = "2026-08-25",
            monthYear = "2026-08",
            vendorName = "UltraTech ReadyMix Concrete Co.",
            billNumber = "UTC-INV-8832",
            expenseType = "Material",
            paymentMode = "Bank Transfer",
            notes = "Supply of M25 concrete (28 cu.m) for column post coping & casting"
        ),
        Expense(
            taskId = "A02",
            taskTitle = "MMS Unloading & Shifting",
            category = "MMS & Modules",
            amount = 14000.0,
            date = "2026-08-28",
            monthYear = "2026-08",
            vendorName = "Sri Sai Crane & Hydra Services",
            billNumber = "SS-CRN-19",
            expenseType = "Machinery/Fuel",
            paymentMode = "UPI",
            notes = "Hydra 14T crane rental for unloading 30 MT MMS structure trailers"
        ),
        Expense(
            taskId = "C01",
            taskTitle = "DC Cable Trench Excavation",
            category = "DC Cabling",
            amount = 32000.0,
            date = "2026-09-04",
            monthYear = "2026-09",
            vendorName = "Mahalaxmi JCB Earthmovers",
            billNumber = "MLX-JCB-210",
            expenseType = "Machinery/Fuel",
            paymentMode = "Bank Transfer",
            notes = "JCB 3DX rental for 260m DC cable trench digging & dressing"
        ),
        Expense(
            taskId = "D04",
            taskTitle = "DC Earthing Pit Installation",
            category = "Earthing & LA",
            amount = 45000.0,
            date = "2026-09-10",
            monthYear = "2026-09",
            vendorName = "Aditi Grounding & Chemical Compounds",
            billNumber = "AGC-0994",
            expenseType = "Material",
            paymentMode = "Bank Transfer",
            notes = "Supply of 10 Cu bonded electrodes & 20 bags BFC chemical compound"
        ),
        Expense(
            taskId = "Q01",
            taskTitle = "LT & HT Panel Foundation",
            category = "Civil & Foundation",
            amount = 195000.0,
            date = "2026-09-18",
            monthYear = "2026-09",
            vendorName = "Siddeshwara Steel & Cement Store",
            billNumber = "SCS-BLR-554",
            expenseType = "Material",
            paymentMode = "Bank Transfer",
            notes = "Fe550D TMT reinforcement rebar (4.5 MT) + 120 bags 53G cement for HT/LT plinth"
        ),
        Expense(
            taskId = "A01",
            taskTitle = "MMS Erection & Assembly",
            category = "MMS & Modules",
            amount = 48000.0,
            date = "2026-09-24",
            monthYear = "2026-09",
            vendorName = "Om Shanti Labor Contracting",
            billNumber = "OSL-LAB-08",
            expenseType = "Labor",
            paymentMode = "Bank Transfer",
            notes = "Assembly team of 8 fitters for 12 MT MMS torque fixing (Week 1)"
        ),
        Expense(
            taskId = "H01",
            taskTitle = "INV to LT Cable Trench Soil Excavation",
            category = "AC Cabling & Trench",
            amount = 52000.0,
            date = "2026-10-02",
            monthYear = "2026-10",
            vendorName = "Basava Trenching Gang",
            billNumber = "BTG-OCT-01",
            expenseType = "Labor",
            paymentMode = "Cash",
            notes = "Manual trench excavation and sand cushioning 180 Rmtr"
        )
    )

    val SAMPLE_RECEIPTS_FOR_SCANNER: List<ScannedReceiptData> = listOf(
        ScannedReceiptData(
            vendor = "Sri Renuka Earthmovers & JCB",
            date = "2026-10-15",
            billNumber = "SR-JCB-304",
            totalAmount = 24500.0,
            taxAmount = 3737.0,
            suggestedCategory = "AC Cabling & Trench",
            suggestedTaskId = "H01",
            lineItemsSummary = "JCB Excavator 28 hrs @ 800/hr + Diesel for INV-LT Trench digging",
            confidenceScore = 0.98f
        ),
        ScannedReceiptData(
            vendor = "Maruti Hardware & Fasteners Depot",
            date = "2026-10-16",
            billNumber = "MHF-8821",
            totalAmount = 18450.0,
            taxAmount = 2814.0,
            suggestedCategory = "MMS & Modules",
            suggestedTaskId = "A01",
            lineItemsSummary = "SS304 M10 x 30mm Allen Bolts (500 Nos) + Serrated Flange Nuts for MMS",
            confidenceScore = 0.96f
        ),
        ScannedReceiptData(
            vendor = "Bharat Chemical & Earthing Solutions",
            date = "2026-10-17",
            billNumber = "BCES-2026-11",
            totalAmount = 36000.0,
            taxAmount = 5491.0,
            suggestedCategory = "Earthing & LA",
            suggestedTaskId = "D04",
            lineItemsSummary = "8 Nos 3000mm Copper Bonded Electrodes + 16 Bags Conductive Compound",
            confidenceScore = 0.99f
        ),
        ScannedReceiptData(
            vendor = "Kaveri Ready-Mix Concrete Pvt Ltd",
            date = "2026-10-17",
            billNumber = "RMC-KAV-774",
            totalAmount = 78500.0,
            taxAmount = 11974.0,
            suggestedCategory = "Civil & Foundation",
            suggestedTaskId = "Q02",
            lineItemsSummary = "M25 Design Mix Concrete (12 Cu.m) with Superplasticizer for IDT Foundation",
            confidenceScore = 0.97f
        )
    )
}
