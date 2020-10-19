@file:Suppress("DEPRECATION")

package asdasd.com.grosery3.activities.auth

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import asdasd.com.grosery3.R
import asdasd.com.grosery3.activities.ForgotPasswordActivity
import asdasd.com.grosery3.activities.MainSellerActivity
import asdasd.com.grosery3.activities.MainUserActivity
import asdasd.com.grosery3.logging
import asdasd.com.grosery3.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activityy_login.*
import java.util.*

class LoginActivityy : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var userId: String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityy_login)

        val sharedPreferences = getSharedPreferences("PREFS_NAME", MODE_PRIVATE) as SharedPreferences
        userId = sharedPreferences.getString("user_id", "")

        init()

        onClick()
    }

    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun onClick() {
        tv_to_registration_activity_login.setOnClickListener {
            startActivity(Intent(this@LoginActivityy, RegistrationUserActivity::class.java))
        }

        tv_forgot_password_login.setOnClickListener {
            startActivity(Intent(this@LoginActivityy, ForgotPasswordActivity::class.java))
        }

        btn_login.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = et_email_login.text.toString().trim()
        val password = et_password_login.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Invalid email pattern...")
            return
        }

        if (password.isEmpty()) {
            toast("Enter password...")
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { //logged in successfully
                    makeMeOnline()
                }
                .addOnFailureListener { e -> //failed logging in
                    progressDialog.dismiss()
                    toast( e.message!!)
                }
    }

    private fun makeMeOnline() {
        progressDialog.setMessage("Chicking User...")
        val hashMap = HashMap<String, Any>()
        hashMap["online"] = "true"

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).updateChildren(hashMap)
                .addOnSuccessListener {
                    checkUserType()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    toast(e.message!!)
                }
    }

    private fun checkUserType() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        logging("${ref.orderByChild("uid")}")
        ref.orderByChild("uid").equalTo(firebaseAuth.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        logging("${firebaseAuth.uid}")
                        for (ds in dataSnapshot.children) {
                            val accountType = "" + ds.child("accountType").value
                            if (accountType == "Seller") {
                                progressDialog.dismiss()
                                startActivity(Intent(this@LoginActivityy, MainSellerActivity::class.java))
                                finish()
                            } else {
                                progressDialog.dismiss()
                                startActivity(Intent(this@LoginActivityy, MainUserActivity::class.java))
                                finish()
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
    }
}