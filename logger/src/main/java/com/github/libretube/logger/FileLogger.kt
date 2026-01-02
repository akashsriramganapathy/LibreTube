package com.github.libretube.logger

import android.content.Context
import android.util.Log
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.concurrent.Executors

object FileLogger {
    private var logFile: File? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun init(context: Context) {
        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir != null) {
            val logDir = File(externalFilesDir, "logs")
            logDir.mkdirs()
            logFile = File(logDir, "app.log")
            
            // Clean up old log file if too large > 2MB
            if (logFile!!.exists() && logFile!!.length() > 2 * 1024 * 1024) {
               logFile!!.delete()
            }
        }
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLog("DEBUG", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeLog("ERROR", tag, "$message ${throwable?.stackTraceToString() ?: ""}")
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog("INFO", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        writeLog("WARN", tag, message)
    }

    private fun writeLog(level: String, tag: String, message: String) {
        val file = logFile ?: return
        executor.execute {
            try {
                val currentMoment = Clock.System.now()
                val datetime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
                val timestamp = "${datetime.date} ${datetime.hour}:${datetime.minute}:${datetime.second}.${datetime.nanosecond / 1000000}"
                
                FileWriter(file, true).use { fw ->
                    PrintWriter(fw).use { pw ->
                        pw.println("$timestamp $level/$tag: $message")
                    }
                }
            } catch (e: Exception) {
                // Ignore fallback to normal logging
            }
        }
    }
    fun getLogContent(): String {
        return logFile?.readText() ?: ""
    }

    fun clearLog() {
        logFile?.writeText("")
    }
}
