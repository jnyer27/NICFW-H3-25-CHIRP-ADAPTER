package com.nicfw.tdh3editor

import android.app.Application
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookCredentialStore

class NicFwApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RepeaterBookCredentialStore.install(this)
    }
}
