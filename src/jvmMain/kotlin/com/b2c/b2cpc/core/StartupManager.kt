package com.b2c.b2cpc.core

import java.io.File

object StartupManager {
    private const val APP_NAME = "B2CPC"
    
    // jpackage로 패키징된 실행 파일(.exe)의 절대 경로를 가져옵니다.
    // IDE에서 직접 실행할 때는 null이 될 수 있습니다.
    private val exePath: String? by lazy {
        System.getProperty("jpackage.app-path")
    }

    fun isStartupEnabled(): Boolean {
        if (exePath == null) return false
        return try {
            val process = ProcessBuilder(
                "reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", APP_NAME
            ).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor() == 0 && output.contains(APP_NAME, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun setStartupEnabled(enabled: Boolean) {
        val path = exePath ?: return
        try {
            if (enabled) {
                // 부팅 시 자동 실행 등록 (강제 덮어쓰기 /f)
                ProcessBuilder(
                    "reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", 
                    "/v", APP_NAME, "/t", "REG_SZ", "/d", "\"$path\"", "/f"
                ).start().waitFor()
            } else {
                // 자동 실행 해제
                ProcessBuilder(
                    "reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", 
                    "/v", APP_NAME, "/f"
                ).start().waitFor()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // 현재 패키징된 버전인지 체크 (UI에서 버튼 활성화 여부 판단위해)
    fun isPackagedEnvironment(): Boolean {
        return exePath != null
    }
}
