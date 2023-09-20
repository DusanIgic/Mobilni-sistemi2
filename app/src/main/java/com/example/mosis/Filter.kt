package com.example.mosis

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.mosis.databinding.FragmentFilterBinding
import com.example.mosis.databinding.FragmentLoginBinding


class Filter : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnFilter.setOnClickListener() {

            if(binding.filterautor.text.toString() != "" && binding.filterazdaljina.text.toString() != ""){
                val fila = binding.filterautor.text.toString()
                val filraz = binding.filterazdaljina.text.toString().toInt()
                findNavController().navigate(FilterDirections.actionFilterToMap( filraz, fila))
            }
            else if(binding.filterautor.text.toString() != "") {
                val fila = binding.filterautor.text.toString()
                findNavController().navigate(FilterDirections.actionFilterToMap( 0, fila))
            }
            else if (binding.filterazdaljina.text.toString() != "") {
                val filraz = binding.filterazdaljina.text.toString().toInt()
                findNavController().navigate(FilterDirections.actionFilterToMap( filraz, "qazwsx"))
            }
            else{
                findNavController().navigate(FilterDirections.actionFilterToMap())
            }
        }
    }


}