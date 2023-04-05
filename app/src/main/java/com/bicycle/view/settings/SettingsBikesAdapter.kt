package com.bicycle.view.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bicycle.R
import com.bicycle.model.Bike
import com.bicycle.repository.randomColors

class SettingsBikesAdapter(
    private var bikes: List<Bike>,
    private val onBikeUpdated: (Bike) -> Unit
) : RecyclerView.Adapter<SettingsBikesAdapter.BikeViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BikeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.settings_bike_list_item, parent, false)
        return BikeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BikeViewHolder, position: Int) {
        holder.bind(bikes[position], onBikeUpdated)
    }

    override fun getItemCount() = bikes.size

    class BikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameEditText: EditText = itemView.findViewById(R.id.nameEditText)
        private val priceEditText: EditText = itemView.findViewById(R.id.priceEditText)
        private val rentDurationEditText: EditText =
            itemView.findViewById(R.id.rentDurationEditText)
        val colorLinearLayout: LinearLayout = itemView.findViewById(R.id.colorLinearLayout)



        // Добавьте другие поля для редактирования здесь

        fun bind(bike: Bike, onBikeUpdated: (Bike) -> Unit) {
            nameEditText.setText(bike.name)
            priceEditText.setText(bike.price)
            rentDurationEditText.setText(bike.rentDuration.toString())


            // Заполните другие поля значениями из объекта Bike

            nameEditText.doOnTextChanged { text, _, _, _ ->
                onBikeUpdated(bike.copy(name = text.toString()))
            }
            priceEditText.doOnTextChanged { text, _, _, _ ->
                onBikeUpdated(bike.copy(price = text.toString()))
            }
            rentDurationEditText.doOnTextChanged { text, _, _, _ ->
                if(!text.isNullOrBlank())
                onBikeUpdated(bike.copy(rentDuration = text.toString().toLong()))
            }

            colorLinearLayout.removeAllViews()
            colorLinearLayout.setBackgroundColor(bike.color)
            randomColors.forEachIndexed { index, color ->
                val colorView = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    setBackgroundColor(color)
                    setOnClickListener {
                        onBikeUpdated(bike.copy(color = color))
                        colorLinearLayout.setBackgroundColor(color)
                    }
                }
                colorLinearLayout.addView(colorView)
            }
            colorLinearLayout.setBackgroundColor(bike.color)
        }
    }
}



data class UpdatedField(
    val name: String,
    val price: String,
    val rentDuration: Long,
    var startTime: Long,
    var endTime: Long,
    val color: Int
)