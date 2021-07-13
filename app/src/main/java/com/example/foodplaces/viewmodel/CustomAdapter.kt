package com.example.foodplaces.viewmodel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplaces.R
import java.util.*

class CustomAdapter(private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    private var localDataSet: MutableList<IPlace> = ArrayList()

    fun setData(dataSet: List<IPlace>) {
        localDataSet.clear()
        localDataSet.addAll(dataSet)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.suggestion, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = localDataSet[position].getFullInfo()
        viewHolder.textView.setOnClickListener { v: View? -> onItemClickListener.onItemClick(localDataSet[position]) }
    }

    override fun getItemCount(): Int {
        return localDataSet.size
    }

    interface OnItemClickListener {
        fun onItemClick(item: IPlace)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.suggestion_address)
    }
}