package asdasd.com.grosery3.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import asdasd.com.grosery3.R
import asdasd.com.grosery3.activities.auth.LoginActivityy
import asdasd.com.grosery3.activities.auth.model.UserInformation
import asdasd.com.grosery3.adabters.AdapterOrderUser
import asdasd.com.grosery3.adabters.AdapterShop
import asdasd.com.grosery3.logging
import asdasd.com.grosery3.models.ModelOrderUser
import asdasd.com.grosery3.models.ModelShop
import asdasd.com.grosery3.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main_user.*
import java.util.*

class MainUserActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var shopsList: ArrayList<ModelShop>? = null
    private var adapterShop: AdapterShop? = null
    private var ordersList: ArrayList<ModelOrderUser>? = null
    private var adapterOrderUser: AdapterOrderUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_user)

        init()

        checkUser()

        showShopsUI()

        onClick()
    }

    private fun init() {
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        firebaseAuth = FirebaseAuth.getInstance()
        logging("user_id = ${firebaseAuth.uid}")
        val editor: SharedPreferences.Editor = getSharedPreferences("PREFS_NAME", MODE_PRIVATE).edit()
        if (firebaseAuth.uid != null){
            editor.putString("user_id",firebaseAuth.uid)
            editor.apply()
        }
        else
            logging("user_id == null")

    }

    private fun onClick() {
        logoutBtn.setOnClickListener {
            makeMeOffline()
        }
        editProfileBtn.setOnClickListener {
            startActivity(Intent(this@MainUserActivity, ProfileEditUserActivity::class.java))
        }
        tabShopsTv.setOnClickListener {
            showShopsUI()
        }
        tabOrdersTv.setOnClickListener {
            showOrdersUI()
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(this@MainUserActivity, SettingsActivity::class.java))
        }
    }

    private fun showShopsUI() {
        shopsRl.visibility = View.VISIBLE
        ordersRl.visibility = View.GONE
        tabShopsTv.setTextColor(resources.getColor(R.color.colorBlack))
        tabShopsTv.setBackgroundResource(R.drawable.shape_rect04)
        tabOrdersTv.setTextColor(resources.getColor(R.color.colorWhite))
        tabOrdersTv.setBackgroundColor(resources.getColor(android.R.color.transparent))
    }

    private fun showOrdersUI() {
        shopsRl.visibility = View.GONE
        ordersRl.visibility = View.VISIBLE
        tabShopsTv.setTextColor(resources.getColor(R.color.colorWhite))
        tabShopsTv.setBackgroundColor(resources.getColor(android.R.color.transparent))
        tabOrdersTv.setTextColor(resources.getColor(R.color.colorBlack))
        tabOrdersTv.setBackgroundResource(R.drawable.shape_rect04)
    }

    private fun makeMeOffline() {
        progressDialog.setMessage("Logging Out...")
        val hashMap = HashMap<String, Any>()
        hashMap["online"] = "false"

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).updateChildren(hashMap)
                .addOnSuccessListener {
                    firebaseAuth.signOut()
                    checkUser()
                }
                .addOnFailureListener { e -> //failed updating
                    progressDialog.dismiss()
                    toast(e.message!!)
                }
    }

    private fun checkUser() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            val intent = Intent(this@MainUserActivity, LoginActivityy::class.java)
            startActivity(intent)
            finish()
        } else {
            loadMyInfo()
        }
    }

    private fun loadMyInfo() {
        val user = firebaseAuth.currentUser
        val userId = user?.uid
        val ref = FirebaseDatabase.getInstance().reference.child("Users")
        if (userId != null) {
            logging(userId)
            ref.child(firebaseAuth.uid!!).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    logging("onDataChange")
                    val userInformation = p0.getValue(UserInformation::class.java)
                    nameTv.text = userInformation?.name
                    emailTv.text = userInformation?.email
                    phoneTv.text = userInformation?.phone
                    if (userInformation?.profileImage.equals("Did't enter image!")) {
                        logging(userInformation?.profileImage!!)
                    } else {
                        try {
                            Picasso.get().load(userInformation?.profileImage).placeholder(R.drawable.ic_person).into(profileIv)
                        } catch (e: Exception) {
                            profileIv.setImageResource(R.drawable.ic_person)
                        }
                    }
                    if (userInformation?.city != null)
                        loadShops(userInformation.city)
                    else
                        logging("city == null")
                    loadOrders()
                }

                override fun onCancelled(p0: DatabaseError) {
                    logging("cancelled")
                }

            })
        } else
            logging("user == null")
    }

    private fun loadOrders() {
        ordersList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                ordersList?.clear()
                for (ds in dataSnapshot.children) {
                    val uid = "" + ds.ref.key
                    val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Orders")
                    ref.orderByChild("orderBy").equalTo(firebaseAuth!!.uid)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        for (ds in dataSnapshot.children) {
                                            val modelOrderUser = ds.getValue(ModelOrderUser::class.java)!!

                                            //add to list
                                            ordersList!!.add(modelOrderUser)
                                        }
                                        //setup adapter
                                        adapterOrderUser = AdapterOrderUser(this@MainUserActivity, ordersList)
                                        //set to recyclerview
                                        ordersRv!!.adapter = adapterOrderUser
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun loadShops(myCity: String) {
        //init list
        shopsList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.orderByChild("accountType").equalTo("Seller")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //clear list before adding
                        shopsList!!.clear()
                        for (ds in dataSnapshot.children) {
                            val modelShop = ds.getValue(ModelShop::class.java)!!
                            val shopCity = "" + ds.child("city").value

                            //show only user city shops
                            if (shopCity == myCity) {
                                shopsList!!.add(modelShop)
                            }

                            //if you want to display all shops, skip the if statement and add this
                            //shopsList.add(modelShop);
                        }
                        //setup adapter
                        adapterShop = AdapterShop(this@MainUserActivity, shopsList)
                        //set adapter to recyclerview
                        shopsRv!!.adapter = adapterShop
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }
}