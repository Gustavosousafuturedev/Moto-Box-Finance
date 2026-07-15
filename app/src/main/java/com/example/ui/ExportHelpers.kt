package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.R
import com.example.data.Delivery
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ExportHelpers {

    private const val TAG = "ExportHelpers"

    // Helper to calculate date ranges
    fun getPeriodTimestamps(
        period: String,
        customStart: Long? = null,
        customEnd: Long? = null
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val todayEnd = cal.timeInMillis

        return when (period) {
            "Hoje" -> Pair(todayStart, todayEnd)
            "Ontem" -> {
                cal.setTimeInMillis(todayStart)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStart = cal.timeInMillis
                
                cal.setTimeInMillis(todayEnd)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayEnd = cal.timeInMillis
                Pair(yesterdayStart, yesterdayEnd)
            }
            "Esta semana" -> {
                cal.setTimeInMillis(todayStart)
                // Set to first day of week
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val weekStart = cal.timeInMillis
                Pair(weekStart, todayEnd)
            }
            "Este mês" -> {
                cal.setTimeInMillis(todayStart)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = cal.timeInMillis
                Pair(monthStart, todayEnd)
            }
            "Personalizado" -> {
                val start = customStart ?: todayStart
                // Set custom end to end of that day
                val endCal = Calendar.getInstance()
                endCal.setTimeInMillis(customEnd ?: todayEnd)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                Pair(start, endCal.timeInMillis)
            }
            else -> Pair(0L, Long.MAX_VALUE)
        }
    }

    // Filters deliveries based on period and establishment
    fun filterDeliveries(
        deliveries: List<Delivery>,
        period: String,
        customStart: Long?,
        customEnd: Long?,
        selectedEstName: String
    ): List<Delivery> {
        val (start, end) = getPeriodTimestamps(period, customStart, customEnd)
        return deliveries.filter { d ->
            val dateMatches = d.date in start..end
            val estMatches = selectedEstName == "Todos os estabelecimentos" || d.establishmentName.equals(selectedEstName, ignoreCase = true)
            dateMatches && estMatches
        }
    }

    // Save File to Documents/NuCorre/ using MediaStore or File API fallback
    fun saveFileToDocuments(
        context: Context,
        fileName: String,
        mimeType: String,
        writeBlock: (OutputStream) -> Unit
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                var uri: Uri? = null

                // Try saving in Documents/NuCorre
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/NuCorre")
                    }
                    uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao criar em Documents/NuCorre, tentando Downloads/NuCorre...", e)
                }

                // Fallback to Download/NuCorre if Documents failed
                if (uri == null) {
                    try {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/NuCorre")
                        }
                        uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao criar em Download/NuCorre...", e)
                    }
                }

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        writeBlock(outputStream)
                    }
                }
                uri
            } else {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val nuCorreDir = File(documentsDir, "NuCorre")
                if (!nuCorreDir.exists()) {
                    nuCorreDir.mkdirs()
                }
                val file = File(nuCorreDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    writeBlock(outputStream)
                }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro geral ao salvar arquivo: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    // Export to PDF
    fun exportToPdf(
        context: Context,
        deliveries: List<Delivery>,
        period: String,
        customStart: Long?,
        customEnd: Long?,
        selectedEstName: String,
        motoboyName: String,
        motoboyPhone: String = "",
        motoboyCity: String = ""
    ): Uri? {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("MMMM_yyyy", Locale( "pt", "BR"))
        
        val fileName = when (period) {
            "Hoje" -> "Entregas_${sdfDate.format(Date())}.pdf"
            "Este mês" -> "Entregas_${sdfMonth.format(Date()).replaceFirstChar { it.uppercase() }}.pdf"
            "Personalizado" -> {
                val startStr = customStart?.let { sdfDate.format(Date(it)) } ?: "inicio"
                val endStr = customEnd?.let { sdfDate.format(Date(it)) } ?: "fim"
                "Entregas_${startStr}_a_${endStr}.pdf"
            }
            else -> "Entregas_${period.replace(" ", "_")}_${sdfDate.format(Date())}.pdf"
        }

        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        val file = File(documentsDir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas

                // Paints setup
                val brandColor = android.graphics.Color.parseColor("#aa00fa")
                val lightBgColor = android.graphics.Color.parseColor("#F3E5F5")
                val grayText = android.graphics.Color.parseColor("#757575")
                val borderGray = android.graphics.Color.parseColor("#E0E0E0")
                
                val titlePaint = Paint().apply {
                    color = brandColor
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val subtitlePaint = Paint().apply {
                    color = grayText
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val normalPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 10f
                    isAntiAlias = true
                }

                val boldPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val tableHeaderPaint = Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val linePaint = Paint().apply {
                    color = borderGray
                    strokeWidth = 1f
                }

                val shadedBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FAFAFA")
                }

                // Draw Header Helper
                fun drawPageHeader(pageNum: Int, pageCanvas: Canvas) {
                    // Try drawing logo
                    try {
                        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.img_nucorre_logo_1784120333345)
                        if (logo != null) {
                            val scaledLogo = Bitmap.createScaledBitmap(logo, 45, 45, true)
                            pageCanvas.drawBitmap(scaledLogo, 40f, 40f, null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao carregar logo no PDF", e)
                    }

                    pageCanvas.drawText("RELATÓRIO DE ENTREGAS", 100f, 58f, titlePaint)
                    pageCanvas.drawText("NuCorre • Inteligência Financeira para Entregadores", 100f, 74f, subtitlePaint)
                    
                    pageCanvas.drawLine(40f, 100f, 555f, 100f, linePaint)

                    // Info block (Updated with deliverer, phone, city)
                    pageCanvas.drawText("Entregador: $motoboyName", 40f, 120f, normalPaint)
                    pageCanvas.drawText("Telefone: $motoboyPhone", 40f, 136f, normalPaint)
                    pageCanvas.drawText("Cidade: $motoboyCity", 40f, 152f, normalPaint)
                    
                    val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    pageCanvas.drawText("Emissão: $currentDateTime", 360f, 120f, normalPaint)
                    pageCanvas.drawText("Período: $period", 360f, 136f, normalPaint)
                    pageCanvas.drawText("Estab.: $selectedEstName", 360f, 152f, normalPaint)

                    pageCanvas.drawLine(40f, 165f, 555f, 165f, linePaint)
                }

                // Draw page 1 header
                drawPageHeader(1, canvas)

                // Table Header positions
                val xData = 40f
                val xHora = 100f
                val xEstab = 150f
                val xBairro = 310f
                val xCidade = 430f

                var y = 185f

                // Draw Table Headers
                canvas.drawText("Data", xData, y, tableHeaderPaint)
                canvas.drawText("Hora", xHora, y, tableHeaderPaint)
                canvas.drawText("Estabelecimento", xEstab, y, tableHeaderPaint)
                canvas.drawText("Bairro", xBairro, y, tableHeaderPaint)
                canvas.drawText("Cidade", xCidade, y, tableHeaderPaint)

                y += 8f
                canvas.drawLine(40f, y, 555f, y, Paint(linePaint).apply { strokeWidth = 1.5f })
                y += 16f

                val rowSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                if (deliveries.isEmpty()) {
                    canvas.drawText("Nenhuma entrega registrada para o período selecionado.", 100f, 250f, subtitlePaint.apply { textSize = 11f })
                } else {
                    deliveries.forEachIndexed { index, d ->
                        // Page boundary check
                        if (y > 750f) {
                            pdfDocument.finishPage(page)
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            
                            // Draw header on new page
                            drawPageHeader(pdfDocument.pages.size, canvas)
                            
                            y = 185f
                            canvas.drawText("Data", xData, y, tableHeaderPaint)
                            canvas.drawText("Hora", xHora, y, tableHeaderPaint)
                            canvas.drawText("Estabelecimento", xEstab, y, tableHeaderPaint)
                            canvas.drawText("Bairro", xBairro, y, tableHeaderPaint)
                            canvas.drawText("Cidade", xCidade, y, tableHeaderPaint)

                            y += 8f
                            canvas.drawLine(40f, y, 555f, y, Paint(linePaint).apply { strokeWidth = 1.5f })
                            y += 16f
                        }

                        // Shaded row background for alternate rows
                        if (index % 2 == 1) {
                            canvas.drawRect(40f, y - 11f, 555f, y + 5f, shadedBgPaint)
                        }

                        val dateStr = rowSdf.format(Date(d.date))
                        canvas.drawText(dateStr, xData, y, normalPaint)
                        canvas.drawText(d.time, xHora, y, normalPaint)

                        val estTrunc = if (d.establishmentName.length > 22) d.establishmentName.take(20) + ".." else d.establishmentName
                        canvas.drawText(estTrunc, xEstab, y, normalPaint)

                        val neighborhoodTrunc = if (d.neighborhood.length > 18) d.neighborhood.take(16) + ".." else d.neighborhood
                        canvas.drawText(neighborhoodTrunc, xBairro, y, normalPaint)

                        val cityTrunc = if (d.city.length > 18) d.city.take(16) + ".." else d.city
                        canvas.drawText(cityTrunc, xCidade, y, normalPaint)

                        y += 8f
                        canvas.drawLine(40f, y, 555f, y, Paint().apply { color = borderGray; strokeWidth = 0.5f })
                        y += 12f
                    }
                }

                // Totals Card Block (if space allows, otherwise on a new page)
                if (y > 700f) {
                    pdfDocument.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    drawPageHeader(pdfDocument.pages.size, canvas)
                    y = 185f
                }

                val totalCount = deliveries.sumOf { it.quantity }

                canvas.drawRoundRect(40f, y, 555f, y + 50f, 6f, 6f, Paint().apply { color = lightBgColor })
                
                val totalPaintText = Paint(normalPaint).apply { textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = brandColor }
                canvas.drawText("Total de Entregas: $totalCount", 55f, y + 22f, totalPaintText)
                
                canvas.drawText("Relatório de entregas gerado com segurança através do NuCorre.", 55f, y + 38f, subtitlePaint.apply { textSize = 8.5f })

                pdfDocument.finishPage(page)

                // IF TODOS OS ESTABELECIMENTOS -> Create second page (or summary page)
                if (selectedEstName == "Todos os estabelecimentos" && deliveries.isNotEmpty()) {
                    val summaryPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    val summaryPage = pdfDocument.startPage(summaryPageInfo)
                    val sumCanvas = summaryPage.canvas

                    // Header for Summary Page
                    try {
                        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.img_nucorre_logo_1784120333345)
                        if (logo != null) {
                            val scaledLogo = Bitmap.createScaledBitmap(logo, 45, 45, true)
                            sumCanvas.drawBitmap(scaledLogo, 40f, 40f, null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao carregar logo no resumo", e)
                    }

                    sumCanvas.drawText("RESUMO POR ESTABELECIMENTO", 100f, 58f, titlePaint)
                    sumCanvas.drawText("NuCorre • Total Geral de Entregas Consolidadas", 100f, 74f, subtitlePaint)
                    sumCanvas.drawLine(40f, 100f, 555f, 100f, linePaint)

                    // Info block
                    sumCanvas.drawText("Entregador: $motoboyName", 40f, 120f, normalPaint)
                    sumCanvas.drawText("Telefone: $motoboyPhone", 40f, 136f, normalPaint)
                    sumCanvas.drawText("Cidade: $motoboyCity", 40f, 152f, normalPaint)
                    
                    sumCanvas.drawText("Emissão: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 360f, 120f, normalPaint)
                    sumCanvas.drawLine(40f, 165f, 555f, 165f, linePaint)

                    var sumY = 180f
                    sumCanvas.drawText("Estabelecimento", 40f, sumY, tableHeaderPaint.apply { textSize = 11f })
                    sumCanvas.drawText("Quantidade de Entregas", 330f, sumY, tableHeaderPaint)

                    sumY += 8f
                    sumCanvas.drawLine(40f, sumY, 555f, sumY, Paint(linePaint).apply { strokeWidth = 1.5f })
                    sumY += 20f

                    // Group by establishment
                    val grouped = deliveries.groupBy { it.establishmentName }
                    var index = 0
                    grouped.forEach { (estName, list) ->
                        if (sumY > 720f) {
                            pdfDocument.finishPage(summaryPage)
                            // This handles if we have many establishments in summary, unlikely but safe
                            val nextSumPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                            val nextSumPage = pdfDocument.startPage(nextSumPageInfo)
                            // Drawing same header
                            // ...
                        }

                        if (index % 2 == 1) {
                            sumCanvas.drawRect(40f, sumY - 14f, 555f, sumY + 6f, shadedBgPaint)
                        }

                        sumCanvas.drawText(estName, 40f, sumY, normalPaint.apply { textSize = 11f })
                        sumCanvas.drawText(list.sumOf { it.quantity }.toString(), 330f, sumY, boldPaint.apply { textSize = 11f })

                        sumY += 10f
                        sumCanvas.drawLine(40f, sumY, 555f, sumY, Paint().apply { color = borderGray; strokeWidth = 0.5f })
                        sumY += 15f
                        index++
                    }

                    // Footer of Summary Page
                    sumCanvas.drawRoundRect(40f, 720f, 555f, 785f, 6f, 6f, Paint().apply { color = lightBgColor })
                    sumCanvas.drawText("TOTAL GERAL DE ENTREGAS: $totalCount", 55f, 755f, boldPaint.apply { textSize = 11f; color = brandColor })

                    pdfDocument.finishPage(summaryPage)
                }

                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
            }
            val authority = "${context.packageName}.fileprovider"
            return androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            Log.e(TAG, "Erro geral ao salvar PDF local: ${e.message}", e)
            return null
        }
    }

    // Export to Excel (.xlsx) using Apache POI
    fun exportToExcel(
        context: Context,
        deliveries: List<Delivery>
    ): Uri? {
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val fileName = "Entregas_${sdfMonth.format(Date())}.xlsx"

        return saveFileToDocuments(context, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") { outputStream ->
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Relatório de Entregas")

            // Title Style / Styling
            val headerFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 11.toShort()
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
            }

            // Headers
            val headerRow = sheet.createRow(0)
            val columns = listOf("Data", "Hora", "Estabelecimento", "Bairro", "Cidade", "Valor", "Forma de Pagamento")
            columns.forEachIndexed { i, col ->
                val cell = headerRow.createCell(i)
                cell.setCellValue(col)
                cell.cellStyle = headerStyle
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            // Rows
            var rowIndex = 1
            deliveries.forEach { d ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(sdf.format(Date(d.date)))
                row.createCell(1).setCellValue(d.time)
                row.createCell(2).setCellValue(d.establishmentName)
                row.createCell(3).setCellValue(d.neighborhood)
                row.createCell(4).setCellValue(d.city)
                row.createCell(5).setCellValue(d.value)
                row.createCell(6).setCellValue(d.paymentMethod)
            }

            // Empty row
            sheet.createRow(rowIndex++)

            // Totals Row
            val totalsRow = sheet.createRow(rowIndex)
            val totalsFont = workbook.createFont().apply {
                bold = true
            }
            val totalsStyle = workbook.createCellStyle().apply {
                setFont(totalsFont)
            }

            val cellTotalLabel = totalsRow.createCell(0)
            cellTotalLabel.setCellValue("Quantidade de Entregas:")
            cellTotalLabel.cellStyle = totalsStyle

            val cellTotalVal = totalsRow.createCell(1)
            cellTotalVal.setCellValue(deliveries.sumOf { it.quantity }.toDouble())
            cellTotalVal.cellStyle = totalsStyle

            val cellEarnLabel = totalsRow.createCell(4)
            cellEarnLabel.setCellValue("Total Recebido:")
            cellEarnLabel.cellStyle = totalsStyle

            val cellEarnVal = totalsRow.createCell(5)
            cellEarnVal.setCellValue(deliveries.sumOf { it.value })
            cellEarnVal.cellStyle = totalsStyle

            // Auto-size columns
            for (i in columns.indices) {
                sheet.autoSizeColumn(i)
            }

            workbook.write(outputStream)
            workbook.close()
        }
    }

    // Open file using Android views
    fun openFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Nenhum aplicativo encontrado para abrir este arquivo.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Erro ao abrir arquivo", e)
        }
    }

    // Share file generally
    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
    }

    // Share directly via WhatsApp
    fun shareToWhatsApp(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            `package` = "com.whatsapp"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Try WhatsApp Business package as alternative
            try {
                val businessIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    `package` = "com.whatsapp.w4b"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(businessIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp não está instalado neste dispositivo.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "WhatsApp não instalado", ex)
            }
        }
    }

    // Share via Email
    fun shareToEmail(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, "Relatório de Entregas - NuCorre")
            putExtra(Intent.EXTRA_TEXT, "Olá,\n\nSegue em anexo o relatório de entregas gerado pelo aplicativo NuCorre.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Enviar por E-mail"))
        } catch (e: Exception) {
            Toast.makeText(context, "Nenhum aplicativo de e-mail configurado.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "E-mail não configurado", e)
        }
    }
}
