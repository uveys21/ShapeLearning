package com.example.shapelearning.ui.games.sorting

import android.graphics.Rect
import android.os.Bundle
import android.util.Log // Added
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper // Added
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView // Added
import com.example.shapelearning.databinding.FragmentShapeSortingBinding
import com.example.shapelearning.service.audio.AudioManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.shapelearning.R

@AndroidEntryPoint
class ShapeSortingFragment : Fragment() {

    private var binding: FragmentShapeSortingBinding? = null
    private val viewModel: ShapeSortingViewModel by viewModels()
    private val args: ShapeSortingFragmentArgs by navArgs()

    // Adapters for the three lists
    private lateinit var shapesAdapter: ShapeSortingAdapter
    private lateinit var category1Adapter: ShapeSortingAdapter
    private lateinit var category2Adapter: ShapeSortingAdapter

    // ItemTouchHelper for drag and drop
    private var itemTouchHelper: ItemTouchHelper? = null

    @Inject
    lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, // Modified: use binding?.root
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShapeSortingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerViews()
        setupDragAndDrop() // Added
        observeViewModel()

        Log.d("ShapeSorting", "Loading level ID: ${args.levelId}")
        viewModel.loadLevel(args.levelId)
    }

    private fun setupUI() {
        // Add content descriptions in XML

        binding?.ivBack?.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigateUp()
        }

        binding?.btnCheck?.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            if (viewModel.shapesToSort.value?.isNotEmpty() == true) {
                Toast.makeText(requireContext(), R.string.sorting_incomplete_message, Toast.LENGTH_SHORT).show() // Add string
            } else {
                viewModel.checkSorting()
            }
        }
    }

    private fun setupRecyclerViews() {
        // Need a way for the adapter to know which RecyclerView it's attached to for drag events
        shapesAdapter = ShapeSortingAdapter { viewHolder -> // Lambda to start drag
            itemTouchHelper?.startDrag(viewHolder)
        }
        binding?.rvShapesToSort?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.rvShapesToSort?.adapter = shapesAdapter

        category1Adapter = ShapeSortingAdapter { viewHolder ->
            itemTouchHelper?.startDrag(viewHolder)
        }
        binding?.rvCategory1?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.rvCategory1?.adapter = category1Adapter

        category2Adapter = ShapeSortingAdapter { viewHolder ->
            itemTouchHelper?.startDrag(viewHolder)
        }
        binding?.rvCategory2?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.rvCategory2?.adapter = category2Adapter
    }


    private fun setupDragAndDrop() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            // Hareket yönlerini tanımla (Listeler arası sürükleme için)
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0 // Swipe yönü yok
        ) {
            private var dragFromViewHolder: RecyclerView.ViewHolder? = null
            private var dragFromAdapter: ShapeSortingAdapter? = null

            // onMove genellikle aynı RecyclerView içinde sürükleme içindir.
            // Farklı RecyclerView'lar arası geçişi clearView'da ele almak daha kolaydır.
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Aynı liste içinde otomatik yer değiştirmeyi engelle
            }

            // Sürükleme başladığında veya bittiğinde çağrılır
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    // Sürükleme başladığında ViewHolder'ı ve Adapter'ı sakla
                    dragFromViewHolder = viewHolder
                    dragFromAdapter = viewHolder?.let { getAdapterFromViewHolder(it) }
                    // Görsel geri bildirim (örn. alpha düşürme)
                    viewHolder?.itemView?.alpha = 0.7f
                    Log.d("ShapeSorting", "Drag started from adapter: ${getAdapterName(dragFromAdapter)}")
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    // Sürükleme bittiğinde veya iptal edildiğinde görünümü eski haline getir
                    // Not: clearView zaten çağrıldığı için alpha burada sıfırlanabilir,
                    // ancak drop dışarıda olursa diye burada da yapmak güvenli olabilir.
                    // dragFromViewHolder?.itemView?.alpha = 1.0f
                    // dragFromViewHolder = null // clearView'da sıfırlanacak
                    // dragFromAdapter = null
                }
            }

            // Sürükleme bittiğinde ve view serbest bırakıldığında çağrılır
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // Görsel geri bildirimi kaldır
                viewHolder.itemView.alpha = 1.0f

                val sourceAdapter = dragFromAdapter
                val sourceViewHolder = dragFromViewHolder
                val sourcePosition = sourceViewHolder?.bindingAdapterPosition ?: RecyclerView.NO_POSITION

                if (sourceAdapter != null && sourceViewHolder != null && sourcePosition != RecyclerView.NO_POSITION) {
                    // Bırakılan konuma göre hedef RecyclerView'ı bul
                    val targetPair = findTargetRecyclerView(viewHolder.itemView)

                    if (targetPair != null) {
                        val targetAdapter = targetPair.first
                        val targetRecyclerView = targetPair.second

                        // Farklı bir listeye bırakıldıysa işlemi yap
                        if (targetAdapter != sourceAdapter) {
                            val shapeToMove = sourceAdapter.currentList.getOrNull(sourcePosition)
                            if (shapeToMove != null) {
                                Log.d("ShapeSorting", "Moving shape ${shapeToMove.id} from ${getAdapterName(sourceAdapter)} to ${getAdapterName(targetAdapter)}")
                                viewModel.moveShape(
                                    shape = shapeToMove,
                                    sourceAdapterType = getAdapterType(sourceAdapter),
                                    targetAdapterType = getAdapterType(targetAdapter)
                                )
                                audioManager.playSound(R.raw.shape_move)
                            } else {
                                Log.w("ShapeSorting", "Could not get shape to move at pos $sourcePosition from ${getAdapterName(sourceAdapter)}")
                            }
                        } else {
                            Log.d("ShapeSorting", "Dropped back onto the same list type: ${getAdapterName(sourceAdapter)}")
                        }
                    } else {
                        Log.d("ShapeSorting", "Dropped outside any valid target RecyclerView.")
                        // Opsiyonel: Geçersiz bırakma animasyonu veya geri bildirim
                    }
                } else {
                    Log.w("ShapeSorting", "Drag ended but source info was invalid.")
                }

                // Saklanan referansları temizle
                dragFromViewHolder = null
                dragFromAdapter = null
            }

            // TODO: Bu fonksiyonu daha güvenilir hale getir! Global koordinatları kullan.
            private fun findTargetRecyclerView(draggedItemView: View): Pair<ShapeSortingAdapter, RecyclerView>? {
                val location = IntArray(2)
                draggedItemView.getLocationOnScreen(location) // Sürüklenen öğenin ekran koordinatları
                val dropX = location[0] + draggedItemView.width / 2 // Bırakma noktasının X merkezi
                val dropY = location[1] + draggedItemView.height / 2 // Bırakma noktasının Y merkezi

                val rvLocations = mapOf(
                    binding?.rvShapesToSort to shapesAdapter,
                    binding?.rvCategory1 to category1Adapter,
                    binding?.rvCategory2 to category2Adapter
                )

                for ((rv, adapter) in rvLocations) {
                    val rvLocation = IntArray(2)
                    rv.getLocationOnScreen(rvLocation)
                    val rvRect = Rect(
                        rvLocation[0],
                        rvLocation[1],
                        rvLocation[0] + rv.width,
                        rvLocation[1] + rv.height
                    )
                    // Eğer bırakma noktası bu RecyclerView'ın sınırları içindeyse
                    if (rvRect.contains(dropX, dropY)) {
                        Log.d("ShapeSorting", "Drop target identified: ${getAdapterName(adapter)}")
                        return Pair(adapter, rv)
                    }
                }
                Log.d("ShapeSorting", "No target RecyclerView found at drop location.")
                return null // Hedef bulunamadı
            }

            // Hangi ViewHolder'ın hangi adaptöre ait olduğunu bulan yardımcı fonksiyon
            private fun getAdapterFromViewHolder(viewHolder: RecyclerView.ViewHolder): ShapeSortingAdapter? {
                return when (viewHolder.bindingAdapter) {
                    is ShapeSortingAdapter -> viewHolder.bindingAdapter as ShapeSortingAdapter
                    else -> null
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Kullanılmıyor */ }

            override fun isLongPressDragEnabled(): Boolean = false
            override fun isItemViewSwipeEnabled(): Boolean = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper?.attachToRecyclerView(binding?.rvShapesToSort)
        itemTouchHelper?.attachToRecyclerView(binding?.rvCategory1)
        itemTouchHelper?.attachToRecyclerView(binding?.rvCategory2)
    }
    // --- End Drag and Drop Setup ---

    // getAdapterType ve getAdapterName fonksiyonları aynı kalabilir...

    // observeViewModel, showFeedback, onDestroyView fonksiyonları aynı kalabilir...
    // --- End Drag and Drop Setup ---

    // Helper to identify adapter types
    private fun getAdapterType(adapter: ShapeSortingAdapter): Int {
        return when (adapter) {
            shapesAdapter -> ShapeSortingAdapter.SHAPES_SOURCE
            category1Adapter -> ShapeSortingAdapter.CATEGORY_1
            category2Adapter -> ShapeSortingAdapter.CATEGORY_2
            else -> -1 // Invalid
        }
    }
    private fun getAdapterName(adapter: ShapeSortingAdapter?): String {
        return when (adapter) {
            shapesAdapter -> "Source"
            category1Adapter -> "Category1"
            category2Adapter -> "Category2"
            null -> "NullAdapter" // Null durumu için
            else -> "Unknown"
        }
    }


    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading -> // Modified: Use binding?.let
            binding?.progressBar?.isVisible = isLoading
            binding?.contentGroup?.isVisible = !isLoading
        }

        viewModel.shapesToSort.observe(viewLifecycleOwner) { shapes ->
            shapesAdapter.submitList(shapes)
            Log.d("ShapeSorting", "Shapes to sort updated: ${shapes.size}")
        }

        viewModel.category1Shapes.observe(viewLifecycleOwner) { shapes ->
            category1Adapter.submitList(shapes)
            Log.d("ShapeSorting", "Category 1 updated: ${shapes.size}")
        }

        viewModel.category2Shapes.observe(viewLifecycleOwner) { shapes ->
            category2Adapter.submitList(shapes)
            Log.d("ShapeSorting", "Category 2 updated: ${shapes.size}")
        }

        // Observe sorting criteria and category names
        viewModel.sortingCriteriaText.observe(viewLifecycleOwner) { criteria ->
            binding?.tvInstructions?.text = criteria
        }
        viewModel.category1Name.observe(viewLifecycleOwner) { name ->
            binding?.tvCategory1?.text = name
        }
        viewModel.category2Name.observe(viewLifecycleOwner) { name ->
            binding?.tvCategory2?.text = name
        }

        viewModel.sortingResult.observe(viewLifecycleOwner) { result ->
            result?.getContentIfNotHandled()?.let { sortingResult -> // Use event wrapper
                when (sortingResult) {
                    ShapeSortingViewModel.SortingResult.SUCCESS -> {
                        audioManager.playSound(R.raw.success)
                        showFeedback(true)
                        viewModel.saveProgress() // Save on success
                    }
                    ShapeSortingViewModel.SortingResult.FAILURE -> {
                        audioManager.playSound(R.raw.failure)
                        showFeedback(false)
                        // Optional: Automatically move items back after delay?
                        // viewModel.resetSorting() // Add reset function if needed
                    }
                    ShapeSortingViewModel.SortingResult.INCOMPLETE -> {
                        // This case handled by btnCheck listener now
                        // Toast.makeText(requireContext(), R.string.sorting_incomplete_message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("ShapeSorting", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }
    }

    // Removed onShapeDragged override as it's handled by ItemTouchHelper now

    private fun showFeedback(isSuccess: Boolean) {
        // TODO: Implement better visual feedback (animations, highlighting)
        val messageRes = if (isSuccess) R.string.sorting_success else R.string.sorting_failure
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()

        // Example: Highlight categories or show overlay
        // binding.category1Layout.setBackgroundColor(if (isSuccess) Color.GREEN else Color.RED)
        // binding.category2Layout.setBackgroundColor(if (isSuccess) Color.GREEN else Color.RED)
        // view?.postDelayed({
        //    binding.category1Layout.setBackgroundColor(Color.TRANSPARENT)
        //    binding.category2Layout.setBackgroundColor(Color.TRANSPARENT)
        // }, 1500)
    }

    override fun onDestroyView() {
        itemTouchHelper?.attachToRecyclerView(null) // Detach helper
        itemTouchHelper = null
        binding?.rvShapesToSort?.adapter = null
        binding?.rvCategory1?.adapter = null
        binding?.rvCategory2?.adapter = null
        binding = null
        super.onDestroyView()
    }

}
