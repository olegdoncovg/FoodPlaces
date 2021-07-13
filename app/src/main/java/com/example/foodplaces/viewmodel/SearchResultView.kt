package com.example.foodplaces.viewmodel

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SearchResultView(private val recyclerView: RecyclerView, onItemClickListener: CustomAdapter.OnItemClickListener) {

    private val customAdapter: CustomAdapter = CustomAdapter(onItemClickListener)

    init {
        recyclerView.adapter = customAdapter
        hide()
    }

    fun showData(dataSet: List<IPlace>) {
        customAdapter.setData(dataSet)
        show()
    }

    private fun show() {
        recyclerView.visibility = View.VISIBLE
    }

    private fun hide() {
        recyclerView.visibility = View.GONE
    }
}