package com.example.shapelearning.ui.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shapelearning.databinding.FragmentParentZoneBinding // Layout adı

class ParentZoneFragment : Fragment() { // Miras

    private var _binding: FragmentParentZoneBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParentZoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Ebeveyn kontrol paneli içeriğini (ayarlar, raporlar vb.) buraya ekleyin
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}