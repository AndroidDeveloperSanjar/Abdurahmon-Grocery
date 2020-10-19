package asdasd.com.grosery3.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
        entities = [ImageUri::class],
        version = 2,
        exportSchema = false
)
abstract class ImageUriDB : RoomDatabase() {

    abstract fun getImageUriDao(): ImageUriDao

    companion object {

        @Volatile
        private var instance: ImageUriDB? = null
        private val LOCK = Any()

        operator fun invoke(
                context: Context
        ) = instance ?: synchronized(LOCK) {
            instance ?: buildDB(context).also {
                instance = it
            }
        }

        private fun buildDB(
                context: Context
        ) = Room.databaseBuilder(
                context.applicationContext,
                ImageUriDB::class.java,
                "imageUriDB"
        )
                .fallbackToDestructiveMigration()
                .build()
    }
}