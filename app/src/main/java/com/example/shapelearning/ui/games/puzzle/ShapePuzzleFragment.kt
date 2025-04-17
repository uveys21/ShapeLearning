package com.example.shapelearning.ui.games.puzzle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shapelearning.databinding.FragmentShapePuzzleBinding

class ShapePuzzleFragment : Fragment() {

    private var _binding: FragmentShapePuzzleBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView.

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShapePuzzleBinding.inflate(inflater, container, false).apply {
            // Example: Set click listeners or initial UI states here if needed.
            // For instance:
            // someButton.setOnClickListener { ... }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Buraya ShapePuzzleFragment'a özel kodlarınızı ekleyin
        // Example: Setup your RecyclerView, observers, or other logic here.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}