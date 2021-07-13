package com.example.foodplaces.datamovel

import com.example.foodplaces.viewmodel.IPlace

interface IResult {
    fun onSuccess(places: List<IPlace>, dataSource: DataSource)
    fun onFailed(errorMessage: String)
}