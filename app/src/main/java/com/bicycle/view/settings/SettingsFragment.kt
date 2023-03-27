package com.bicycle.view.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bicycle.R
import com.bicycle.model.Bike
import com.bicycle.repository.BikeRepository

class SettingsFragment : Fragment() {
    private lateinit var bikeRepository: BikeRepository
    private lateinit var bikes: List<Bike>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    lateinit var bikesAdapter:SettingsBikesAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bikeRepository = BikeRepository()
        bikes = bikeRepository.getBikes()

        bikesAdapter = SettingsBikesAdapter(bikes) { bike ->
            // Обработка изменений в полях


            bikeRepository.updateBike(bike) {
                // Обновление списка велосипедов после успешного обновления
                bikes = bikeRepository.getBikes()
                //bikesAdapter.updateBikes(bikes)
            }
        }

        val recyclerView: RecyclerView = view.findViewById(R.id.bikes_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = bikesAdapter
    }
}
