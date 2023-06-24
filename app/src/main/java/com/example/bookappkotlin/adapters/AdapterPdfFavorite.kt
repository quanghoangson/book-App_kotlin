package com.example.bookappkotlin.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookappkotlin.MyApplication
import com.example.bookappkotlin.activities.PdfDetailActivity
import com.example.bookappkotlin.activities.ProfileActivity
import com.example.bookappkotlin.databinding.RowPdfFavoriteBinding
import com.example.bookappkotlin.models.ModelPdf
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterPdfFavorite : RecyclerView.Adapter<AdapterPdfFavorite.HolderPdfFavorite> {

    //context
    private val context: Context
    private var booksArrayList: ArrayList<ModelPdf>
    private lateinit var binding: RowPdfFavoriteBinding


    constructor(context: Context, booksArrayList: ArrayList<ModelPdf>):super() {
        this.context = context
        this.booksArrayList = booksArrayList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfFavorite {
        //bind/ inflate row_pdf_favorite.xml
        binding = RowPdfFavoriteBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfFavorite(binding.root)
    }

    override fun getItemCount(): Int {
        return booksArrayList.size
    }

    override fun onBindViewHolder(holder: HolderPdfFavorite, position: Int) {
//        get data set data, handle click

        //get data ; form [users->uid->favorites] we will only have ids of favorite books so we have to load their details from[books] node
        val model = booksArrayList[position]
        loadBookDetails(model, holder)

        //handle open pdf  details page, pass book id to load details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId", model.id)// pass book id not category id
            context.startActivity(intent)
        }

        //handle click remove
        holder.removeFavBtn.setOnClickListener {
            MyApplication.removeFromFavorite(context, model.id)
        }
    }

    private fun loadBookDetails(model: ModelPdf, holder: AdapterPdfFavorite.HolderPdfFavorite) {
        val bookId = model.id
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book info
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    val title = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    val url = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    //set data to model
                    model.isFavorite = true
                    model.title = title
                    model.description = description
                    model.categoryId = categoryId
                    model.timestamp = timestamp.toLong()
                    model.uid = uid
                    model.url = url
                    model.viewsCount = viewsCount.toLong()
                    model.downloadsCount = downloadsCount.toLong()
                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())
                    MyApplication.loadCategory("$categoryId", holder.categoryTv)
                    MyApplication.loadPdfFromUrlSinglePage(
                        "$url",
                        "$title",
                        holder.pdfView,
                        holder.progressBar,
                        null
                    )
                    MyApplication.loadPdfSize("$url", "$title", holder.sizeTv)

                    holder.titleTv.text = title
                    holder.descriptionTv.text = description
                    holder.dateTv.text = date
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    //    view hodel class to manage ui views of row_pdf_favorite.xml
    inner class HolderPdfFavorite(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //init ui views
        var pdfView = binding.pdfView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var removeFavBtn = binding.removeFavBtn
        var descriptionTv = binding.descriptionTv
        var categoryTv = binding.categoryTv
        var sizeTv = binding.sizeTv
        var dateTv = binding.dateTv
    }


}