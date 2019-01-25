package it.sephiroth.android.library.numberpicker

import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View

internal class CutoutDrawable : GradientDrawable() {
    private val cutoutPaint = Paint(1)
    private val cutoutBounds: RectF
    private var savedLayer: Int = 0

    init {
        this.setPaintStyles()
        this.cutoutBounds = RectF()
    }

    private fun setPaintStyles() {
        this.cutoutPaint.style = Paint.Style.FILL_AND_STROKE
        this.cutoutPaint.color = -1
        this.cutoutPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    fun hasCutout(): Boolean {
        return !this.cutoutBounds.isEmpty
    }

    fun setCutout(left: Float, top: Float, right: Float, bottom: Float) {
        if (left != this.cutoutBounds.left || top != this.cutoutBounds.top || right != this.cutoutBounds.right || bottom != this.cutoutBounds.bottom) {
            this.cutoutBounds.set(left, top, right, bottom)
            this.invalidateSelf()
        }

    }

    fun setCutout(bounds: RectF) {
        this.setCutout(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    fun removeCutout() {
        this.setCutout(0.0f, 0.0f, 0.0f, 0.0f)
    }

    override fun draw(canvas: Canvas) {
        this.preDraw(canvas)
        super.draw(canvas)
        canvas.drawRect(this.cutoutBounds, this.cutoutPaint)
        this.postDraw(canvas)
    }

    private fun preDraw(canvas: Canvas) {
        val callback = this.callback
        if (this.useHardwareLayer(callback)) {
            val viewCallback = callback as View
            viewCallback.setLayerType(2, null as Paint?)
        } else {
            this.saveCanvasLayer(canvas)
        }

    }

    private fun saveCanvasLayer(canvas: Canvas) {
        if (Build.VERSION.SDK_INT >= 21) {
            this.savedLayer = canvas.saveLayer(0.0f, 0.0f, canvas.width.toFloat(), canvas.height.toFloat(), null as Paint?)
        } else {
            this.savedLayer = canvas.saveLayer(0.0f, 0.0f, canvas.width.toFloat(), canvas.height.toFloat(), null as Paint?, Canvas.ALL_SAVE_FLAG)
        }

    }

    private fun postDraw(canvas: Canvas) {
        if (!this.useHardwareLayer(this.callback)) {
            canvas.restoreToCount(this.savedLayer)
        }

    }

    private fun useHardwareLayer(callback: Callback?): Boolean {
        return callback is View
    }
}
