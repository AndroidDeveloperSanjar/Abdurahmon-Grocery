package asdasd.com.grosery3.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import asdasd.com.grosery3.R
import asdasd.com.grosery3.db.ImageUriDB
import asdasd.com.grosery3.logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AllImagesActivity : AppCompatActivity() {

    private var image: String? = null
    private var productId: String? = null
    private lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_images)
        job = Job()
//        intent.apply {
//            image = getStringExtra("image")
//            productId = getStringExtra("product_id")
//        }
//        if (image != null && productId != null) {
//            try {
//                Picasso.get().load(image).placeholder(R.drawable.ic_add_shopping_primary).into(iv_product)
//            } catch (e: Exception) {
//                iv_product.setImageResource(R.drawable.ic_add_shopping_primary)
//            }
//        }
        CoroutineScope(Main+job).launch {
            applicationContext?.let {
                val imageUriList = ImageUriDB(it).getImageUriDao().getAllImageUris()
                logging("$imageUriList")
            }
        }
    }
}