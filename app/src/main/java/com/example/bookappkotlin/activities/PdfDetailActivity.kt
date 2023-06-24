package com.example.bookappkotlin.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bookappkotlin.Constants
import com.example.bookappkotlin.MyApplication
import com.example.bookappkotlin.R
import com.example.bookappkotlin.adapters.AdapterComment
import com.example.bookappkotlin.databinding.ActivityPdfDetailBinding
import com.example.bookappkotlin.databinding.DialogCommentAddBinding
import com.example.bookappkotlin.models.ModelComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream

class PdfDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfDetailBinding

    private companion object{
        const val TAG="BOOK_DETAILS_TAG"
    }
    //book id
    private var bookId =""
    //get from firebase
    private var bookTitle =""
    private var bookUrl=""
// will hold a boolean val
    private var isInMyfavorite = false
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    //arraylist to hold comments
    private lateinit var commentArrayList: ArrayList<ModelComment>
    // adapter to be set to recyclerview
    private lateinit var adapterComment: AdapterComment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get book id from intent , will load book info using this bookid
        bookId = intent.getStringExtra("bookId")!!

        // init progress bar
         progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser !=null){
            //user is logged in . check if book is in fav or not
            checkIsFavorite()
        }


        //increment book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId)

        loadBookDetails()
        showComments()

        //handle back button click, goback
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click , pdf view activity
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }

        //handle click download book pdf
        binding.downloadBookBtn.setOnClickListener {
            //let's check WRITE_EXTERNAL_STORAGE permission first, if granted download book, if not granted requPest permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onCreate: STORAGE PERMISSION is already granted")
                downloadBook()
            }
            else{
                Log.d(TAG, "onCreate: STORAGE PERMISSION is not granted, LEST request it")
                requestStoragePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding.favoriteBtn.setOnClickListener {
            //we an add only if user is logged in
            //1 check if user is logged in or not
            if (firebaseAuth.currentUser ==null){
                //user not logged in , cant do favorite functionality
                Toast.makeText(this, "You're not Logged in", Toast.LENGTH_SHORT).show()
            }
            else{
                //user is logged in ,we can do favorite functionlity
                if (isInMyfavorite){
                    //already in fav, remove
                    MyApplication.removeFromFavorite(this, bookId)
                }else{
                    //not in fav.add
                    addToFavorite()
                }
            }
        }

        //handle click, show comment dialog
        binding.addCommentBtn.setOnClickListener {
            /*to add  a comment , user must be looged in , if not just show a message */
            if (firebaseAuth.currentUser==null){
                Toast.makeText(this, "You're not Logged In", Toast.LENGTH_SHORT).show()
            }
            else {
                addCommentDialog()
            }
        }

    }

    private fun showComments() {
        //init arraylist
        commentArrayList = ArrayList()

        //db path to load comments
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list
                    commentArrayList.clear()
                    for (ds in snapshot.children){
                        //get data  s model , be careFull of spellings and data type
                        val model = ds.getValue(ModelComment::class.java)
                        //add to list
                        commentArrayList.add(model!!)
                    }
                    //setup adapter
                    adapterComment= AdapterComment(this@PdfDetailActivity, commentArrayList)
                    //set adapter to recyclerview
                    binding.commentsRv.adapter = adapterComment
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

    }

    private var comment = ""
    private fun addCommentDialog() {
//        inflate/ bind view for dialog dialog_comment_add.xml
        val commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this))

        //setup alert dialog
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(commentAddBinding.root)

        //create and show alert dialog
        val alertDialog = builder.create()
        alertDialog.show()

        //handle click ,dismiss dialog
        commentAddBinding.backBtn.setOnClickListener { alertDialog.dismiss() }
        //handle add comment
        commentAddBinding.submitBtn.setOnClickListener {
            //get data
            comment = commentAddBinding.commentEt.text.toString().trim()
            //validate data
            if (comment.isEmpty()){
                Toast.makeText(this, "Enter comment...", Toast.LENGTH_SHORT).show()
            }
            else
            {
                alertDialog.dismiss()
                addComment()
            }
        }
    }

    private fun addComment() {
//        show progress
        progressDialog.setMessage("Adding Comment")
        progressDialog.show()

        //timestamp for comment id, comment timestamp etc
        val timestamp = "${System.currentTimeMillis()}"
        //setup data to add in db for comment
        val hashMap =HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["bookId"]= "$bookId"
        hashMap["timestamp"]= "$timestamp"
        hashMap["comment"]= "$comment"
        hashMap["uid"]= "${firebaseAuth.uid}"

        //db path to add data into it
        //book >bookId> Comments>CommentID> commentData
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Comment add....", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this, "Faided to add Comment due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean->
        //lets check if granted or not
        if (isGranted){
            Log.d(TAG, "onCreate: STORAGE PERMISSION is  granted")
            downloadBook()
        }
        else{
            Log.d(TAG, "onCreate: STORAGE PERMISSION is  denied")
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadBook(){
        Log.d(TAG, "downloadBook: Downloading Book")
        progressDialog.setMessage("Downloading Book..")
        progressDialog.show()

        //let download book from firebase storage using url
        val storageReference= FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "downloadBook: Book downloaded...")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener {  e->
                progressDialog.dismiss()
                Log.d(TAG, "downloadBook: failed to download book due to ${e.message}")
                Toast.makeText(this, "failed to download book due to ${e.message}", Toast.LENGTH_SHORT).show()

            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray?) {
        Log.d(TAG,"saveToDownloadsFolder: saving downloaded book")

        val nameWithExtention ="${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs() //create folder if not exists

            val filePath = downloadsFolder.path +"/"+ nameWithExtention

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this, "SAve to downloads Folder", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "saveToDownloadsFolder: Saved to Downloads Folder")
            progressDialog.dismiss()
            incrementDownloadCount()
        }
        catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadsFolder: failed to save due ${e.message}")
            Toast.makeText(this, "1111failed to save due ${e.message}", Toast.LENGTH_SHORT).show()

        }
    }

    private fun incrementDownloadCount() {
        //increment downloads count to firebases db
        Log.d(TAG, "incrementDownloadCount: ")

        //step 1 get previous downloads count
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get download count
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDataChange: Current Downloads Count: $downloadsCount")

                    if (downloadsCount==""||downloadsCount=="null")
                    {
                        downloadsCount="0"
                    }
                    //convert to long and increment 1
                    val newDownloadCount:Long = downloadsCount.toLong()+1
                    Log.d(TAG, "onDataChange: New Downloads Count: $newDownloadCount")

                    //setup data to update to db
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadCount

                    //step 2 update new incremented downloads count to db
                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Downloads count incremented")
                        }
                        .addOnFailureListener {e->
                            Log.d(TAG, "onDataChange: FAILED to increment due to ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun loadBookDetails() {
        //book -> bookId->Details


        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //geet data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())
                    //load pdf category
                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    //load pdf thumbnail , pages count
                    MyApplication.loadPdfFromUrlSinglePage(
                        "$bookUrl",
                        "$bookTitle",
                        binding.pdfView,
                        binding.progressBar,
                        binding.pagesTv
                    )
                    //load pdf size
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    //set data
                    binding.titleTv.text=bookTitle
                    binding.descriptionTv.text=description
                    binding.viewsTv.text=viewsCount
                    binding.dowloadsTv.text=downloadsCount
                    binding.dateTv.text=date
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: Checking ìs book is in fav or not")
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyfavorite = snapshot.exists()
                    if (isInMyfavorite){
                        //available in favorite
                        Log.d(TAG, "onDataChange: available in favorite")
                        //sert drawable top icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_filled_white,0,0)
                        binding.favoriteBtn.text= "Remove Favorite"
                    }
                    else{
                        //not available in favorite
                        Log.d(TAG, "onDataChange: not available in favorite")
                        //set drawable top icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_border_white,0,0)
                        binding.favoriteBtn.text="Add Favorite"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun addToFavorite(){
        Log.d(TAG, "addToFavorite: Adding to fav")
        val timestamp = System.currentTimeMillis()

        //setup data to add in db
        val hashMap = HashMap<String, Any>()
        hashMap["bookId"]= bookId
        hashMap["timestamp"]= timestamp

        //save to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                //added to fav
                Log.d(TAG, "addToFavorite : Added to fav")
                Toast.makeText(this, "addToFavorite : Added to Favorite", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                //failed to add to fav
                Log.d(TAG, "addToFavorite : failed to add to fav due to ${e.message}")
                Toast.makeText(this, "Failed to add to fave due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}