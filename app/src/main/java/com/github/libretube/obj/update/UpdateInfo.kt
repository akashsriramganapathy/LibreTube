package com.github.libretube.obj.update

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UpdateInfo(
    val name: String,
    val body: String,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<Asset> = emptyList()
) : Parcelable

@Serializable
@Parcelize
data class Asset(
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("content_type") val contentType: String,
    val name: String
) : Parcelable
