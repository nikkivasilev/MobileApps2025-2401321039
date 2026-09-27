package com.macrosnap.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MacroSnapApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch { container.workoutRepository.seedExercisesIfEmpty() }
        scope.launch { container.foodRepository.deleteOrphanPhotos() }
    }
}

val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as MacroSnapApp).container
