package asdasd.com.grosery3.activities.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import asdasd.com.grosery3.R
import asdasd.com.grosery3.activities.MainUserActivity
import asdasd.com.grosery3.logging
import asdasd.com.grosery3.toast
import com.google.android.gms.tasks.TaskExecutors
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_verify_user.*
import java.util.HashMap
import java.util.concurrent.TimeUnit


class VerifyUserActivity : AppCompatActivity() {

    private lateinit var userPhoneNumber: String
    private var verificationId: String? = null
    private var smsCode: String? = null

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var userFullName: String
    private var lat = 0.0
    private var lon = 0.0
    private lateinit var country: String
    private lateinit var city: String
    private lateinit var address: String
    private lateinit var emailAddress: String
    private lateinit var password: String
    private lateinit var confirmPassword: String
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_user)

        init()
    }

    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        intent.apply {
            userFullName = getStringExtra("full_name")!!
            userPhoneNumber = getStringExtra("phone_number")!!
            lat = getDoubleExtra("lat", 0.0)
            lon = getDoubleExtra("lon", 0.0)
            country = getStringExtra("country")!!
            city = getStringExtra("city")!!
            address = getStringExtra("address")!!
            emailAddress = getStringExtra("email_address")!!
            password = getStringExtra("password")!!
            confirmPassword = getStringExtra("confirm_password")!!
            imageUri = getParcelableExtra("image_uri")
            logging(userPhoneNumber)
        }

        sendVerificationCode("+998$userPhoneNumber")

        onClick()
    }

    private fun onClick() {
        btn_verification.setOnClickListener {
            val code: String = et_code_user.text.toString().trim()

            if (code.isEmpty() || code.length < 6) {
                et_code_user.error = "Enter code..."
                et_code_user.requestFocus()
                return@setOnClickListener
            }
            verifyCode(code)
        }
    }

    private fun verifyCode(code: String) {
        if (verificationId != null) {
            if (et_code_user.text.isNotEmpty()) {
                if (smsCode != null) {
                    if (code == smsCode) {
                        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                        signInWithCredential(credential)
                    } else
                        toast("Wrong verification id!")
                } else
                    toast("Sms code == null")
            } else
                toast("Enter the code!")
        } else
            toast("Verification id null!")
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logging("task successfully")
                        //saverFirebaseData()
                        createAccount()
                        progress_bar_verify_user.visibility = View.VISIBLE
                        Handler().postDelayed(runnable, 5000)
                    } else {
                        logging(task.exception!!.message!!)
                    }
                }
    }

    private val runnable = Runnable {
        val intent = Intent(this@VerifyUserActivity, MainUserActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        progress_bar_verify_user.visibility = View.GONE
    }

    private fun sendVerificationCode(number: String) {
        progress_bar_verify_user.visibility = View.VISIBLE
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                number,
                60,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallBack
        )
    }

    private val mCallBack: OnVerificationStateChangedCallbacks = object : OnVerificationStateChangedCallbacks() {
        override fun onCodeSent(s: String, forceResendingToken: ForceResendingToken) {
            super.onCodeSent(s, forceResendingToken)
            verificationId = s
        }

        override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
            smsCode = phoneAuthCredential.smsCode
            if (smsCode != null) {
                et_code_user.setText(smsCode)
                verifyCode(smsCode!!)
                logging("$smsCode")
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            logging(e.message!!)
        }
    }

    private fun createAccount() {
        firebaseAuth.createUserWithEmailAndPassword(emailAddress, password)
                .addOnSuccessListener {
                    saverFirebaseData()
                }
                .addOnFailureListener { e ->
                    logging(e.message!!)
                }
    }

    private fun saverFirebaseData() {
        val timestamp = System.currentTimeMillis()
        if (imageUri == null) {
            val hashMap = HashMap<String, Any>()
            hashMap["uid"] = "${firebaseAuth.uid}"
            hashMap["email"] = emailAddress
            hashMap["name"] = userFullName
            hashMap["phone"] = "+998${userPhoneNumber}"
            hashMap["country"] = country
            hashMap["city"] = city
            hashMap["address"] = address
            hashMap["latitude"] = "$lat"
            hashMap["longitude"] = "$lon"
            hashMap["timestamp"] = "$timestamp"
            hashMap["accountType"] = "User"
            hashMap["online"] = "true"
            hashMap["profileImage"] = "Did't enter image!"

            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).setValue(hashMap)
                    .addOnSuccessListener {
                        logging("Wrote to db all informations.")
                    }
                    .addOnFailureListener {
                        logging("Don't write to db all informations.")
                    }
        } else {
            val filePathAndName = "profile_images/" + "" + firebaseAuth.uid
            val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageReference.putFile(imageUri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        val uriTask = taskSnapshot.storage.downloadUrl
                        while (!uriTask.isSuccessful);
                        val downloadImageUrl = uriTask.result
                        if (uriTask.isSuccessful) {
                            val hashMap = HashMap<String, Any>()
                            hashMap["uid"] = "" + firebaseAuth.uid
                            hashMap["email"] = emailAddress
                            hashMap["name"] = userFullName
                            hashMap["phone"] = "+998${userPhoneNumber}"
                            hashMap["country"] = country
                            hashMap["city"] = city
                            hashMap["address"] = address
                            hashMap["latitude"] = "$lat"
                            hashMap["longitude"] = "$lon"
                            hashMap["timestamp"] = "$timestamp"
                            hashMap["accountType"] = "User"
                            hashMap["online"] = "true"
                            hashMap["profileImage"] = "" + downloadImageUrl

                            val ref = FirebaseDatabase.getInstance().getReference("Users")
                            ref.child(firebaseAuth.uid!!).setValue(hashMap)
                                    .addOnSuccessListener {
                                        logging("Wrote to db all informations.")
                                    }
                                    .addOnFailureListener {
                                        logging("Don't write to db all informations.")
                                    }
                        }
                    }
                    .addOnFailureListener { e ->
                        logging(e.message!!)
                    }
        }
    }
}