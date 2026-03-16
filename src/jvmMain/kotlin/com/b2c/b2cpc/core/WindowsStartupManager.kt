package com.b2c.b2cpc.core

import java.io.File

object WindowsStartupManager {
    fun setStartup(enable: Boolean) {
        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("win")) return
        
        try {
            val appData = System.getenv("APPDATA") ?: return
            val startUpFolder = File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
            if (!startUpFolder.exists()) return
            
            // jar나 exe로 실행되는 경우 현재 실행 파일 경로 찾기
            // 개발 환경에서는 gradlew를 실행하도록 설정
            val currentDir = System.getProperty("user.dir")
            val executablePath = if (File(currentDir, "B2CPC.exe").exists()) {
                File(currentDir, "B2CPC.exe").absolutePath
            } else if (File(currentDir, "gradlew.bat").exists()) {
                "\"${File(currentDir, "gradlew.bat").absolutePath}\" run"
            } else {
                 return // 실행 파일을 찾을 수 없음
            }

            val batchFile = File(startUpFolder, "B2CPC_AutoStart.bat")
            
            if (enable) {
                // 백그라운드로 실행하게 만들 수도 있지만 (vbs 등 필요) 
                // 가장 간단하게 bat으로 등록
                val script = """
                    @echo off
                    cd /d "$currentDir"
                    start "" $executablePath
                """.trimIndent()
                batchFile.writeText(script)
            } else {
                if (batchFile.exists()) {
                    batchFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isStartupEnabled(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("win")) return false
        
        return try {
            val appData = System.getenv("APPDATA") ?: return false
            val startUpFolder = File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
            val batchFile = File(startUpFolder, "B2CPC_AutoStart.bat")
            batchFile.exists()
        } catch (e: Exception) {
            false
        }
    }
}
