package com.example

import android.app.Application
import com.example.ai.AiManager
import com.example.ai.AiProviderClient
import com.example.ai.ToolExecutor
import com.example.data.local.SettingsDataStore
import com.example.data.local.YadetDatabase
import com.example.data.repository.YadetRepository

class YadetApplication : Application() {

    lateinit var database: YadetDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var repository: YadetRepository
        private set
    lateinit var toolExecutor: ToolExecutor
        private set
    lateinit var aiClient: AiProviderClient
        private set
    lateinit var aiManager: AiManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = YadetDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        repository = YadetRepository(this, database, settingsDataStore)
        toolExecutor = ToolExecutor(repository)
        aiClient = AiProviderClient(toolExecutor)
        aiManager = AiManager(this, aiClient, settingsDataStore)
    }
}
