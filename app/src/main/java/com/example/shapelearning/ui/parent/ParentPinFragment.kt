package com.example.shapelearning.ui.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shapelearning.databinding.FragmentParentPinBinding

class ParentPinFragment : Fragment() {

    private var _binding: FragmentParentPinBinding? = null
    private val binding get() = _binding!! // Non-null assertion, use with caution

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentParentPinBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding?.apply {
            // TODO: Set up your views and listeners here
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}