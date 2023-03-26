package com.bicycle.view

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bicycle.databinding.FragmentMainBinding
import com.bicycle.model.Bike
import com.bicycle.repository.BikeRepository


class MainFragment : Fragment() {


    private lateinit var viewModel: BikeViewModel
    private var timer: CountDownTimer? = null
    private val TIMER_DURATION = 5000L // 5 секунд

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

        viewModel.bikes.observe(viewLifecycleOwner) { bikes ->
            bikeAdapter = BikeAdapter(requireContext(), bikes)
            binding.gridView.adapter = bikeAdapter
        }

        binding.gridView.setOnItemClickListener { _, _, position, _ ->
            val bike = bikeAdapter.getItem(position) as Bike
            viewModel.onBikeSelected(bike)
        }

        viewModel.selectedBike.observe(viewLifecycleOwner) { selectedBike ->
            selectedBike?.let {
                startTimer(it)
            } ?: run {
                stopTimer()
            }
        }

        binding.gridView.setOnItemClickListener { _, _, position, _ ->
            val bike = bikeAdapter.getItem(position) as Bike
            viewModel.onBikeSelected(bike)
        }
    }

    private fun startTimer(bike: Bike) {
        timer?.cancel()

        timer = object : CountDownTimer(TIMER_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateBikeCellColor(bike, Color.WHITE)
            }

            override fun onFinish() {
                updateBikeCellColor(bike)
                viewModel.onTimerFinished()
            }
        }
        timer?.start()
    }

    private fun stopTimer() {
        timer?.cancel()
    }

    private fun updateBikeCellColor(bike: Bike, color: Int= bike.color) {
        val position = bikeAdapter.getPosition(bike)
        val cellView = binding.gridView.getChildAt(position)
        cellView?.setBackgroundColor(color)
    }



    private fun toggleBikeDetails(bike: Bike) {
        // Изменение состояния ячейки и отображение дополнительной информации
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }
}