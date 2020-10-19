package asdasd.com.grosery3.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import asdasd.com.grosery3.Constants
import asdasd.com.grosery3.R
import asdasd.com.grosery3.activities.auth.LoginActivityy
import asdasd.com.grosery3.adabters.AdapterOrderShop
import asdasd.com.grosery3.adabters.AdapterProductSeller
import asdasd.com.grosery3.models.ModelOrderShop
import asdasd.com.grosery3.models.ModelProduct
import asdasd.com.grosery3.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main_seller.*
import java.util.*

class MainSellerActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var productList: ArrayList<ModelProduct>? = null
    private var adapterProductSeller: AdapterProductSeller? = null
    private var orderShopArrayList: ArrayList<ModelOrderShop>? = null
    private var adapterOrderShop: AdapterOrderShop? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_seller)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()
        loadAllProducts()
        loadAllOrders()
        showProductsUI()

        //search
        searchProductEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    adapterProductSeller!!.filter.filter(s)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        logoutBtn.setOnClickListener {
            makeMeOffline()
        }
        editProfileBtn.setOnClickListener {
            startActivity(Intent(this@MainSellerActivity, ProfileEditSellerActivity::class.java))
        }
        addProductBtn.setOnClickListener {
            startActivity(Intent(this@MainSellerActivity, AddProductActivity::class.java))
        }
        tabProductsTv.setOnClickListener {
            showProductsUI()
        }
        tabOrdersTv.setOnClickListener {
            showOrdersUI()
        }
        filterProductBtn.setOnClickListener(View.OnClickListener {
            val builder = AlertDialog.Builder(this@MainSellerActivity)
            builder.setTitle("Choose Category:")
                    .setItems(Constants.productCategories1) { dialog, which ->
                        //get selected item
                        val selected = Constants.productCategories1[which]
                        filteredProductsTv.text = selected
                        if (selected == "All") {
                            //load all
                            loadAllProducts()
                        } else {
                            //load filtered
                            loadFilteredProducts(selected)
                        }
                    }
                    .show()
        })
        filterOrderBtn.setOnClickListener(View.OnClickListener {
            //options to display in diolog
            val options = arrayOf("All", "In Progress", "Completed", "Cancelled")
            //dialog
            val builder = AlertDialog.Builder(this@MainSellerActivity)
            builder.setTitle("Filter Orders:")
                    .setItems(options) { dialog, which ->
                        //handle item clicks
                        if (which == 0) {
                            //All clicked
                            filteredOrdersTv.text = "Showing All Orders"
                            adapterOrderShop!!.filter.filter("") //show all orders
                        } else {
                            val optionClicked = options[which]
                            filteredOrdersTv.text = "Showing $optionClicked Orders" //e.g. Showing Completed Orders
                            adapterOrderShop!!.filter.filter(optionClicked)
                        }
                    }
                    .show()
        })
        reviewsBtn.setOnClickListener(View.OnClickListener { //open same reviews activity as used in user main page
            val intent = Intent(this@MainSellerActivity, ShopReviewsActivity::class.java)
            intent.putExtra("shopUid", "" + firebaseAuth.uid)
            startActivity(intent)
        })

        //start settings screen
        settingsBtn.setOnClickListener(View.OnClickListener { startActivity(Intent(this@MainSellerActivity, SettingsActivity::class.java)) })
    }

    private fun loadAllOrders() {
        //init array list
        orderShopArrayList = ArrayList()

        //load orders of shop
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Orders")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //clear list before adding new data in it
                        orderShopArrayList!!.clear()
                        for (ds in dataSnapshot.children) {
                            val modelOrderShop = ds.getValue(ModelOrderShop::class.java)!!
                            //add to list
                            orderShopArrayList!!.add(modelOrderShop)
                        }
                        //setup adapter
                        adapterOrderShop = AdapterOrderShop(this@MainSellerActivity, orderShopArrayList)
                        //set adapter to recyclerview
                        ordersRv!!.adapter = adapterOrderShop
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    private fun loadFilteredProducts(selected: String) {
        productList = ArrayList()

        //get all products
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth.uid!!).child("Products")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //before getting reset list
                        productList!!.clear()
                        for (ds in dataSnapshot.children) {
                            val productCategory = "" + ds.child("productCategory").value

                            //if selected category matches product category then add in list
                            if (selected == productCategory) {
                                val modelProduct = ds.getValue(ModelProduct::class.java)!!
                                productList!!.add(modelProduct)
                            }
                        }
                        //setup adapter
                        adapterProductSeller = AdapterProductSeller(this@MainSellerActivity, productList)
                        //set adabter
                        productsRv!!.adapter = adapterProductSeller
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    private fun loadAllProducts() {
        productList = ArrayList()

        //get all products
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth.uid!!).child("Products")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //before getting reset list
                        productList!!.clear()
                        for (ds in dataSnapshot.children) {
                            val modelProduct = ds.getValue(ModelProduct::class.java)!!
                            productList!!.add(modelProduct)
                        }
                        //setup adapter
                        adapterProductSeller = AdapterProductSeller(this@MainSellerActivity, productList)
                        //set adabter
                        productsRv!!.adapter = adapterProductSeller
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    private fun showProductsUI() {
        //show products ui and hide orders ui
        productsRl!!.visibility = View.VISIBLE
        ordersRl!!.visibility = View.GONE
        tabProductsTv!!.setTextColor(resources.getColor(R.color.colorBlack))
        tabProductsTv!!.setBackgroundResource(R.drawable.shape_rect04)
        tabOrdersTv!!.setTextColor(resources.getColor(R.color.colorWhite))
        tabOrdersTv!!.setBackgroundColor(resources.getColor(android.R.color.transparent))
    }

    private fun showOrdersUI() {
        //show orders ui and hide products ui
        productsRl!!.visibility = View.GONE
        ordersRl!!.visibility = View.VISIBLE
        tabProductsTv!!.setTextColor(resources.getColor(R.color.colorWhite))
        tabProductsTv!!.setBackgroundColor(resources.getColor(android.R.color.transparent))
        tabOrdersTv!!.setTextColor(resources.getColor(R.color.colorBlack))
        tabOrdersTv!!.setBackgroundResource(R.drawable.shape_rect04)
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
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    toast(e.message!!)
                }
    }

    private fun checkUser() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            startActivity(Intent(this@MainSellerActivity, LoginActivityy::class.java))
            finish()
        } else {
            loadMyInfo()
        }
    }

    private fun loadMyInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.orderByChild("uid").equalTo(firebaseAuth.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (ds in dataSnapshot.children) {
                            //get data from db
                            val name = "" + ds.child("name").value
                            val accountType = "" + ds.child("accountType").value
                            val email = "" + ds.child("email").value
                            val shopName = "" + ds.child("shopName").value
                            val profileImage = "" + ds.child("profileImage").value

                            //set data to ui
                            nameTv!!.text = name
                            shopNameTv!!.text = shopName
                            emailTv!!.text = email
                            try {
                                Picasso.get().load(profileImage).placeholder(R.drawable.ic_store).into(profileIv)
                            } catch (e: Exception) {
                                profileIv!!.setImageResource(R.drawable.ic_store)
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }
}