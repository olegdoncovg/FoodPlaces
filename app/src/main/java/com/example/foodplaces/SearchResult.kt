package com.example.foodplaces

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplaces.realm.PlaceRealm

class SearchResult(private val recyclerView: RecyclerView, onItemClickListener: CustomAdapter.OnItemClickListener) {

    private val customAdapter: CustomAdapter = CustomAdapter(onItemClickListener)

    init {
        hide()
    }

    fun setData(dataSet: List<PlaceRealm>) {
        customAdapter.setData(dataSet)
        show()
    }

    fun show() {
        recyclerView.visibility = View.VISIBLE
    }

    fun hide() {
        recyclerView.visibility = View.GONE
    }
}