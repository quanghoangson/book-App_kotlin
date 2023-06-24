package com.example.bookappkotlin.filters

import android.widget.Filter
import com.example.bookappkotlin.adapters.AdepterCategory
import com.example.bookappkotlin.models.ModelCategory

class FilterCategory: Filter {

    //arraylist in which we want to search
    private var filterList: ArrayList<ModelCategory>

    //adepter in which  need to be implemented
    private var adepterCategory: AdepterCategory

    //constructer
    constructor(filterList: ArrayList<ModelCategory>, adepterCategory: AdepterCategory) : super() {
        this.filterList = filterList
        this.adepterCategory = adepterCategory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        //value should not be null and not empty
        if(constraint !=null && constraint.isNotEmpty()){
            //searched value is nor null not empty

            //change to upper case., or lower case to avoid case sensitivity
            constraint = constraint.toString().uppercase()
            val filteredModels:ArrayList<ModelCategory> = ArrayList()
            for (i in 0 until filterList.size){
                //validate
                if(filterList[i].category.uppercase().contains(constraint)){
                    //add to filtered list
                    filteredModels.add(filterList[i])
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        }
        else{
            //search value is either null or empty
            results.count = filterList.size
            results.values = filterList
        }
        return results //don't miss it
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        //apply filter changes
        adepterCategory.categoryArrayList = results.values as ArrayList<ModelCategory>
        // notify channges
        adepterCategory.notifyDataSetChanged()
    }
}