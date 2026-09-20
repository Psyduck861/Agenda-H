package com.example.data

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AgendaHFolderManager {

    fun getCandidateDirs(context: Context): List<File> {
        val dirs = mutableListOf<File>()

        // 1. Documents / Agenda H
        try {
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            dirs.add(File(docs, "Agenda H"))
        } catch (_: Exception) {}

        // 2. Download / Agenda H
        try {
            val down = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dirs.add(File(down, "Agenda H"))
        } catch (_: Exception) {}

        // 3. Storage root / Agenda H
        try {
            val root = Environment.getExternalStorageDirectory()
            dirs.add(File(root, "Agenda H"))
        } catch (_: Exception) {}

        // 4. App-specific external files dir / Agenda H
        try {
            context.getExternalFilesDir(null)?.let {
                dirs.add(File(it, "Agenda H"))
            }
        } catch (_: Exception) {}

        // 5. Internal files dir / Agenda H
        try {
            dirs.add(File(context.filesDir, "Agenda H"))
        } catch (_: Exception) {}

        return dirs
    }

    fun getPreferredDir(context: Context): File {
        val candidates = getCandidateDirs(context)
        // First check if any existing directory exists and is writable
        for (dir in candidates) {
            if (dir.exists() && dir.isDirectory && dir.canWrite()) {
                return dir
            }
        }
        // Try creating candidate in Documents or Download
        for (dir in candidates) {
            try {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (dir.exists() && dir.canWrite()) {
                    return dir
                }
            } catch (_: Exception) {}
        }
        // Fallback to internal filesDir / Agenda H
        val fallback = File(context.filesDir, "Agenda H")
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }

    fun saveBackupToAgendaH(context: Context, json: String): Pair<Boolean, String> {
        if (json.isBlank()) return Pair(false, "Conteúdo de backup vazio.")
        
        val candidates = getCandidateDirs(context)
        var savedPath = ""
        var success = false

        for (dir in candidates) {
            try {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (dir.exists() && dir.canWrite()) {
                    // Standard named backup
                    val standardFile = File(dir, "agenda_backup.json")
                    standardFile.writeText(json, Charsets.UTF_8)

                    // Also a backup named agenda_h_backup.json for max compatibility
                    val altFile = File(dir, "agenda_h_backup.json")
                    altFile.writeText(json, Charsets.UTF_8)

                    savedPath = standardFile.absolutePath
                    success = true
                    break
                }
            } catch (e: Exception) {
                // Try next
            }
        }

        return if (success) {
            Pair(true, savedPath)
        } else {
            Pair(false, "Não foi possível gravar na pasta 'Agenda H'. Verifique as permissões de armazenamento.")
        }
    }

    fun readBackupFromAgendaH(context: Context): Pair<String?, String> {
        val candidates = getCandidateDirs(context)
        val fileNames = listOf("agenda_backup.json", "agenda_h_backup.json", "backup.json", "agenda_backup_auto.json")

        for (dir in candidates) {
            if (dir.exists() && dir.isDirectory) {
                for (name in fileNames) {
                    val file = File(dir, name)
                    if (file.exists() && file.length() > 0) {
                        try {
                            val content = file.readText(Charsets.UTF_8)
                            if (content.isNotBlank()) {
                                return Pair(content, file.absolutePath)
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Check any .json in the folder
                try {
                    val jsonFiles = dir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }
                    if (!jsonFiles.isNullOrEmpty()) {
                        val latest = jsonFiles.maxByOrNull { it.lastModified() }
                        if (latest != null && latest.length() > 0) {
                            val content = latest.readText(Charsets.UTF_8)
                            if (content.isNotBlank()) {
                                return Pair(content, latest.absolutePath)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return Pair(null, "Nenhum arquivo de backup (.json) encontrado na pasta 'Agenda H'.")
    }
}
