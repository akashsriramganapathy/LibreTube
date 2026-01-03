package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class AppSetting(
    @PrimaryKey
    val key: String,
    val value: String
)
