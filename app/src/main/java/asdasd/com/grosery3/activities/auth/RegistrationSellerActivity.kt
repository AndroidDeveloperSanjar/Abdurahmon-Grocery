package asdasd.com.grosery3.activities.auth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import asdasd.com.grosery3.R
import asdasd.com.grosery3.activities.auth.livedata.LocationViewModel
import asdasd.com.grosery3.activities.auth.utils.LocationUtil
import asdasd.com.grosery3.toast
import com.androidisland.ezpermission.EzPermission
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_registration_seller.*
import kotlinx.android.synthetic.main.activity_registration_user.*
import java.util.*

class RegistrationSellerActivity : AppCompatActivity() {

    private lateinit var locationViewModel: LocationViewModel
    private var isGPSEnabled = false

    private val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var lat = 0.0
    private var lon = 0.0

    private var imageUri: Uri? = null

    private var address: String? = null
    private var city: String? = null
    private var country: String? = null

    private var cameraPermissions = arrayOf<String>()
    private var storagePermissions = arrayOf<String>()

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_seller)

        init()

        onClick()

    }

    private fun init() {

        // Instance of LocationViewModel
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)


        //Check weather Location/GPS is ON or OFF
        LocationUtil(this).turnGPSOn(object :
                LocationUtil.OnLocationOnListener {

            override fun locationStatus(isLocationOn: Boolean) {
                this@RegistrationSellerActivity.isGPSEnabled = isLocationOn
            }
        })

        firebaseAuth = FirebaseAuth.getInstance()

        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun onClick() {
        btn_sellerR.setOnClickListener {
            when {
                et_full_name_sellerR.text.isEmpty() -> toast("Enter full name!")
                et_shop_name_sellerR.text.isEmpty() -> toast("Enter shop name!")
                et_phone_number_sellerR.length() != 23 -> toast("Enter phone number!")
                et_email_sellerR.text.isEmpty() -> toast("Enter email address!")
                et_delivery_fee_sellerR.text.isEmpty() -> toast("Enter delivery fee!")
                et_password_sellerR.text.isEmpty() -> toast("Enter password!")
                et_password_sellerR.length() < 6 -> toast("Must password length > 6 ")
                et_password_confirm_sellerR.length() < 6 -> toast("Must confirm password length > 6")
                et_password_confirm_sellerR.text.isEmpty() -> toast("Enter confirm password!")
                et_password_sellerR.text.toString() != et_password_confirm_sellerR.text.toString() -> toast("Password != Confirm password")
                else -> {
                    //createAccount()
                    val intent = Intent(this@RegistrationSellerActivity, VerifySellerActivity::class.java)
                    intent.apply {
                        putExtra("full_name", et_full_name_sellerR.text.toString())
                        putExtra("shop_name", et_shop_name_sellerR.text.toString())
                        putExtra("phone_number", et_phone_number_sellerR.rawText)
                        putExtra("delivery_fee", et_delivery_fee_sellerR.text.toString())
                        putExtra("lat", lat)
                        putExtra("lon", lon)
                        putExtra("country", country)
                        putExtra("city", city)
                        putExtra("address", address)
                        putExtra("email_address", et_email_sellerR.text.toString())
                        putExtra("password", et_password_sellerR.text.toString())
                        putExtra("confirm_password", et_password_confirm_sellerR.text.toString())
                        putExtra("image_uri", imageUri)
                        startActivity(this)
                    }
                }
            }
        }

        iv_back_seller.setOnClickListener {
            onBackPressed()
        }

        iv_profile_sellerR.setOnClickListener {
            showImagePickDialog()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeLocationUpdates() {
        locationViewModel.getLocationData.observe(this, androidx.lifecycle.Observer {
            lat = it.latitude
            lon = it.longitude
            findAddress(it.latitude, it.longitude)
        })
    }

    private fun findAddress(lat: Double, lon: Double) {
        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geoCoder.getFromLocation(lat, lon, 1)
            address = addresses[0].getAddressLine(0)
            city = addresses[0].locality
            country = addresses[0].countryName

        } catch (e: Exception) {
            toast(e.message!!)
        }
    }


    /**
     * onStart lifecycle of activity
     */
    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }


    /**
     * Initiate Location updated by checking Location/GPS settings is ON or OFF
     * Requesting permissions to read location.
     */
    private fun startLocationUpdates() {
        when {
            isLocationPermissionsGranted() -> {
                observeLocationUpdates()
            }
            else -> {
                askLocationPermission()
            }
        }
    }

    /**
     * Check the availability of location permissions
     */
    private fun isLocationPermissionsGranted(): Boolean {
        return (EzPermission.isGranted(this, locationPermissions[0])
                && EzPermission.isGranted(this, locationPermissions[1]))
    }

    /**
     *
     */
    private fun askLocationPermission() {
        EzPermission
                .with(this)
                .permissions(locationPermissions[0], locationPermissions[1])
                .request { granted, denied, permanentlyDenied ->
                    if (granted.contains(locationPermissions[0]) &&
                            granted.contains(locationPermissions[1])
                    ) { // Granted
                        startLocationUpdates()

                    } else if (denied.contains(locationPermissions[0]) ||
                            denied.contains(locationPermissions[1])
                    ) { // Denied

                        showDeniedDialog()

                    } else if (permanentlyDenied.contains(locationPermissions[0]) ||
                            permanentlyDenied.contains(locationPermissions[1])
                    ) { // Permanently denied
                        showPermanentlyDeniedDialog()
                    }

                }
    }

    /**
     *
     */
    private fun showPermanentlyDeniedDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(getString(R.string.title_permission_permanently_denied))
        dialog.setMessage(getString(R.string.message_permission_permanently_denied))
        dialog.setNegativeButton(getString(R.string.not_now)) { _, _ -> }
        dialog.setPositiveButton(getString(R.string.settings)) { _, _ ->
            startActivity(
                    EzPermission.appDetailSettingsIntent(
                            this
                    )
            )
        }
        dialog.setOnCancelListener { } //important
        dialog.show()
    }


    /**
     *
     */
    private fun showDeniedDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(getString(R.string.title_permission_denied))
        dialog.setMessage(getString(R.string.message_permission_denied))
        dialog.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
        dialog.setPositiveButton(getString(R.string.allow)) { _, _ ->
            askLocationPermission()
        }
        dialog.setOnCancelListener { } //important
        dialog.show()
    }

    /**
     * On Activity Result for locations permissions updates
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == LOCATION_PERMISSION_REQUEST) {
                isGPSEnabled = true
                startLocationUpdates()
            }

            imageUri = data?.data
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                if (imageUri != null)
                    iv_profile_sellerR.setImageURI(imageUri)
                else
                    toast("Image Uri == null")
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                if (imageUri != null)
                    iv_profile_sellerR.setImageURI(imageUri)
                else
                    toast("Image Uri == null")
            }
        }
    }

    private fun showImagePickDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Image")
                .setItems(options) { dialog, which ->
                    if (which == 0) {
                        if (checkCameraPermission())
                            pickFromCamera()
                        else
                            requestCameraPermission()

                    } else
                        if (checkStoragePermission())
                            pickFromGallery()
                        else
                            requestStoragePermission()

                }.show()
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        return result && result1
    }

    private fun pickFromCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image Title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image Description")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted)
                        pickFromCamera()
                    else
                        toast("Camera permissions are necessary...")
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted)
                        pickFromGallery()
                    else
                        toast("Storage permissions is necessary...")
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val CAMERA_REQUEST_CODE = 2000
        const val STORAGE_REQUEST_CODE = 3000
        const val IMAGE_PICK_GALLERY_CODE = 4000
        const val IMAGE_PICK_CAMERA_CODE = 5000
    }
}