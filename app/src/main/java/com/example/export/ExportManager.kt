package com.example.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.TransactionEntity
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    suspend fun writeCsv(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write("Date,Merchant,Category,Amount_INR,Source,ML_Confidence,Snippet\n")
                    for (tx in transactions) {
                        val cat = ExpenseCategory.fromName(tx.categoryName).displayName
                        val row = listOf(
                            csvEscape(dateFmt.format(Date(tx.timestamp))),
                            csvEscape(tx.merchant),
                            csvEscape(cat),
                            String.format(Locale.US, "%.2f", tx.amountInr),
                            csvEscape(tx.sourceName),
                            String.format(Locale.US, "%.0f%%", tx.mlConfidence * 100f),
                            csvEscape(tx.rawSnippet),
                        ).joinToString(",")
                        writer.write(row)
                        writer.write("\n")
                    }
                }
            } ?: error("Unable to open destination file")
            transactions.size
        }
    }

    suspend fun writePdf(
        context: Context,
        uri: Uri,
        monthTitle: String,
        monthlyGoalInr: Double?,
        transactions: List<TransactionEntity>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = PdfDocument()
            val pageWidth = 595 // A4 points
            val pageHeight = 842
            val margin = 40f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#121316")
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#66686D")
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#181A1C")
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#181A1C")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#121316")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E4E7")
                strokeWidth = 1f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas
            var y = 52f

            // Header
            canvas.drawText("FinTracker · Monthly Statement", margin, y, titlePaint)
            y += 18f
            val totalSpent = transactions.sumOf { it.amountInr }
            val goalSummary = monthlyGoalInr?.let { "Goal: ${InrFormatter.format(it)}" } ?: "No monthly goal set"
            canvas.drawText("$monthTitle  ·  Total Spent: ${InrFormatter.format(totalSpent)}  ·  $goalSummary", margin, y, subPaint)
            y += 14f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 24f

            // Category summary
            canvas.drawText("Category Summary", margin, y, sectionPaint)
            y += 16f
            val byCategory = transactions.groupBy { ExpenseCategory.fromName(it.categoryName) }
                .entries
                .sortedByDescending { it.value.sumOf { tx -> tx.amountInr } }

            for ((cat, list) in byCategory) {
                val sum = list.sumOf { it.amountInr }
                canvas.drawText("${cat.displayName} (${list.size})", margin, y, rowPaint)
                canvas.drawText(InrFormatter.format(sum), pageWidth - margin, y, amountPaint)
                y += 16f
            }

            y += 12f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 22f
            canvas.drawText("Transactions (${transactions.size})", margin, y, sectionPaint)
            y += 18f

            val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
            for (tx in transactions) {
                if (y > pageHeight - 54f) {
                    doc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 48f
                    canvas.drawText("FinTracker · $monthTitle (Page $pageNumber)", margin, y, subPaint)
                    y += 22f
                }

                val cat = ExpenseCategory.fromName(tx.categoryName).shortName
                val dateStr = dateFmt.format(Date(tx.timestamp))
                canvas.drawText(tx.merchant.take(28), margin, y, sectionPaint.apply { textSize = 11f })
                canvas.drawText(InrFormatter.format(tx.amountInr), pageWidth - margin, y, amountPaint)
                y += 13f
                canvas.drawText("$cat · $dateStr · ${tx.sourceName}", margin, y, subPaint)
                y += 10f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 14f
            }

            doc.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                doc.writeTo(out)
            } ?: error("Unable to open PDF destination")
            doc.close()
            transactions.size
        }
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
