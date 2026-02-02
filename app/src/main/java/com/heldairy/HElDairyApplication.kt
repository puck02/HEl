package com.heldairy

import android.app.Application
import com.heldairy.core.di.AppContainer
import com.heldairy.core.di.AppContainerImpl

class HElDairyApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainerImpl(this) }
}
