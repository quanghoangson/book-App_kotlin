package com.example.bookappkotlin.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.bookappkotlin.filters.FilterCategory
import com.example.bookappkotlin.models.ModelCategory
import com.example.bookappkotlin.activities.PdfListAdminActivity
import com.example.bookappkotlin.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdepterCategory :RecyclerView.Adapter<AdepterCategory.HolderCategory>, Filterable {

    private  val context:Context
    public var categoryArrayList: ArrayList<ModelCategory>
    private var filterList: ArrayList<ModelCategory> //luu tru

    private var filter : FilterCategory?= null

    private lateinit var  binding: RowCategoryBinding
    //constructor
    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
        this.filterList= categoryArrayList
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        //inflate / bind row_category.xml
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderCategory(binding.root)
    }

    @SuppressLint("SuspiciousIndentation")  //fix lỗi
    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        //        get data , set data , handle clicks etc
        //get data
        val model = categoryArrayList[position]
        val id = model.id
        val category= model.category
        val uid =model.uid
        val timestamp = model.timestamp

        //setdata
        holder.categoryTv.text = category

        //handle click, delete category
        holder.deletebtn.setOnClickListener{
            //confirm before delete
            val builder = AlertDialog.Builder(context)
                builder.setTitle("Deletes")
                    .setMessage("Are sure you want to delete this category")
                    .setPositiveButton("Confirm"){ a,d->
                        Toast.makeText(context, "Deleting...", Toast.LENGTH_SHORT).show()
                        deleteCategory(model, holder)
                    }
                    .setNegativeButton("Cancel"){a, d->
                        a.dismiss()
                    }
                    .show()
        }

        //handle click , start pdf list admin activity , also pas pdf id , title
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfListAdminActivity::class.java)
            intent.putExtra("categoryId", id)
            intent.putExtra("category", category)
            context.startActivity(intent)
        }
    }

    private fun deleteCategory(model: ModelCategory, holder: HolderCategory) {
        //get id of category to delete
        val id = model.id
        //firebasse DB > categories > categoryId
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted...", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                Toast.makeText(context, "unable to delete due to ${e.message}...", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int {
        return categoryArrayList.size //number of items in list

    }
    //viewHolder class to hold/init UI views for row_category.xml
    inner class HolderCategory(itemView: View): RecyclerView.ViewHolder(itemView){  //lớp con cho phép truy cập vào các thuộc tính và phương thức của lớp chứa nó.
        //init ui views
        var categoryTv:TextView =  binding.categoryTv  //tên
        var deletebtn:ImageButton= binding.deleteBtn  //
    }

    override fun getFilter(): Filter {
        if(filter == null)
        {
            filter = FilterCategory(filterList, this)

        }
        return filter as FilterCategory
    }

}