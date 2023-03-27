package com.bicycle.view

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.media.RingtoneManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bicycle.R
import com.bicycle.model.Bike
import java.text.SimpleDateFormat
import java.util.*

class BikeAdapter(private val context: Context, val onItemClickListener: ((Bike) -> Unit)? = null, val onItemLongClickListener: ((Bike) -> Boolean)? = null) : RecyclerView.Adapter<BikeAdapter.ViewHolder>() {

    private var bikes: List<Bike> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        val itemHeight = (parent.height * 0.25).toInt()
        view.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight)
        return ViewHolder(view)
    }

    override fun getItemCount() = bikes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bike = bikes[position]
        holder.bind(bike)
        holder.itemView.setOnClickListener { onItemClickListener?.invoke(bike) }
        holder.itemView.setOnLongClickListener { onItemLongClickListener?.invoke(bike) ?: false }

    }

    fun updateData(newBikes: List<Bike>) {
        val diffCallback = BikeDiffCallback(bikes, newBikes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        bikes = newBikes
        diffResult.dispatchUpdatesTo(this)
    }

    fun getItem(position: Int)=bikes[position]

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvBikeName: TextView = view.findViewById(R.id.tvBikeName)
        private val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        private val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        private val tvStartTime: TextView = view.findViewById(R.id.tvStartTime)
        private val tvEndTime: TextView = view.findViewById(R.id.tvEndTime)
        private val tvTimer: TextView = view.findViewById(R.id.tvTimer)

        fun bind(bike: Bike) {

            val color = if(bike.endTime>5*1000) Color.WHITE else if(bike.endTime>0) {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(itemView.context, notification)
                ringtone.play()
                val animator = ObjectAnimator.ofInt(itemView, "backgroundColor", Color.WHITE, Color.RED)
                animator.duration = 1000
                animator.setEvaluator(ArgbEvaluator())
                animator.start()
                Color.RED
            } else bike.color
            itemView.setBackgroundColor(color)

            tvBikeName.text = bike.name
            tvPrice.text = "${bike.price}"
            tvDuration.text = "- ${bike.rentDuration}"
            tvStartTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(bike.startTime)
            tvTimer.text = SimpleDateFormat("mm:ss", Locale.getDefault()).format(bike.endTime)
            tvEndTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(bike.startTime+bike.rentDuration*60*1000)



           // val itemHeight = ((itemView.parent as ViewGroup).height * 0.25).toInt() - 4
           // itemView.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight)
        }
    }

    class BikeDiffCallback(private val oldList: List<Bike>, private val newList: List<Bike>) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldBike = oldList[oldItemPosition]
            val newBike = newList[newItemPosition]
            return oldBike.name == newBike.name
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldBike = oldList[oldItemPosition]
            val newBike = newList[newItemPosition]
            return oldBike == newBike
        }
    }
}
