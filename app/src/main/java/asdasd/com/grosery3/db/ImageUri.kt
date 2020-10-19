package asdasd.com.grosery3.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class ImageUri(
        @PrimaryKey
        val productId: String? = null,
        val imageUri: String? = null
)