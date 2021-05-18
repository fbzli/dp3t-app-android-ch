package ch.admin.bag.dp3t.checkin.generateqrcode

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorInt
import ch.admin.bag.dp3t.R
import ch.admin.bag.dp3t.checkin.utils.getSubtitle
import ch.admin.bag.dp3t.databinding.PdfQrCodeBinding
import org.crowdnotifier.android.sdk.model.VenueInfo


private const val PDF_WIDTH = 1240
private const val PDF_HEIGHT = 1748

fun createEntryPdf(venueInfo: VenueInfo, bitmap: Bitmap, context: Context): PdfDocument {

	val document = PdfDocument()
	val pageInfo = PdfDocument.PageInfo.Builder(PDF_WIDTH, PDF_HEIGHT, 1).create() // A4 size
	val page = document.startPage(pageInfo)
	page.canvas.apply {
		val indicatorOffset = 20f
		val qrCodeY = 400f
		drawLinesAroundQrCode(
			start = (PDF_WIDTH - bitmap.width) / 2f - indicatorOffset,
			top = qrCodeY - indicatorOffset,
			end = PDF_WIDTH - (PDF_WIDTH - bitmap.width) / 2f + indicatorOffset,
			bottom = qrCodeY + bitmap.height + indicatorOffset,
			this,
			swissCovidBlue
		)
		drawBitmap(bitmap, (PDF_WIDTH - bitmap.width) / 2f, qrCodeY, Paint())

		drawText(
			context.getString(R.string.check_in_now_button_title),
			PDF_WIDTH / 2f,
			qrCodeY + bitmap.height + 2 * indicatorOffset,
			blueCenteredBoldPaint
		)

		val pdfView = PdfQrCodeBinding.inflate(LayoutInflater.from(context)).apply {
			title.text = venueInfo.title
			subtitle.text = context.getString(venueInfo.getSubtitle())
		}.root

		pdfView.measure(
			View.MeasureSpec.makeMeasureSpec(PDF_WIDTH, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(PDF_HEIGHT, View.MeasureSpec.EXACTLY)
		)
		pdfView.layout(0, 0, PDF_WIDTH, PDF_HEIGHT)
		pdfView.draw(this)
	}

	document.finishPage(page)
	return document

}

private val swissCovidBlue = Color.parseColor("#5094bf")

private val blueCenteredBoldPaint = Paint().apply {
	color = swissCovidBlue
	textAlign = Paint.Align.CENTER
	textSize = 30f
	typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

private fun drawLinesAroundQrCode(start: Float, top: Float, end: Float, bottom: Float, canvas: Canvas, @ColorInt strokeColor: Int) {
	val indicatorWidth = (end - start) / 5

	val linePaint = Paint().apply {
		color = strokeColor
		strokeWidth = 8f
		style = Paint.Style.STROKE
	}
	canvas.apply {
		drawPath(Path().apply {
			moveTo(start, top + indicatorWidth)
			lineTo(start, top)
			lineTo(start + indicatorWidth, top)
			moveTo(end - indicatorWidth, top)
			lineTo(end, top)
			lineTo(end, top + indicatorWidth)
			moveTo(end, bottom - indicatorWidth)
			lineTo(end, bottom)
			lineTo(end - indicatorWidth, bottom)
			moveTo(start + indicatorWidth, bottom)
			lineTo(start, bottom)
			lineTo(start, bottom - indicatorWidth)

		}, linePaint)
	}
}