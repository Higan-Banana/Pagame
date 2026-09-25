package gt.pagame.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class PagameDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: PagameDatabase? = null

        fun getInstance(context: Context): PagameDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PagameDatabase::class.java,
                    "pagame.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
