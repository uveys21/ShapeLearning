package com.example.shapelearning.ui.components

import android.content.Context
import android.graphics.* // Keep specific imports
import android.util.AttributeSet
import android.util.Log // Added
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt // Added for paint color
import androidx.core.content.ContextCompat
import com.example.shapelearning.R

/**
 * Şekil izleme görünümü
 * Kullanıcının şekilleri izlemesini sağlayan özel görünüm.
 * Bu görünüm SADECE kullanıcının çizimini yapar. Arka plandaki ana hat
 * ayrı bir ImageView tarafından gösterilmelidir.
 */
class TracingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var strokeWidthPx = 16f // Default stroke width in pixels
    private var paintColor = ContextCompat.getColor(context, R.color.tracing_pen_color) // Use specific color

    private val paint = Paint().apply {
        color = paintColor
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokeWidthPx
        isAntiAlias = true
    }

    private val path = Path() // Path for the current continuous stroke
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null // Bitmap to hold the persistent drawing
    private var lastX = 0f
    private var lastY = 0f

    // Stores the sequence of points traced by the user
    private val tracingPathPoints = mutableListOf<Pair<Float, Float>>()

    // Callback for when tracing gesture ends (optional)
    // var onTracingCompleteListener: ((List<Pair<Float, Float>>) -> Unit)? = null

    init {
        // Load custom attributes if defined in XML (e.g., strokeWidth, paintColor)
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TracingView, defStyleAttr, 0)
            strokeWidthPx = typedArray.getDimension(R.styleable.TracingView_strokeWidth, strokeWidthPx)
            paintColor = typedArray.getColor(R.styleable.TracingView_paintColor, paintColor)
            paint.strokeWidth = strokeWidthPx
            paint.color = paintColor
            typedArray.recycle()
        }
        // Ensure view draws itself if background isn't set
        // setWillNotDraw(false) // Usually not needed unless extending a layout
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Recreate bitmap and canvas if size changes, clear existing drawing
        if (w > 0 && h > 0) {
            // Release old bitmap first if it exists
            canvasBitmap?.recycle()
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(canvasBitmap!!)
            // Important: Clear the new bitmap (might be reused)
            drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            path.reset() // Reset path as well
            tracingPathPoints.clear() // Clear points on resize
            Log.d("TracingView", "onSizeChanged: New Bitmap created ($w x $h)")
        } else {
            canvasBitmap = null
            drawCanvas = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw the persistent bitmap (previous strokes)
        canvasBitmap?.let {
            if (!it.isRecycled) {
                canvas.drawBitmap(it, 0f, 0f, null)
            }
        }
        // Draw the current stroke being made
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        // Ignore touches outside the view bounds
        if (x < 0 || x > width || y < 0 || y > height) {
            // Consider lifting the pen if touch moves outside
            if (event.action == MotionEvent.ACTION_MOVE && !path.isEmpty) {
                // Treat as ACTION_UP to finalize the path segment
                finalizeStroke(x, y)
                return true // Consume event
            }
            return false
        }


        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset() // Start a new path segment
                path.moveTo(x, y)
                lastX = x
                lastY = y
                tracingPathPoints.add(x to y)
                invalidate() // Redraw
                return true // Consume event
            }
            MotionEvent.ACTION_MOVE -> {
                // Use quadratic bezier for smoother curves
                val midX = (x + lastX) / 2
                val midY = (y + lastY) / 2
                path.quadTo(lastX, lastY, midX, midY)
                lastX = x
                lastY = y
                tracingPathPoints.add(x to y) // Add intermediate points
                invalidate() // Redraw
                return true // Consume event
            }
            MotionEvent.ACTION_UP -> {
                finalizeStroke(x, y)
                return true // Consume event
            }
            else -> return false
        }
    }

    // Finalize the current stroke onto the bitmap
    private fun finalizeStroke(x: Float, y: Float) {
        path.lineTo(x, y) // Ensure the last point is connected
        drawCanvas?.drawPath(path, paint) // Draw the completed path onto the bitmap
        path.reset() // Reset the path for the next stroke
        tracingPathPoints.add(x to y) // Add final point
        invalidate() // Redraw to show the path now on the bitmap

        // Optional: Notify listener that tracing gesture ended
        // onTracingCompleteListener?.invoke(tracingPathPoints.toList())
    }


    /**
     * Çizimi temizle
     */
    fun clearDrawing() {
        path.reset()
        // Clear the bitmap with transparency
        drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        tracingPathPoints.clear() // Clear the points list
        Log.d("TracingView", "Drawing cleared.")
        invalidate() // Request redraw
    }

    /**
     * Şekil ana hat verisini ayarla (Doğrulama için)
     * Bu metod görünümü DEĞİŞTİRMEZ, sadece doğrulama için veri sağlar.
     * Gerçek ana hat bir ImageView'da gösterilmelidir.
     * @param resourceId Ana hat resmi ID'si (ileride nokta verisine dönüştürülebilir)
     */
    fun setShapeOutlineData(resourceId: Int) {
        // Currently just logs it. ViewModel should use this ID to get actual points.
        Log.d("TracingView", "Outline data set (using resource ID): $resourceId")
        // In the future, this might load points:
        // targetOutlinePoints = loadPointsFromResource(resourceId)
    }

    /**
     * Çizim yolunu al (nokta listesi olarak)
     */
    fun getTracingPath(): List<Pair<Float, Float>> {
        // Return a copy to prevent external modification
        return tracingPathPoints.toList()
    }

    /**
     * Set paint color programmatically.
     */
    fun setPaintColor(@ColorInt color: Int) {
        paintColor = color
        paint.color = color
        invalidate()
    }

    /**
     * Set stroke width programmatically.
     */
    fun setStrokeWidth(widthPx: Float) {
        strokeWidthPx = widthPx.coerceAtLeast(1f) // Ensure positive width
        paint.strokeWidth = strokeWidthPx
        invalidate()
    }
}

// **Add these attributes to res/values/attrs.xml:**
/*
<resources>
    <declare-styleable name="TracingView">
        <attr name="strokeWidth" format="dimension" />
        <attr name="paintColor" format="color" />
    </declare-styleable>
</resources>
*/