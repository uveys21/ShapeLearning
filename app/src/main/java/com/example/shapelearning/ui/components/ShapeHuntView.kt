package com.example.shapelearning.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF // Added for clarity
import android.util.AttributeSet
import android.util.Log // Added
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.shapelearning.R
import com.example.shapelearning.ui.games.hunt.ShapeHuntViewModel // Keep specific import

/**
 * Şekil avı görünümü
 * Kullanıcının sahnedeki gizli şekilleri bulmak için dokunmasını sağlar
 * ve bulunanları işaretler.
 */
class ShapeHuntView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Listener interface for tap attempts
    interface OnShapeFoundListener {
        fun onShapeFoundAttempt(tapPosition: Pair<Float, Float>) // Normalized coordinates (0.0 to 1.0)
    }

    private var markerStrokeWidth = 8f
    private var markerColor = ContextCompat.getColor(context, R.color.hunt_marker_success) // Add color
    private var markerRadiusRatio = 0.05f // Radius as fraction of view width

    private val markerPaint = Paint().apply {
        color = markerColor
        style = Paint.Style.STROKE
        strokeWidth = markerStrokeWidth
        isAntiAlias = true
    }

    // List of FoundShape objects provided by the ViewModel
    private var foundShapesList = listOf<ShapeHuntViewModel.FoundShape>()
    private var listener: OnShapeFoundListener? = null

    init {
        // Load custom attributes if defined (e.g., markerColor, markerStrokeWidth)
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ShapeHuntView, defStyleAttr, 0)
            markerStrokeWidth = typedArray.getDimension(R.styleable.ShapeHuntView_markerStrokeWidth, markerStrokeWidth)
            markerColor = typedArray.getColor(R.styleable.ShapeHuntView_markerColor, markerColor)
            markerRadiusRatio = typedArray.getFloat(R.styleable.ShapeHuntView_markerRadiusRatio, markerRadiusRatio).coerceIn(0.01f, 0.2f) // Clamp ratio
            markerPaint.strokeWidth = markerStrokeWidth
            markerPaint.color = markerColor
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return // Avoid drawing if view not measured

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val dynamicRadius = viewWidth * markerRadiusRatio // Calculate radius based on width

        // Draw markers for each found shape
        for (foundShape in foundShapesList) {
            // Convert normalized position (0-1) to view coordinates
            val cx = foundShape.position.first * viewWidth
            val cy = foundShape.position.second * viewHeight

            // Draw a circle marker
            canvas.drawCircle(cx, cy, dynamicRadius, markerPaint)

            // Optional: Draw a cross marker inside/instead
            val crossSize = dynamicRadius * 0.7f // Make cross smaller than circle
            canvas.drawLine(cx - crossSize, cy - crossSize, cx + crossSize, cy + crossSize, markerPaint)
            canvas.drawLine(cx + crossSize, cy - crossSize, cx - crossSize, cy + crossSize, markerPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (width == 0 || height == 0) return false // Ignore taps if view not measured

            val tapX = event.x
            val tapY = event.y

            // Normalize tap coordinates to 0.0 - 1.0 range
            val normalizedX = (tapX / width.toFloat()).coerceIn(0f, 1f)
            val normalizedY = (tapY / height.toFloat()).coerceIn(0f, 1f)

            val tapPosition = Pair(normalizedX, normalizedY)

            // Notify the listener (Fragment/ViewModel) about the tap attempt
            Log.d("ShapeHuntView", "Tap detected at normalized: $tapPosition")
            listener?.onShapeFoundAttempt(tapPosition)
            // performClick() // Call for accessibility

            return true // Consume the event
        }
        return super.onTouchEvent(event)
    }

    // Required for accessibility
    override fun performClick(): Boolean {
        super.performClick()
        // Handle click action if needed separately from ACTION_DOWN
        return true
    }

    /**
     * Bulunan şekilleri güncelle (ViewModel'den çağrılır)
     */
    fun updateFoundShapes(shapes: List<ShapeHuntViewModel.FoundShape>) {
        foundShapesList = shapes
        invalidate() // Request redraw to show new markers
    }

    /**
     * Dokunma dinleyicisini ayarla (Fragment tarafından)
     */
    fun setOnShapeFoundListener(listener: OnShapeFoundListener) {
        this.listener = listener
    }

    /**
     * Set marker color programmatically.
     */
    fun setMarkerColor(color: Int) {
        markerColor = color
        markerPaint.color = color
        invalidate()
    }
}


// **Add these attributes to res/values/attrs.xml:**
/*
<resources>
    <declare-styleable name="ShapeHuntView">
        <attr name="markerStrokeWidth" format="dimension" />
        <attr name="markerColor" format="color" />
        <attr name="markerRadiusRatio" format="float" /> <!-- Radius as ratio of width -->
    </declare-styleable>
</resources>
*/