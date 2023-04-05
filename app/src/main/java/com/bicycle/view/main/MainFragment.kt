package com.bicycle.view.main

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bicycle.R
import com.bicycle.databinding.FragmentMainBinding
import com.bicycle.model.StatusBike
import com.bicycle.repository.AppState


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
        viewModel = ViewModelProvider(this)[BikeViewModel::class.java]

        bikeAdapter = BikeAdapter(requireContext(),
            onItemClickListener = { bike ->
                // Обработка нажатия на элемент
                if(bike.status==StatusBike.WAIT_FOR_CANCEL)
                viewModel.pressBike(bike)
            },
            onItemLongClickListener = { bike ->
                view.isPressed = true
                viewModel.pressBike(bike)
                true
            }
        )
        binding.gridView.adapter = bikeAdapter
        binding.gridView.layoutManager = GridLayoutManager(requireContext(), 6)

        viewModel.bikes.observe(viewLifecycleOwner) { bikes ->
            bikeAdapter.updateData(bikes)
        }

        viewModel.appStateLiveData.observe(viewLifecycleOwner) { appState ->
            render(appState)
        }

    }

    private fun render(appState: AppState) {
        when (appState) {
            is AppState.Success -> {
                showToast(requireContext(), appState.message)
            }
            is AppState.Error -> {
                showErrorDialog(requireContext(), getString(R.string.error), appState.message)
            }
            AppState.Loading -> {}
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Displays an error dialog with the given title and message.
     * @param context the context of the calling activity
     * @param title the title of the dialog
     * @param message the message to display
     */
    private fun showErrorDialog(context: Context, title: String, message: String) {
        (context as Activity).runOnUiThread {
            val dialogBuilder = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.ok_button_text)) { _, _ -> }
            val dialog = dialogBuilder.create()
            dialog.show()
        }
    }

}