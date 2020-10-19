package asdasd.com.grosery3.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import asdasd.com.grosery3.Constants
import asdasd.com.grosery3.R
import asdasd.com.grosery3.db.ImageUri
import asdasd.com.grosery3.db.ImageUriDB
import asdasd.com.grosery3.logging
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class AddProductActivity : AppCompatActivity() {
    //ui views
    private lateinit var backBtn: ImageButton
    private lateinit var productIconIv: ImageView
    private lateinit var titleEt: EditText
    private lateinit var descriptionEt: EditText
    private lateinit var categoryTv: TextView
    private lateinit var quantityEt: TextView
    private lateinit var priceEt: TextView
    private lateinit var discountedPriceEt: TextView
    private lateinit var discountedNoteEt: TextView
    private lateinit var discountSwitch: SwitchCompat
    private lateinit var addProductBtn: Button

    private lateinit var job: Job

    //permission arrays
    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>

    //image picked uri
    private var image_uri: Uri? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        logging(productId)

        job = Job()

        //init ui views
        backBtn = findViewById(R.id.backBtn)
        productIconIv = findViewById(R.id.productIconIv)
        titleEt = findViewById(R.id.titleEt)
        descriptionEt = findViewById(R.id.descriptionEt)
        categoryTv = findViewById(R.id.categoryTv)
        quantityEt = findViewById(R.id.quantityEt)
        priceEt = findViewById(R.id.priceEt)
        discountSwitch = findViewById(R.id.discounSwitch)
        discountedPriceEt = findViewById(R.id.discountedPriceEt)
        discountedNoteEt = findViewById(R.id.discountedNoteEt)
        addProductBtn = findViewById(R.id.addProductBtn)

        //on start is unchecked, so hide discountPriceEt, discountNoteEt
        discountedPriceEt.visibility = View.GONE
        discountedNoteEt.visibility = View.GONE
        firebaseAuth = FirebaseAuth.getInstance()

        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please wait")
        progressDialog!!.setCanceledOnTouchOutside(false)

        //init permission arrays
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //if discountSwitch is checked: show discountPriceEt, discountNoteEt | if diskountSwitch is not checked: hide discountPriceEt, discountNoteEt
        discountSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                //checked, show discountPriceEt, discountNoteEt
                discountedPriceEt.visibility = View.VISIBLE
                discountedNoteEt.visibility = View.VISIBLE
            } else {
                //unchecked hide discountPriceEt, discountNoteEt
                discountedPriceEt.visibility = View.GONE
                discountedNoteEt.visibility = View.GONE
            }
        })
        backBtn.setOnClickListener(View.OnClickListener { onBackPressed() })
        productIconIv.setOnClickListener(View.OnClickListener { //show dialog to pick image
            showImagePickDialog()
        })
        categoryTv.setOnClickListener(View.OnClickListener { //pick category
            categoryDialog()
        })
        addProductBtn.setOnClickListener(View.OnClickListener { //Flow:
            //1) Input data
            //2) Validate data
            //3) Add data to db
            inputData()
        })
    }

    private var productTitle: String? = null
    private var productDescription: String? = null
    private var productCategory: String? = null
    private var productQuantity: String? = null
    private var originalPrice: String? = null
    private var discountPrice: String? = null
    private var discountNote: String? = null
    private var discountAvailable = false
    private fun inputData() {
        //1) Input data
        productTitle = titleEt.text.toString().trim { it <= ' ' }
        productDescription = descriptionEt.text.toString().trim { it <= ' ' }
        productCategory = categoryTv.text.toString().trim { it <= ' ' }
        productQuantity = quantityEt.text.toString().trim { it <= ' ' }
        originalPrice = priceEt.text.toString().trim { it <= ' ' }
        discountAvailable = discountSwitch.isChecked //true//false

        //2) Validate data
        if (TextUtils.isEmpty(productTitle)) {
            Toast.makeText(this, "Title is required...", Toast.LENGTH_SHORT).show()
            return  // don't proceed further
        }
        if (TextUtils.isEmpty(productCategory)) {
            Toast.makeText(this, "Category is required...", Toast.LENGTH_SHORT).show()
            return  // don't proceed further
        }
        if (TextUtils.isEmpty(originalPrice)) {
            Toast.makeText(this, "Price is required...", Toast.LENGTH_SHORT).show()
            return  // don't proceed further
        }
        if (discountAvailable) {
            //product is with discount
            discountPrice = discountedPriceEt.text.toString().trim { it <= ' ' }
            discountNote = discountedNoteEt.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(discountPrice)) {
                Toast.makeText(this, "Discount Price is required...", Toast.LENGTH_SHORT).show()
                return  // don't proceed further
            }
        } else {
            //product is without discount
            discountPrice = "0"
            discountNote = ""
        }
        addProduct()
        addImageUriToDb()
    }

    private lateinit var productId: String

    private fun addProduct() {
        //3) Add data to db
        progressDialog!!.setMessage("Adding Product...")
        progressDialog!!.show()
        productId = "" + System.currentTimeMillis()
        if (image_uri == null) {
            //upload without image

            //setup data to upload
            val hashMap = HashMap<String, Any>()
            hashMap["productId"] = "" + productId
            hashMap["productTitle"] = "" + productTitle
            hashMap["productDescription"] = "" + productDescription
            hashMap["productCategory"] = "" + productCategory
            hashMap["productQuantity"] = "" + productQuantity
            hashMap["productIcon"] = "" // no image, set empty
            hashMap["originalPrice"] = "" + originalPrice
            hashMap["discountPrice"] = "" + discountPrice
            hashMap["discountNote"] = "" + discountNote
            hashMap["discountAvailable"] = "" + discountAvailable
            hashMap["timestamp"] = "" + productId
            hashMap["uid"] = "" + firebaseAuth!!.uid
            //add to db
            val reference = FirebaseDatabase.getInstance().getReference("Users")
            reference.child(firebaseAuth!!.uid!!).child("Products").child(productId).setValue(hashMap)
                    .addOnSuccessListener { //added to db
                        progressDialog!!.dismiss()
                        Toast.makeText(this@AddProductActivity, "Product added...", Toast.LENGTH_SHORT).show()
                        clearData()
                    }
                    .addOnFailureListener { e -> //failed adding to db
                        progressDialog!!.dismiss()
                        Toast.makeText(this@AddProductActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
        } else {
            //upload with image

            //first upload image to storage

            //name and path of image to be uploaded
            val filePathAndName = "product_images/$productId"
            val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageReference.putFile(image_uri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        //image uploaded
                        //get url of uploaded image
                        val uriTask = taskSnapshot.storage.downloadUrl
                        while (!uriTask.isSuccessful);
                        val downloadImageUri = uriTask.result
                        if (uriTask.isSuccessful) {
                            //url of image received, upload to db
                            //setup data to upload
                            val hashMap = HashMap<String, Any>()
                            hashMap["productId"] = "" + productId
                            hashMap["productTitle"] = "" + productTitle
                            hashMap["productDescription"] = "" + productDescription
                            hashMap["productCategory"] = "" + productCategory
                            hashMap["productQuantity"] = "" + productQuantity
                            hashMap["productIcon"] = "" + downloadImageUri
                            hashMap["originalPrice"] = "" + originalPrice
                            hashMap["discountPrice"] = "" + discountPrice
                            hashMap["discountNote"] = "" + discountNote
                            hashMap["discountAvailable"] = "" + discountAvailable
                            hashMap["timestamp"] = "" + productId
                            hashMap["uid"] = "" + firebaseAuth!!.uid
                            //add to db
                            val reference = FirebaseDatabase.getInstance().getReference("Users")
                            reference.child(firebaseAuth!!.uid!!).child("Products").child(productId).setValue(hashMap)
                                    .addOnSuccessListener { //added to db
                                        progressDialog!!.dismiss()
                                        Toast.makeText(this@AddProductActivity, "Product added...", Toast.LENGTH_SHORT).show()
                                        clearData()
                                    }
                                    .addOnFailureListener { e -> //failed adding to db
                                        progressDialog!!.dismiss()
                                        Toast.makeText(this@AddProductActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                                    }
                        }
                    }
                    .addOnFailureListener { e -> //failed uploading image
                        progressDialog!!.dismiss()
                        Toast.makeText(this@AddProductActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
        }
    }

    private fun addImageUriToDb() {
        if (image_uri != null) {
            CoroutineScope(Main + job).launch {
                applicationContext?.let {
                    val imageUri = ImageUri(
                            productId,
                            image_uri?.toString()
                    )
                    ImageUriDB(it).getImageUriDao().addImageUri(imageUri)
                }
            }
        }
    }

    private fun clearData() {
        //clear data after uploading product
        titleEt.setText("")
        descriptionEt.setText("")
        categoryTv.text = ""
        quantityEt.text = ""
        priceEt.text = ""
        discountedPriceEt.text = ""
        discountedNoteEt.text = ""
        productIconIv.setImageResource(R.drawable.ic_add_shopping_primary)
        image_uri = null
    }

    private fun categoryDialog() {
        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Product Category")
                .setItems(Constants.productCategories) { dialog, which -> //get picked category
                    val category = Constants.productCategories[which]

                    //set picked category
                    categoryTv.text = category
                }
                .show()
    }

    private fun showImagePickDialog() {
        //options to display in dialog
        val options = arrayOf("Camera", "Gallery")
        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Image")
                .setItems(options) { dialog, which ->
                    //handle item clicks
                    if (which == 0) {
                        //camera clicked
                        if (checkCameraPermission()) {
                            //permission granted
                            pickFromCamera()
                        } else {
                            //permission not granted, request
                            requestCameraPermission()
                        }
                    } else {
                        //gallery clicked
                        if (checkStoragePermission()) {
                            //permission granted
                            pickFromGallery()
                        } else {
                            //permission not granted, request
                            requestStoragePermission()
                        }
                    }
                }
                .show()
    }

    private fun pickFromGallery() {
        //intent to pick image from gellery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickFromCamera() {
        //intent to pick image from camera

        //using media store to pick high/original quality image
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image_Title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image_Description")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED //returns true/false
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        return result && result1
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    //handle permission results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                run {
                    if (grantResults.size > 0) {
                        val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                        val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                        if (cameraAccepted && storageAccepted) {
                            //both permissions granted
                            pickFromCamera()
                        } else {
                            //both or one of permissions denied
                            Toast.makeText(this, "Camera & Storage permissions are required...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                run {
                    if (grantResults.size > 0) {
                        val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                        if (storageAccepted) {
                            //permission granted
                            pickFromGallery()
                        } else {
                            //permission denied
                            Toast.makeText(this, "Storage permission is required...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.size > 0) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted) {
                        pickFromGallery()
                    } else {
                        Toast.makeText(this, "Storage permission is required...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //handle image pick results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image picked from gallery

                //save picked image uri
                image_uri = data!!.data


                productIconIv.setImageURI(image_uri)
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //image picked from camera
                productIconIv.setImageURI(image_uri)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        //permission constants
        private const val CAMERA_REQUEST_CODE = 200
        private const val STORAGE_REQUEST_CODE = 300

        //image pick constants
        private const val IMAGE_PICK_GALLERY_CODE = 400
        private const val IMAGE_PICK_CAMERA_CODE = 500
    }
}