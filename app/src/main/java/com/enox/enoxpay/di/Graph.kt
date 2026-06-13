package com.enox.enoxpay.di

import android.content.Context
import com.enox.enoxpay.data.local.AppDatabase
import com.enox.enoxpay.data.repository.EnoxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object Graph {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: EnoxRepository
        private set
    
    lateinit var themeManager: com.enox.enoxpay.util.ThemeManager
        private set
        
    lateinit var appPreferencesManager: com.enox.enoxpay.util.AppPreferencesManager
        private set

    val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun provide(context: Context) {
        themeManager = com.enox.enoxpay.util.ThemeManager(context)
        appPreferencesManager = com.enox.enoxpay.util.AppPreferencesManager(context)
        database = AppDatabase.getDatabase(context)
        repository = EnoxRepository(
            platformDao = database.platformDao(),
            regexDao = database.regexDao(),
            smsDao = database.smsDao(),
            apiConfigDao = database.apiConfigDao()
        )
        
        // Populate default data on first run
        CoroutineScope(Dispatchers.IO).launch {
            repository.initDefaults()
        }
    }
}
