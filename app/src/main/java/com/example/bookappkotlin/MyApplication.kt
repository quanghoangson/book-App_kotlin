package com.example.bookappkotlin

import android.app.Application
import android.app.ProgressDialog
import android.content.Context

import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.bookappkotlin.activities.PdfDetailActivity
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage


import java.util.*
import kotlin.collections.HashMap

class MyApplication :Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object{
        //create a static method to conver t timestamp to proper date format. so can use it everywhere in project, no need to rewirte again
        fun formatTimeStamp (timestamp : Long):String{
            val cal  = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp
            //format dd//mm/yyyy
            return DateFormat.format("dd/MM/yyyy", cal).toString()
        }

        //function to get pdf size
        fun loadPdfSize (pdfUrl: String, pdfTitle: String,  sizeTv :TextView){
            val TAG ="PDF_SIZE_TAG"

            //using url we can get file and  is a metadata from firebase storage
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.metadata
                .addOnSuccessListener { storageMetadata ->
                    Log.d(TAG, "LoadPdfSize: got metadata")
                    val bytes = storageMetadata.sizeBytes.toDouble()
                    Log.d(TAG, "LoadPdfSize: Size Bytes $bytes")

//                    convert bytes to KB/MB
                    val kb = bytes/1024
                    val mb = kb/1024
                    if (mb >=1 ){
                        sizeTv.text="${String.format("%.2f", mb)} MB"
                    }
                    else if (kb >=1){
                        sizeTv.text="${String.format("%.2f", kb)} KB"
                    }else{
                        sizeTv.text="${String.format("%.2f", bytes)} bytes"
                    }
                }
                .addOnFailureListener {e->
                    //failed to get metadata
                    Log.d(TAG, "LoadPdfSize: Failed to get metada due to ${e.message}")
                }
        }
//        instead of making new function loadpdfpagecount() to just load pages count it would be more good to ues some existing function to do that
//        i.e loadPdf FromUrlSinglePage
//        we will add another  parameter of type textview e.g. pagesTv
//        whenever we call that function
//        1, if we require page numbers we will pass  pageTv(textView
//        2, if ew don't require pagfe number we will passs null
//        add in function if pageTv (textView) parameter is not null we will set the page number count
        fun loadPdfFromUrlSinglePage(pdfUrl: String, pdfTitle: String, pdfView: PDFView, progressBar: ProgressBar, pagesTv:TextView?){
        val TAG ="PDF_THUMBNNAIL_TAG"
                //using url we can get file and its metadata from firebase storage
        val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        ref.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "LoadPdfSize: Size Bytes $bytes")

                //set to pdfview
                pdfView.fromBytes(bytes)
                    .pages(0)
                    .spacing(0)
                    .swipeHorizontal(false)
                    .enableSwipe(false)
                    .onError{t->
                        progressBar.visibility = View.INVISIBLE
                        Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                    }
                    .onPageError { page, t ->
                        progressBar.visibility = View.INVISIBLE
                        Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                    }
                    .onLoad { nbPages ->
                        Log.d(TAG, "loadPdfFromUrlSingPage: Pages: $nbPages")
                        //pdf loaded , we can set page count, pdf thumbnail
                        progressBar.visibility = View.INVISIBLE

                        //if pagesTv param is not null then set page number
                        if (pagesTv !=null){
                            pagesTv.text = "$nbPages"
                        }
                    }
                    .load()
            }
            .addOnFailureListener {e->
                //failed to get metadata
                Log.d(TAG, "LoadPdfSize: Failed to get metada due to ${e.message}")
            }
        }

        fun loadCategory( categoryId: String, categoryTv : TextView){
            //load category using category if from firebase
            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get cateroy
                        val category = "${snapshot.child("category").value}"

                        //set category
                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }

        fun deleteBook(context: Context, bookId: String, bookUrl: String, bookTitle :String){
            //param details
//            1. context , used when require e.g. for progressdialog , toast
//            2. bookid , to delete book from db
//            3. book url .delete booko from firebse storage
//            4. booktitle, show in dialog etc
            val TAG = "DELETE_BOOK_TAG"
            Log.d(TAG,"deleteBook: deleting...")
            //progress dialog
            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("please wait")
            progressDialog.setMessage("Delete $bookTitle...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            Log.d(TAG, "deleteBook: Deleting from storage...")
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "deleteBook: Deleting from storage...")
                    Log.d(TAG, "deleteBook: Deleting from db now.")

                    val ref = FirebaseDatabase.getInstance().getReference("Books")
                    ref.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(context, "Successfully deleted...", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "deleteBook : deleted from db too...")
                        }
                        .addOnFailureListener { e->
                            progressDialog.dismiss()
                            Log.d(TAG, "deleteBook : failed deleted from db due to ${e.message}")
                            Toast.makeText(context, "failed deleted from db due to ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e->
                    progressDialog.dismiss()
                    Log.d(TAG, "deleteBook : Failed to delete from storage due to ${e.message}")
                    Toast.makeText(context, "failed deleted from db due to ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        fun incrementBookViewCount( bookId: String){
            //get current book views count
            val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get  views count
                        var viewsCount = "${snapshot.child("viewsCount").value}"

                      if (viewsCount=="" ||  viewsCount=="null"){
                          viewsCount ="0";
                      }
                        //2 increment views count
                        val newViewsCount = viewsCount.toLong() +1

                        //set up data to update in db
                        val hashMap = HashMap<String, Any>()
                        hashMap["viewsCount"]= newViewsCount

                        //set to db
                        val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                        dbRef.child(bookId)
                            .updateChildren(hashMap)

                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }

        public fun removeFromFavorite(context: Context, bookId: String){
            val TAG = "REMOVE_FAV_TAG"
            Log.d(TAG, "removeFromFavorite : removing from fav")

            val firebaseAuth = FirebaseAuth.getInstance()
            //database ref
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
                .removeValue()
                .addOnSuccessListener {
                    //added to fav
                    Log.d(TAG, "removeFromFavorite: Removed from fav")
                    Toast.makeText(context, "Removed from favorite", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {e->
                    //failed to add to fav
                    Log.d(TAG, "addToFavorite : Failed to remove from to fav due to ${e.message}")
                    Toast.makeText(context, "Failed to remove from to fav due to ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}