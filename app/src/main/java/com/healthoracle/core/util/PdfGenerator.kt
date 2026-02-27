package com.healthoracle.core.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object PdfGenerator {
    fun generatePdf(context: Context, uri: Uri, conditionName: String, content: String) {
        val pdfDocument = PdfDocument()

        val titlePaint = TextPaint().apply {
            color = Color.rgb(11, 135, 125) // HealthOracle Primary Teal
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Standard A4 dimensions
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50
        val textWidth = pageWidth - (margin * 2)

        // StaticLayout automatically handles line breaks and text wrapping!
        val staticLayout = StaticLayout.Builder.obtain(content, 0, content.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.0f, 1.0f)
            .setIncludePad(false)
            .build()

        var yPosition = margin.toFloat()
        var currentLine = 0
        var pageNumber = 1

        // Loop to create as many pages as needed for long text
        while (currentLine < staticLayout.lineCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw the title header only on the very first page
            if (pageNumber == 1) {
                canvas.drawText("HealthOracle AI Plan: $conditionName", margin.toFloat(), yPosition, titlePaint)
                yPosition += 40f
            } else {
                yPosition = margin.toFloat()
            }

            canvas.save()
            canvas.translate(margin.toFloat(), yPosition)

            // Calculate how many lines of text can actually fit on this current page
            val startY = staticLayout.getLineTop(currentLine)
            while (currentLine < staticLayout.lineCount) {
                val lineBottom = staticLayout.getLineBottom(currentLine)
                if (yPosition + (lineBottom - startY) > pageHeight - margin) {
                    break // Page is full! We will continue on the next page
                }
                currentLine++
            }

            // Clip the canvas and draw the specific chunk of text for this page
            canvas.clipRect(0, 0, textWidth, pageHeight - margin - yPosition.toInt())
            canvas.translate(0f, -startY.toFloat())
            staticLayout.draw(canvas)

            canvas.restore()
            pdfDocument.finishPage(page)
            pageNumber++
        }

        // Write the PDF securely to the user's chosen folder
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }
}