package com.example.bookappkotlin.activities

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.bookappkotlin.databinding.ActivityCategoryAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CategoryAddActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding: ActivityCategoryAddBinding
    //firebbase dialog
    private lateinit var firebaseAuth: FirebaseAuth
    //progress dialog
    private lateinit var progressDialog :ProgressDialog //tai data trang

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth= FirebaseAuth.getInstance()

        //configure progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle back, begin upload category
        binding.submitBtn.setOnClickListener{

            validateData()
        }
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }
    private var category =" "

    private fun validateData(){
        //get data
        category = binding.categoryEt.text.toString().trim()
        //vallidate data
        if (category.isEmpty()){
            Toast.makeText(this,"Enter Category...", Toast.LENGTH_SHORT).show()

        }
        else{
            addCategoryFirebase()
        }
    }

    private fun addCategoryFirebase() {
        //show progress
        progressDialog.show()

        //get timestamp
        val timestamp = System.currentTimeMillis() //lấy thời gian hiện tại của hệ thống 1/1/1990

        //setup data to add in firebase db
        val hashMap = HashMap<String, Any>() //save các keyvalue
        hashMap["id"] = "$timestamp"        //lấy chuỗi cho id
        hashMap["category"] = category
        hashMap["timestamp"] = timestamp
        hashMap["uid"] = "${firebaseAuth.uid}"

        //add to fire base db: Database root > category> catogory id> category info
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"Added successfully..", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this,"Failed to add due to ${e.message}...", Toast.LENGTH_SHORT).show()
            }
    }
}