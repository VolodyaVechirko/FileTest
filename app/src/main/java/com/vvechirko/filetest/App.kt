package com.vvechirko.filetest

import android.app.Application

class App: Application() {

    companion object {
        lateinit var instance: App

        fun appContext() = instance.applicationContext

        fun contentResolver() = instance.contentResolver
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}