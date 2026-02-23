package com.b2c.b2cpc.core

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

object AutoUpdater {

    private const val GITHUB_OWNER = "AutoIlon"
    private const val GITHUB_REPO = "B2CPC"
    private const val GITHUB_TOKEN = "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"
    
    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * GitHub API를 찔러 최신 버전을 가져오고, 
     * 현재 버전보다 높으면 다운로드 URL을 콜백으로 넘겨줍니다.
     */
    suspend fun checkForUpdates(currentVersion: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $GITHUB_TOKEN")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = Gson().fromJson(responseJson, JsonObject::class.java)

                // 태그 이름 (예: "v1.0.1" -> "1.0.1")
                val latestVersionTag = jsonObject.get("tag_name")?.asString?.replace("v", "") ?: ""
                
                // 첨부파일(assets) 목록 중 .exe 파일 찾기
                val assets = jsonObject.getAsJsonArray("assets")
                var downloadUrl = ""
                for (asset in assets) {
                    val assetObj = asset.asJsonObject
                    val name = assetObj.get("name")?.asString ?: ""
                    if (name.endsWith(".exe")) {
                        // Private 저장소는 브라우저 URL이 아닌 API URL(url)을 통해 Accept Header와 토큰을 첨부해 다운로드해야 함
                        downloadUrl = assetObj.get("url")?.asString ?: ""
                        break
                    }
                }

                if (latestVersionTag.isNotBlank() && downloadUrl.isNotBlank()) {
                    // 버전 비교 로직 (단순 문자열 비교 또는 커스텀 로직)
                    // (ex: "1.0.1" > "1.0.0" 이라면 업데이트 필요)
                    if (isNewerVersion(currentVersion, latestVersionTag)) {
                        return@withContext Pair(latestVersionTag, downloadUrl)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    /**
     * 버전을 비교하는 단순 함수 
     * (V1: 1.0.0, V2: 1.0.1 일 때 V2가 크면 true 반환)
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(currParts.size, latestParts.size)
        for (i in 0 until length) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    /**
     * 주어진 URL에서 .exe 파일을 받아와 임시 경로에 저장하고 바로 실행합니다.
     */
    suspend fun downloadAndInstallUpdate(downloadUrl: String, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        try {
            var url = URL(downloadUrl)
            var connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $GITHUB_TOKEN")
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.instanceFollowRedirects = false // 수동 Redirect 처리 (S3에 Token 넘어가면 에러 발생하므로)
            connection.connect()
            
            // GitHub의 API Asset 다운로드는 S3 버킷으로 리다이렉트 (302) 됨
            if (connection.responseCode in 300..399) {
                val redirectUrl = connection.getHeaderField("Location")
                url = URL(redirectUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                // S3 버킷 접속 시에는 토큰을 빼야 함
                connection.connect()
            }
            
            val fileLength = connection.contentLength
            val tempFile = File(System.getProperty("java.io.tmpdir"), "B2CPC_update_${System.currentTimeMillis()}.exe")
            
            val input = connection.inputStream
            val output = FileOutputStream(tempFile)
            
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    onProgress((total * 100 / fileLength).toInt())
                }
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()

            // 다운로드 성공 후 윈도우 실행 프로세스로 .exe 시작
            if (tempFile.exists()) {
                val osName = System.getProperty("os.name").lowercase()
                if (osName.contains("win")) {
                    ProcessBuilder("cmd", "/c", "start", tempFile.absolutePath).start()
                    // 업데이트 설치를 원활히 하기 위해 현재 앱을 강제 종료
                    exitProcess(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1) // 장애 발생
        }
    }
}
