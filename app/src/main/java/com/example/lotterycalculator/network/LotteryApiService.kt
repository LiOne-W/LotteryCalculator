package com.example.lotterycalculator.network

import android.util.Log
import com.example.lotterycalculator.data.model.DrawResult
import com.example.lotterycalculator.data.model.LotteryType
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class LotteryApiService {
    companion object {
        private const val TAG = "LotteryApiService"
    }
    private val appId = "your_app_id_here"
    private val appSecret = "your_app_secret_here"
    private val baseUrl = "https://www.mxnzp.com/api"
    
    suspend fun getLatestDrawResult(lotteryType: LotteryType): DrawResult? {
        return withContext(Dispatchers.IO) {
            try {
                val lotteryCode = when (lotteryType) {
                    LotteryType.DALETOU -> "cjdlt"  // 超级大乐透
                    LotteryType.SHUANGSEQIU -> "ssq"
                }
                Log.d(TAG, "请求${lotteryType.name}最新号码，彩票代码: $lotteryCode")
                
                val params = mapOf(
                    "app_id" to appId,
                    "app_secret" to appSecret,
                    "code" to lotteryCode
                )
                
                val urlWithParams = buildUrl("$baseUrl/lottery/common/latest", params)
                Log.d(TAG, "API请求URL: $urlWithParams")
                val response = makeGetRequest(urlWithParams)
                Log.d(TAG, "API响应: $response")
                val result = parseLatestApiResponse(response, lotteryType)
                if (result != null) {
                    Log.d(TAG, "解析成功，期号: ${result.period}")
                } else {
                    Log.w(TAG, "解析返回null")
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "获取${lotteryType.name}最新号码失败", e)
                null
            }
        }
    }
    
    suspend fun getHistoryDrawResults(lotteryType: LotteryType, count: Int): List<DrawResult> {
        return withContext(Dispatchers.IO) {
            try {
                val lotteryCode = when (lotteryType) {
                    LotteryType.DALETOU -> "cjdlt"  // 超级大乐透
                    LotteryType.SHUANGSEQIU -> "ssq"
                }
                
                val params = mapOf(
                    "app_id" to appId,
                    "app_secret" to appSecret,
                    "code" to lotteryCode,
                    "size" to count.toString()
                )
                
                val urlWithParams = buildUrl("$baseUrl/lottery/common/history", params)
                val response = makeGetRequest(urlWithParams)
                parseHistoryApiResponse(response, lotteryType)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * 获取指定期号的开奖结果
     * @param lotteryType 彩票类型
     * @param period 期号
     * @return 开奖结果，如果获取失败则返回null
     */
    suspend fun getDrawResultByPeriod(lotteryType: LotteryType, period: String): DrawResult? {
        return withContext(Dispatchers.IO) {
            try {
                val lotteryCode = when (lotteryType) {
                    LotteryType.DALETOU -> "cjdlt"  // 超级大乐透
                    LotteryType.SHUANGSEQIU -> "ssq"
                }
                Log.d(TAG, "请求${lotteryType.name}指定期号，期号: $period, 彩票代码: $lotteryCode")
                
                val params = mapOf(
                    "app_id" to appId,
                    "app_secret" to appSecret,
                    "code" to lotteryCode,
                    "expect" to period
                )
                
                val urlWithParams = buildUrl("$baseUrl/lottery/common/aim_lottery", params)
                Log.d(TAG, "API请求URL: $urlWithParams")
                val response = makeGetRequest(urlWithParams)
                Log.d(TAG, "API响应: $response")
                val result = parseAimLotteryApiResponse(response, lotteryType)
                if (result != null) {
                    Log.d(TAG, "解析成功，期号: ${result.period}")
                } else {
                    Log.w(TAG, "解析返回null")
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "获取${lotteryType.name}期号结果失败", e)
                null
            }
        }
    }
    
    private fun generateSignature(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val paramString = sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        
        val secretKey = SecretKeySpec(appSecret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val signatureBytes = mac.doFinal(paramString.toByteArray())
        
        return Base64.getEncoder().encodeToString(signatureBytes)
    }
    
    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        val encodedParams = params.entries.joinToString("&") { 
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}" 
        }
        return "$baseUrl?$encodedParams"
    }
    
    private fun makeGetRequest(urlString: String): String {
        Log.d(TAG, "开始HTTP请求: $urlString")
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        Log.d(TAG, "HTTP响应码: $responseCode")
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            Log.d(TAG, "HTTP响应内容长度: ${response.length}")
            return response
        } else {
            val errorStream = connection.errorStream
            val errorResponse = if (errorStream != null) {
                val reader = BufferedReader(InputStreamReader(errorStream))
                val error = reader.readText()
                reader.close()
                error
            } else {
                "无错误响应体"
            }
            Log.e(TAG, "HTTP请求失败，响应码: $responseCode, 错误响应: $errorResponse")
            throw Exception("HTTP error code: $responseCode")
        }
    }
    
    private fun parseLatestApiResponse(jsonString: String, lotteryType: LotteryType): DrawResult? {
        return try {
            val jsonObject = JsonParser().parse(jsonString).asJsonObject
            
            // 检查API响应状态
            val code = jsonObject.get("code").asInt
            if (code != 1) {
                // 如果code不是1，表示API调用失败
                val msg = jsonObject.get("msg").asString
                throw Exception("API调用失败: $msg (code: $code)")
            }
            
            val data = jsonObject.getAsJsonObject("data")
            
            val period = data.get("expect").asString
            val opencode = data.get("openCode").asString
            val opentime = data.get("time").asString
            
            // 解析开奖号码
            val (frontNumbers, backNumbers) = parseOpencode(opencode, lotteryType)
            val drawDate = parseDate(opentime)
            
            DrawResult(
                id = period,
                lotteryType = lotteryType,
                period = period,
                drawDate = drawDate,
                frontNumbers = frontNumbers,
                backNumbers = backNumbers
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseHistoryApiResponse(jsonString: String, lotteryType: LotteryType): List<DrawResult> {
        android.util.Log.d("LotteryApiService", "开始解析历史API响应，彩票类型: $lotteryType")
        return try {
            val jsonObject = JsonParser().parse(jsonString).asJsonObject
            
            // 检查API响应状态
            val code = jsonObject.get("code").asInt
            android.util.Log.d("LotteryApiService", "API响应码: $code")
            if (code != 1) {
                // 如果code不是1，表示API调用失败
                val msg = jsonObject.get("msg").asString
                android.util.Log.e("LotteryApiService", "API调用失败: $msg (code: $code)")
                throw Exception("API调用失败: $msg (code: $code)")
            }
            
            val dataArray = jsonObject.getAsJsonArray("data")
            android.util.Log.d("LotteryApiService", "数据数组大小: ${dataArray.size()}")
            
            val results = mutableListOf<DrawResult>()
            for (i in 0 until dataArray.size()) {
                val data = dataArray.get(i).asJsonObject
                
                val period = data.get("expect").asString
                val opencode = data.get("openCode").asString
                val opentime = data.get("time").asString
                
                // 解析开奖号码
                val (frontNumbers, backNumbers) = parseOpencode(opencode, lotteryType)
                val drawDate = parseDate(opentime)
                
                results.add(DrawResult(
                    id = period,
                    lotteryType = lotteryType,
                    period = period,
                    drawDate = drawDate,
                    frontNumbers = frontNumbers,
                    backNumbers = backNumbers
                ))
            }
            
            android.util.Log.d("LotteryApiService", "解析成功，返回 ${results.size} 条历史记录")
            if (results.isNotEmpty()) {
                android.util.Log.d("LotteryApiService", "第1条记录期号: ${results.first().period}, 最后1条记录期号: ${results.last().period}")
            }
            results
        } catch (e: Exception) {
            android.util.Log.e("LotteryApiService", "解析历史API响应失败", e)
            emptyList()
        }
    }
    
    private fun parseAimLotteryApiResponse(jsonString: String, lotteryType: LotteryType): DrawResult? {
        android.util.Log.d("LotteryApiService", "开始解析指定期号API响应，彩票类型: $lotteryType")
        return try {
            val jsonObject = JsonParser().parse(jsonString).asJsonObject
            
            // 检查API响应状态
            val code = jsonObject.get("code").asInt
            android.util.Log.d("LotteryApiService", "API响应码: $code")
            if (code != 1) {
                // 如果code不是1，表示API调用失败
                val msg = jsonObject.get("msg").asString
                android.util.Log.e("LotteryApiService", "API调用失败: $msg (code: $code)")
                throw Exception("API调用失败: $msg (code: $code)")
            }
            
            // aim_lottery接口可能直接返回数据，也可能嵌套在data字段中
            val data = if (jsonObject.has("data")) {
                jsonObject.getAsJsonObject("data")
            } else {
                // 如果没有data字段，假设整个响应就是数据
                jsonObject
            }
            
            val period = data.get("expect").asString
            val opencode = data.get("openCode").asString
            val opentime = data.get("time").asString
            
            // 解析开奖号码
            val (frontNumbers, backNumbers) = parseOpencode(opencode, lotteryType)
            val drawDate = parseDate(opentime)
            
            android.util.Log.d("LotteryApiService", "解析成功，期号: $period")
            
            DrawResult(
                id = period,
                lotteryType = lotteryType,
                period = period,
                drawDate = drawDate,
                frontNumbers = frontNumbers,
                backNumbers = backNumbers
            )
        } catch (e: Exception) {
            android.util.Log.e("LotteryApiService", "解析指定期号API响应失败", e)
            null
        }
    }
    
    private fun parseOpencode(opencode: String, lotteryType: LotteryType): Pair<List<Int>, List<Int>> {
        // 解析开奖号码字符串
        // 双色球格式: "03,04,14,22,23,33+04" (6个前区+1个后区)
        // 大乐透格式: "09,11,20,26,27+06+09" (5个前区+2个后区，两个+号分隔)
        
        when (lotteryType) {
            LotteryType.DALETOU -> {
                // 大乐透：处理两个+号的情况
                val parts = opencode.split("+")
                if (parts.size != 3) {
                    throw Exception("大乐透号码格式不正确，应为'前区号码+后区号码1+后区号码2': $opencode")
                }
                
                val frontNumbersStr = parts[0]
                val backNumber1 = parts[1]
                val backNumber2 = parts[2]
                
                val frontNumbers = frontNumbersStr.split(",").map { it.trim().toInt() }
                val backNumbers = listOf(backNumber1.toInt(), backNumber2.toInt())
                
                if (frontNumbers.size != 5) {
                    throw Exception("大乐透前区号码数量不正确: 应有5个号码，实际 ${frontNumbers.size} 个")
                }
                if (backNumbers.size != 2) {
                    throw Exception("大乐透后区号码数量不正确: 应有2个号码，实际 ${backNumbers.size} 个")
                }
                
                return Pair(frontNumbers, backNumbers)
            }
            LotteryType.SHUANGSEQIU -> {
                // 双色球：处理一个+号的情况
                val parts = opencode.split("+")
                if (parts.size != 2) {
                    throw Exception("双色球号码格式不正确，应为'前区号码+后区号码': $opencode")
                }
                
                val frontNumbersStr = parts[0]
                val backNumbersStr = parts[1]
                
                val frontNumbers = frontNumbersStr.split(",").map { it.trim().toInt() }
                val backNumbers = backNumbersStr.split(",").map { it.trim().toInt() }
                
                if (frontNumbers.size != 6) {
                    throw Exception("双色球前区号码数量不正确: 应有6个号码，实际 ${frontNumbers.size} 个")
                }
                if (backNumbers.size != 1) {
                    throw Exception("双色球后区号码数量不正确: 应有1个号码，实际 ${backNumbers.size} 个")
                }
                
                return Pair(frontNumbers, backNumbers)
            }
        }
    }
    
    private fun parseNumbers(jsonArray: com.google.gson.JsonArray): List<Int> {
        val numbers = mutableListOf<Int>()
        for (i in 0 until jsonArray.size()) {
            numbers.add(jsonArray.get(i).asInt)
        }
        return numbers
    }
    
    private fun parseDate(dateString: String): Date {
        return try {
            // API返回的时间格式: "2026-04-12 21:15:00"
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
    
    /**
     * 调用中奖结果计算接口
     * @param lotteryType 彩票类型
     * @param period 期号
     * @param ticketNumbers 彩票号码，格式为"前区号码@后区号码"
     * @return 中奖结果描述
     */
    suspend fun calculatePrize(lotteryType: LotteryType, period: String, ticketNumbers: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val lotteryCode = when (lotteryType) {
                    LotteryType.DALETOU -> "cjdlt"  // 超级大乐透
                    LotteryType.SHUANGSEQIU -> "ssq"
                }
                Log.d(TAG, "请求${lotteryType.name}中奖结果计算，期号: $period, 号码: $ticketNumbers")
                
                val params = mapOf(
                    "app_id" to appId,
                    "app_secret" to appSecret,
                    "code" to lotteryCode,
                    "expect" to period,
                    "checked_code" to ticketNumbers
                )
                
                val urlWithParams = buildUrl("$baseUrl/lottery/common/calc", params)
                Log.d(TAG, "API请求URL: $urlWithParams")
                val response = makeGetRequest(urlWithParams)
                Log.d(TAG, "API响应: $response")
                parseCalcApiResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "计算${lotteryType.name}中奖结果失败", e)
                null
            }
        }
    }
    
    private fun parseCalcApiResponse(jsonString: String): String? {
        return try {
            val jsonObject = JsonParser().parse(jsonString).asJsonObject
            
            // 检查API响应状态
            val code = jsonObject.get("code").asInt
            if (code != 1) {
                // 如果code不是1，表示API调用失败
                val msg = jsonObject.get("msg").asString
                throw Exception("API调用失败: $msg (code: $code)")
            }
            
            val data = jsonObject.getAsJsonObject("data")
            val resultDetails = data.get("resultDetails").asString
            resultDetails
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}