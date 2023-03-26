package com.bicycle.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.TextView
import com.bicycle.R
import com.bicycle.model.Bike

class BikeAdapter(private val context: Context, private val bikes: List<Bike>) : BaseAdapter() {

    override fun getCount() = bikes.size

    override fun getItem(position: Int): Any = bikes[position]

    override fun getItemId(position: Int): Long = position.toLong()

    fun getPosition(bike: Bike): Int {
        return bikes.indexOf(bike)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_view, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val bike = getItem(position) as Bike
        viewHolder.tvBikeName.text = bike.name
        viewHolder.tvPrice.text = "${bike.price}"
        viewHolder.tvDuration.text = "- ${bike.rentDuration}"
        viewHolder.tvStartTime.text = bike.startTime
        viewHolder.tvEndTime.text = bike.endTime



        val itemHeight = ((parent.height) * 0.25).toInt()-4
        view.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight)

        view.setBackgroundColor(bike.color)
        return view
    }

    private class ViewHolder(view: View) {
        val tvBikeName: TextView = view.findViewById(R.id.tvBikeName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvStartTime: TextView = view.findViewById(R.id.tvStartTime)
        val tvEndTime: TextView = view.findViewById(R.id.tvEndTime)
    }
}

