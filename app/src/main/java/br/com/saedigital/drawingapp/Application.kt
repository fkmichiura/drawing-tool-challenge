package br.com.saedigital.drawingapp

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class App : Application(){

    override fun onCreate() {
        super.onCreate()

        Realm.init(this)

        val realmConfiguration = RealmConfiguration.Builder()
                .name("drawingApp.realm")
                .schemaVersion(0)
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)
    }
}