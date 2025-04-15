package com.example.shapelearning.ui.parent // Paketin doğru olduğundan emin olun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shapelearning.databinding.FragmentParentPinBinding // Bu isimle layout oluşturulacak

class ParentPinFragment : Fragment() { // Fragment'tan miras almalı

    private var _binding: FragmentParentPinBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParentPinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Pin giriş mantığını, butonları vb. buraya ekleyin
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}