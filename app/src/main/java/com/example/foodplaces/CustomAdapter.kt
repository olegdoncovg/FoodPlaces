package com.example.foodplaces

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplaces.realm.PlaceRealm
import java.util.*

class CustomAdapter(private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    private val localDataSet: MutableList<PlaceRealm>

    init {
        this.localDataSet = ArrayList()
    }

    fun setData(dataSet: List<PlaceRealm>) {
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
        viewHolder.textView.text = localDataSet[position].fullInfo
        viewHolder.textView.setOnClickListener { v: View? -> onItemClickListener.onItemClick(localDataSet[position]) }
    }

    override fun getItemCount(): Int {
        return localDataSet.size
    }

    interface OnItemClickListener {
        fun onItemClick(item: PlaceRealm?)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.suggestion_address)
    }
}