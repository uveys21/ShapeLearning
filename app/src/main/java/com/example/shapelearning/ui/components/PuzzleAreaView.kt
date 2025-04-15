package com.example.shapelearning.ui.components

import android.content.Context
import android.graphics.* // Keep specific imports
import android.util.AttributeSet
import android.util.Log // Added
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.shapelearning.R
import com.example.shapelearning.ui.games.puzzle.ShapePuzzleViewModel // Keep specific import
import androidx.core.content.withStyledAttributes

/**
 * Yapboz alanı görünümü
 * Kullanıcının şekilleri yerleştirmesi için dokunulabilir slotlar gösterir.
 */
class PuzzleAreaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Listener interface for slot taps
    interface OnShapeDroppedListener { // Renamed for clarity, though it's a tap now
        fun onPuzzleSlotTapped(slotIndex: Int)
    }

    private var gridRows = 3 // Default grid size
    private var gridCols = 3
    private var slotStrokeWidth = 6f
    private var slotColor = ContextCompat.getColor(context, R.color.puzzle_slot_border) // Add color

    private val slotPaint = Paint().apply {
        color = slotColor
        style = Paint.Style.STROKE
        strokeWidth = slotStrokeWidth
        isAntiAlias = true
    }
    private val shapePaint = Paint().apply { // Paint for drawing placed shapes (optional effects)
        isAntiAlias = true
        isFilterBitmap = true
    }


    // Map: Slot Index -> Placed PuzzleShape data
    private val placedShapes = mutableMapOf<Int, ShapePuzzleViewModel.PuzzleShape>()
    // List of Rect objects representing the tappable slots
    private val puzzleSlots = mutableListOf<Rect>()

    private var listener: OnShapeDroppedListener? = null

    init {
        // Load custom attributes if defined (e.g., gridRows, gridCols, slotColor)
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.PuzzleAreaView, defStyleAttr, 0) {
                gridRows = getInt(R.styleable.PuzzleAreaView_gridRows, gridRows)
                gridCols = getInt(R.styleable.PuzzleAreaView_gridCols, gridCols)
                slotStrokeWidth =
                    getDimension(R.styleable.PuzzleAreaView_slotStrokeWidth, slotStrokeWidth)
                slotColor = getColor(R.styleable.PuzzleAreaView_slotColor, slotColor)
                slotPaint.strokeWidth = slotStrokeWidth
                slotPaint.color = slotColor
            }
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            setupPuzzleSlots(w, h)
            Log.d("PuzzleAreaView", "onSizeChanged: Slots recalculated ($w x $h)")
        }
    }

    private fun setupPuzzleSlots(width: Int, height: Int) {
        puzzleSlots.clear()
        if (gridRows <= 0 || gridCols <= 0) {
            Log.e("PuzzleAreaView", "Invalid grid dimensions: Rows=$gridRows, Cols=$gridCols")
            return
        }

        val slotWidth = width / gridCols
        val slotHeight = height / gridRows
        val totalSlots = gridRows * gridCols

        for (i in 0 until totalSlots) {
            val row = i / gridCols
            val col = i % gridCols

            val left = col * slotWidth
            val top = row * slotHeight
            val right = left + slotWidth
            val bottom = top + slotHeight

            // Add slight inset/padding to the drawing bounds if desired
            // val padding = (slotStrokeWidth / 2).toInt()
            // puzzleSlots.add(Rect(left + padding, top + padding, right - padding, bottom - padding))
            puzzleSlots.add(Rect(left, top, right, bottom))
        }
        Log.d("PuzzleAreaView", "Setup ${puzzleSlots.size} puzzle slots.")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the empty puzzle slots (borders)
        for (slotRect in puzzleSlots) {
            canvas.drawRect(slotRect, slotPaint)
        }

        // Draw the placed shapes within their corresponding slots
        for ((slotIndex, puzzleShape) in placedShapes) {
            if (slotIndex >= 0 && slotIndex < puzzleSlots.size) {
                val slotRect = puzzleSlots[slotIndex]
                try {
                    val drawable = ContextCompat.getDrawable(context, puzzleShape.imageResId)
                    if (drawable != null) {
                        // Create inset bounds for the drawable within the slot
                        val inset = (slotRect.width() * 0.1f).toInt() // Example 10% inset
                        val drawableBounds = Rect(
                            slotRect.left + inset,
                            slotRect.top + inset,
                            slotRect.right - inset,
                            slotRect.bottom - inset
                        )
                        drawable.bounds = drawableBounds
                        drawable.draw(canvas)
                    } else {
                        Log.w("PuzzleAreaView", "Drawable not found for shape ID: ${puzzleShape.id}, Res ID: ${puzzleShape.imageResId}")
                    }
                } catch (e: Exception) {
                    Log.e("PuzzleAreaView", "Error drawing shape ID: ${puzzleShape.id}", e)
                    // Optionally draw an error placeholder in the slot
                    // canvas.drawText("!", slotRect.centerX().toFloat(), slotRect.centerY().toFloat(), errorPaint)
                }
            } else {
                Log.w("PuzzleAreaView", "Invalid slot index found in placedShapes map: $slotIndex")
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            // Find which slot was tapped
            for ((index, slotRect) in puzzleSlots.withIndex()) {
                if (slotRect.contains(x.toInt(), y.toInt())) {
                    // Notify the listener that this slot was tapped
                    Log.d("PuzzleAreaView", "Slot tapped: $index")
                    listener?.onPuzzleSlotTapped(index)
                    // Optional: Add ripple effect or visual feedback on tap
                    // performClick() // Call this for accessibility
                    return true // Consume the event
                }
            }
        }
        return super.onTouchEvent(event) // Allow default handling if no slot hit
    }

    // Required for accessibility if OnClickListener is set or touch events handled
    override fun performClick(): Boolean {
        super.performClick()
        // Handle click action if needed, though ACTION_DOWN is used here
        return true
    }

    /**
     * Yerleştirilmiş şekilleri güncelle (ViewModel'den çağrılır)
     */
    fun updatePlacedShapes(newPlacedShapes: Map<Int, ShapePuzzleViewModel.PuzzleShape>) {
        placedShapes.clear()
        placedShapes.putAll(newPlacedShapes)
        invalidate() // Request redraw
    }

    /**
     * Slot tıklama dinleyicisini ayarla (Fragment tarafından)
     */
    fun setOnShapeDroppedListener(listener: OnShapeDroppedListener) { // Renamed listener type
        this.listener = listener
    }

    /**
     * Set grid dimensions programmatically. Call before view is measured/laid out or call requestLayout().
     */
    fun setGridDimensions(rows: Int, cols: Int) {
        if (rows > 0 && cols > 0) {
            gridRows = rows
            gridCols = cols
            setupPuzzleSlots(width, height) // Recalculate slots based on current size
            invalidate()
        } else {
            Log.w("PuzzleAreaView", "Attempted to set invalid grid dimensions: $rows x $cols")
        }
    }
}
