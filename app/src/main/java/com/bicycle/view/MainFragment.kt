package com.bicycle.view

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bicycle.databinding.FragmentMainBinding
import com.bicycle.model.Bike
import com.bicycle.model.StatusBike
import com.bicycle.repository.BikeRepository


class MainFragment : Fragment() {


    private lateinit var viewModel: BikeViewModel

    private lateinit var bikeAdapter: BikeAdapter

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)




        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(BikeViewModel::class.java)

        bikeAdapter = BikeAdapter(requireContext(),
            onItemClickListener = { bike ->
                // Обработка нажатия на элемент
                if(bike.status==StatusBike.WAIT_FOR_CANCEL)
                viewModel.pressBike(bike)
            },
            onItemLongClickListener = { bike ->
                view.isPressed = true
                viewModel.pressBike(bike)
                true // возвращаем true, чтобы сказать, что событие обработано
            }
        )
        binding.gridView.adapter = bikeAdapter
        binding.gridView.layoutManager = GridLayoutManager(requireContext(), 6)

        viewModel.bikes.observe(viewLifecycleOwner) { bikes ->
            bikeAdapter.updateData(bikes)
        }

    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}