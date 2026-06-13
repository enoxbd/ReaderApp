package com.enox.enoxpay

import android.app.Application
import com.enox.enoxpay.di.Graph
import com.enox.enoxpay.service.WatchdogWorker

class EnoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
        WatchdogWorker.enqueuePeriodic(this)
    }
}
