package com.example.bookappkotlin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.example.bookappkotlin.adapters.AdapterPdfUser
import com.example.bookappkotlin.adapters.AdepterCategory
import com.example.bookappkotlin.databinding.ActivityDashboardAdminBinding
import com.example.bookappkotlin.databinding.FragmentBookUserBinding
import com.example.bookappkotlin.models.ModelPdf
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BooksUserFragment : Fragment{

    //view binding
    private lateinit var binding: FragmentBookUserBinding
    private  lateinit var adepterCategory: AdepterCategory

    companion object{
        private const val TAG = "BOOKS_USER_TAG"

        //receive data from activity to load books e.g. categoryId , category, uid
        public fun newInstance (categoryId: String, category :String, uid :String): BooksUserFragment{
            val fragment = BooksUserFragment()
            //put data to bundle intent
            val args = Bundle()
            args.putString("categoryId", categoryId)
            args.putString("category", category)
            args.putString("uid", uid)
            fragment.arguments = args
            return fragment
        }
    }

    private var categoryId= ""
    private var category= ""
    private var uid= ""

    //arraylist to hold pdfs
    private lateinit var pdfArrayList : ArrayList<ModelPdf>
    private lateinit var adapterPdfUser : AdapterPdfUser

    constructor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding= FragmentBookUserBinding.inflate(layoutInflater)
//        //search
//        binding.searchEt.addTextChangedListener(object: TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
//
//            }
//            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                //called as and when user tupe anything
//                try {
//                    adepterCategory.filter.filter(s)
//                }
//                catch (e: Exception){
//                }
//            }
//            override fun afterTextChanged(p0: Editable?) {
//            }
//        })

        //get arguments that we passed in newInstance method
        val args = arguments
        if (args!= null){
            categoryId = args.getString("categoryId")!!
            category= args.getString("category")!!
            uid = args.getString("uid")!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentBookUserBinding.inflate(LayoutInflater.from(context), container, false)
        
        //load pdf according to category , this fragment will have new instance to load each category pdfs
        Log.d(TAG, "onCreateView: Category: $category")

        if (category == "All"){
            //load all books
            loadAllBooks()
        }
        else if (category == "Most Viewed")
        {
            ///load most viewed book
            loadMostViewedDownloadedBooks("viewsCount")
        }
        else if (category=="Most Downloaded")
        {
            //load most downloaded boo  ks
            loadMostViewedDownloadedBooks("downloadsCount")
        }
        else{
            //load selected caategory books
            loadCategorizedBooks()
        }
        //search
        binding.searchEt.addTextChangedListener{object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    adapterPdfUser.filter.filter(s)
                }
                catch (e:Exception){
                    Log.d(TAG, "onTextChanged: SEARCH EXCEPTION: ${e.message}")
                }
            }

            override fun afterTextChanged(s: Editable?) {
                TODO("Not yet implemented")
            }
        }
        }
        return binding.root
    }

    private fun loadAllBooks() {
        //init list
        pdfArrayList = ArrayList()
        val ref= FirebaseDatabase.getInstance().getReference("Books")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list before starting adding data into it
                pdfArrayList.clear()
                for (ds in snapshot.children){
                    //get data
                    val model = ds.getValue(ModelPdf::class.java)
                    //add to list
                    pdfArrayList.add(model!!)
                }
                //setup adapter
                adapterPdfUser = AdapterPdfUser(context!!, pdfArrayList)
                //set adapter to recyclerview
                binding.booksRv.adapter = adapterPdfUser
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun loadMostViewedDownloadedBooks(orderBy: String) {
        //init list
        pdfArrayList = ArrayList()
        val ref= FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild(orderBy).limitToLast(10) //load 10 most viewed or most downloaded book.
            .addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list before starting adding data into it
                pdfArrayList.clear()
                for (ds in snapshot.children){
                    //get data
                    val model = ds.getValue(ModelPdf::class.java)
                    //add to list
                    pdfArrayList.add(model!!)
                }
                //setup adapter
                adapterPdfUser = AdapterPdfUser(context!!, pdfArrayList)
                //set adapter to recyclerview
                binding.booksRv.adapter = adapterPdfUser
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun loadCategorizedBooks() {
        //init list
        pdfArrayList = ArrayList()
        val ref= FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list before starting adding data into it
                    pdfArrayList.clear()
                    for (ds in snapshot.children){
                        //get data
                        val model = ds.getValue(ModelPdf::class.java)
                        //add to list
                        pdfArrayList.add(model!!)
                    }
                    //setup adapter
                    adapterPdfUser = AdapterPdfUser(context!!, pdfArrayList)
                    //set adapter to recyclerview
                    binding.booksRv.adapter = adapterPdfUser
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
 }