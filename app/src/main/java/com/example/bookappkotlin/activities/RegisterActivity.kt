package com.example.bookappkotlin.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.bookappkotlin.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    //view binding
    private lateinit var  binding: ActivityRegisterBinding

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    //progres dialog
    private lateinit var  progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //init progress dialog, wwill show while creating account \ register user
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside (false)

        //handle back button click
        binding.backBtn.setOnClickListener{
           onBackPressed() //goto previous screen
        }

        //handle click , begin register
        binding.registerBtn.setOnClickListener {
////             steps
//            1.input data
//            2. valiodate date
//            3. createe account - firebase auth
//            4 save user info 0 firebase realtime database
            validateDate()
        }

    }

    private var name = ""
    private var email = ""
    private var password = ""

    private fun validateDate(){
        //1 input data
        name = binding.nameEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        val cPassword = binding.cPasswordEt.text.toString().trim()

        //2 validate data
        if (name.isEmpty()){
            Toast.makeText(this, "Enter your Name...", Toast.LENGTH_SHORT).show()
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //invalid email pattern
            Toast.makeText(this, "Invalid Email Pattern...", Toast.LENGTH_SHORT).show()
        }
        else if  (password.isEmpty()){
            Toast.makeText(this, "Enter Password..." ,Toast.LENGTH_SHORT).show()
        }
        else if (cPassword.isEmpty()){
            Toast.makeText(this, "Confirm Password..." ,Toast.LENGTH_SHORT).show()
        } else if (cPassword!= password){
            Toast.makeText(this, "Password doesn't match..." ,Toast.LENGTH_SHORT).show()
        }else {
            createUserAccount()
        }
    }
    private fun createUserAccount(){
        //3 create acc
        //show progress
        progressDialog.setMessage("Creating Account...")
        progressDialog.show()

        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //acc created, now add user info in db
                updateUserInfo()
            }
            .addOnFailureListener{ e->
                //faild creatng acc
                Toast.makeText(this,"Faild creating account due to ${e.message}", Toast.LENGTH_SHORT).show()

            }
    }

    private fun updateUserInfo(){
     //   4 save user info 0 firebase realtime database
        progressDialog.setMessage("Saving user info..")

        //timestamp
        val timestamp = System.currentTimeMillis()

        //get current user uid, since user is registered so we can get it now
        val uid = firebaseAuth.uid

        //setup data to add in db
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["uid"]=uid
        hashMap["email"]=email
        hashMap["name"]= name
        hashMap["profileImage"]= ""//add empty , will doi in profile edit
        hashMap["userType"]="user" //possible values are user/admin, will change value to admin manually on firebase db
        hashMap["timestamp"]=timestamp

        //set data to db
        val ref= FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                //user info saved , open user dashboard
                progressDialog.dismiss()

                Toast.makeText(this,"Account created...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, DashboardUserActivity::class.java))
                finish()
            }
            .addOnFailureListener{ e->
                progressDialog.dismiss() //dong hop thoai

                //faild creatng acc
                Toast.makeText(this,"Faild saving info due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}