package com.taleon.reader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View

data class PageRenderStyle(
    val textSizePx: Float,
    val lineSpacingMultiplier: Float,
    val typeface: Typeface,
    val textColor: Int,
)

/**
 * Draws one pre-computed page with the same StaticLayout line breaking used by the paginator.
 * Keeping this a plain View avoids Compose's text layout diverging from the page boundaries.
 */
class ReaderPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var display: CharSequence? = null
    private var start = 0
    private var end = 0
    private var style: PageRenderStyle? = null
    private var layout: StaticLayout? = null

    fun setPage(display: CharSequence?, start: Int, end: Int, style: PageRenderStyle) {
        val safeStart = start.coerceIn(0, display?.length ?: 0)
        val safeEnd = end.coerceIn(safeStart, display?.length ?: safeStart)
        val changed = this.display !== display || this.start != safeStart || this.end != safeEnd || this.style != style
        if (!changed) return
        this.display = display
        this.start = safeStart
        this.end = safeEnd
        this.style = style
        buildLayout()
        contentDescription = display
            ?.subSequence(safeStart, safeEnd)
            ?.toString()
            ?.replace(Regex("""\s+"""), " ")
            ?.take(600)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        layout?.draw(canvas)
    }

    @SuppressLint("WrongConstant")
    private fun buildLayout() {
        val source = display ?: return
        val renderStyle = style ?: return
        if (width <= 0) return
        val pageText = SpannableStringBuilder(source, start, end)
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = renderStyle.textSizePx
            typeface = renderStyle.typeface
            color = renderStyle.textColor
            letterSpacing = 0.008f
            isSubpixelText = true
        }
        layout = StaticLayout.Builder.obtain(pageText, 0, pageText.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, renderStyle.lineSpacingMultiplier)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            .build()
    }
}
