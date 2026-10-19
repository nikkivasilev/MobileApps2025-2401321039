package com.macrosnap.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Starts the app with [TestMacroSnapApp], so UI tests never touch the real food log. */
class MacroSnapTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestMacroSnapApp::class.java.name, context)
}

class TestMacroSnapApp : MacroSnapApp() {
    override fun createContainer() = AppContainer(this, inMemoryDatabase = true)
}
