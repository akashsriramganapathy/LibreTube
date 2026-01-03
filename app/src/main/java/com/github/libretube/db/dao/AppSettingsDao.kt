package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.AppSetting

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM AppSetting")
    suspend fun getAll(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: AppSetting)

    @Query("DELETE FROM AppSetting WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM AppSetting")
    suspend fun deleteAll()
}
