package com.macrosnap.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class MacroSnapApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch { container.workoutRepository.seedExercisesIfEmpty() }
        scope.launch { container.foodRepository.deleteOrphanPhotos() }
    }

    protected open fun createContainer() = AppContainer(this)
}

val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as MacroSnapApp).container
