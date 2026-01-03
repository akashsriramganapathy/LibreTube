package com.github.libretube.helpers

import androidx.preference.PreferenceDataStore
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.AppSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

object RoomPreferenceDataStore : PreferenceDataStore() {
    private val cache = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        
        runBlocking {
            try {
                val settings = DatabaseHolder.Database.appSettingsDao().getAll()
                settings.forEach { cache[it.key] = it.value }
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAll(): Map<String, String> {
        return cache.toMap()
    }

    override fun putString(key: String, value: String?) {
        updateCacheAndDb(key, value)
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        updateCacheAndDb(key, values?.joinToString(","))
    }

    override fun putInt(key: String, value: Int) {
        updateCacheAndDb(key, value.toString())
    }

    override fun putLong(key: String, value: Long) {
        updateCacheAndDb(key, value.toString())
    }

    override fun putFloat(key: String, value: Float) {
        updateCacheAndDb(key, value.toString())
    }

    override fun putBoolean(key: String, value: Boolean) {
        updateCacheAndDb(key, value.toString())
    }

    override fun getString(key: String, defValue: String?): String? {
        return cache[key] ?: defValue
    }

    override fun getStringSet(key: String, defValue: Set<String>?): Set<String>? {
        return cache[key]?.split(",")?.toSet() ?: defValue
    }

    override fun getInt(key: String, defValue: Int): Int {
        return cache[key]?.toIntOrNull() ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return cache[key]?.toLongOrNull() ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return cache[key]?.toFloatOrNull() ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return cache[key]?.toBooleanStrictOrNull() ?: defValue
    }

    private fun updateCacheAndDb(key: String, value: String?) {
        if (value == null) {
            cache.remove(key)
            scope.launch {
                DatabaseHolder.Database.appSettingsDao().delete(key)
            }
        } else {
            cache[key] = value
            scope.launch {
                DatabaseHolder.Database.appSettingsDao().insert(AppSetting(key, value))
            }
        }
    }
    
    fun clear() {
        cache.clear()
        scope.launch {
            DatabaseHolder.Database.appSettingsDao().deleteAll()
        }
    }
}
