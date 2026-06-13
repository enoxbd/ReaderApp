package com.enox.enoxpay.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.enox.enoxpay.data.local.dao.ApiConfigDao
import com.enox.enoxpay.data.local.dao.PlatformDao
import com.enox.enoxpay.data.local.dao.RegexDao
import com.enox.enoxpay.data.local.dao.SmsDao
import com.enox.enoxpay.data.local.entity.ApiConfigEntity
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.data.local.entity.RegexEntity
import com.enox.enoxpay.data.local.entity.SmsEntity

@Database(
    entities = [
        PlatformEntity::class,
        RegexEntity::class,
        SmsEntity::class,
        ApiConfigEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun platformDao(): PlatformDao
    abstract fun regexDao(): RegexDao
    abstract fun smsDao(): SmsDao
    abstract fun apiConfigDao(): ApiConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enox_pay_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
