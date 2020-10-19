package asdasd.com.grosery3.db

import androidx.room.*

@Dao
interface ImageUriDao {

    @Insert
    suspend fun addImageUri(imageUri: ImageUri)

    @Query("SELECT * FROM imageuri ORDER BY productId")
    suspend fun getAllImageUris() : MutableList<ImageUri>

    @Delete
    suspend fun deleteImageUri(imageUri: ImageUri)

}