package com.example.lotterycalculator.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lotterycalculator.data.model.DrawResult
import com.example.lotterycalculator.data.model.LotteryType
import com.example.lotterycalculator.data.model.PeriodBoundary
import com.example.lotterycalculator.data.model.UserTicket
import com.example.lotterycalculator.network.LotteryApiService

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

class LotteryRepository(private val context: Context) {
    private val apiService = LotteryApiService()
    private val dataStore: DataStore<Preferences> = context.dataStore
    private val gson = Gson()
    
    companion object {
        // 彩票期号边界缓存（静态内存存储，所有Repository实例共享）
        private val periodBoundaries: MutableMap<LotteryType, PeriodBoundary> = mutableMapOf()
        
        // 历史数据缓存（静态内存存储，所有Repository实例共享）
        private val historicalResultsCache: MutableMap<LotteryType, List<DrawResult>> = mutableMapOf()
        
        // 开奖结果缓存（静态内存存储，所有Repository实例共享）
        private val drawResultCache: MutableMap<LotteryType, DrawResult> = mutableMapOf()
        
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lottery_preferences")
        private val KEY_USER_TICKETS_DALETOU = stringPreferencesKey("user_tickets_daletou")
        private val KEY_USER_TICKETS_SHUANGSEQIU = stringPreferencesKey("user_tickets_shuangseqiu")
    }
    suspend fun getLatestDrawResult(lotteryType: LotteryType): DrawResult {
        val result = apiService.getLatestDrawResult(lotteryType)
        if (result == null) {
            throw Exception("获取最新开奖结果失败")
        }
        // 缓存开奖结果
        cacheDrawResult(lotteryType, result)
        return result
    }

    /**
     * 获取指定期号的开奖结果
     * @param lotteryType 彩票类型
     * @param period 期号
     * @return 开奖结果
     * @throws Exception 如果获取失败
     */
    suspend fun getDrawResultByPeriod(lotteryType: LotteryType, period: String): DrawResult {
        android.util.Log.d("LotteryRepository", "请求指定期号开奖结果，彩票类型: $lotteryType, 期号: $period")
        val result = apiService.getDrawResultByPeriod(lotteryType, period)
        if (result == null) {
            throw Exception("获取期号 $period 开奖结果失败")
        }
        android.util.Log.d("LotteryRepository", "成功获取期号 $period 开奖结果")
        // 缓存开奖结果
        cacheDrawResult(lotteryType, result)
        return result
    }

    suspend fun getHistoricalDrawResults(lotteryType: LotteryType, updateBoundary: Boolean = false): List<DrawResult> {
        val results = apiService.getHistoryDrawResults(lotteryType, 30)
        // API返回的数据可能是正序（最旧到最新），需要反转以确保索引0=最新一期
        android.util.Log.d("LotteryRepository", "获取历史数据，原始数量: ${results.size}, 更新边界: $updateBoundary")
        if (results.isNotEmpty()) {
            android.util.Log.d("LotteryRepository", "原始顺序 - 第1期: ${results.first().period}, 最后1期: ${results.last().period}")
            android.util.Log.d("LotteryRepository", "反转后 - 第1期: ${results.asReversed().first().period}, 最后1期: ${results.asReversed().last().period}")
        }
        val reversedResults = results.asReversed()
        
        // 只有当调用方要求时才更新期号边界缓存
        if (updateBoundary) {
            updatePeriodBoundary(reversedResults, lotteryType)
        }
        
        // 更新历史数据缓存
        historicalResultsCache[lotteryType] = reversedResults
        android.util.Log.d("LotteryRepository", "历史数据缓存已更新 - 彩票类型: $lotteryType, 缓存数量: ${reversedResults.size}")
        
        return reversedResults
    }
    
    /**
     * 更新彩票期号边界缓存（从历史数据）
     * @param historicalResults 历史数据列表，必须按时间倒序排列（最新在前）
     * @param lotteryType 彩票类型
     */
    private fun updatePeriodBoundary(historicalResults: List<DrawResult>, lotteryType: LotteryType) {
        if (historicalResults.isEmpty()) {
            android.util.Log.w("LotteryRepository", "历史数据为空，无法更新期号边界")
            return
        }
        
        val boundary = PeriodBoundary.fromHistoricalResults(historicalResults, lotteryType)
        if (boundary != null) {
            periodBoundaries[lotteryType] = boundary
            android.util.Log.d("LotteryRepository", "更新期号边界 - 彩票类型: $lotteryType, 最新期: ${boundary.latestPeriod}, 最早期: ${boundary.earliestPeriod}")
        } else {
            android.util.Log.w("LotteryRepository", "创建期号边界失败")
        }
    }
    
    /**
     * 更新彩票期号边界缓存（直接设置边界对象）
     * @param boundary 期号边界对象
     */
    fun updatePeriodBoundary(boundary: PeriodBoundary) {
        periodBoundaries[boundary.lotteryType] = boundary
        android.util.Log.d("LotteryRepository", "直接更新期号边界 - 彩票类型: ${boundary.lotteryType}, 最新期: ${boundary.latestPeriod}, 最早期: ${boundary.earliestPeriod}")
    }
    
    /**
     * 获取彩票期号边界
     * @param lotteryType 彩票类型
     * @return 期号边界，如果未缓存则返回null
     */
    fun getPeriodBoundary(lotteryType: LotteryType): PeriodBoundary? {
        val boundary = periodBoundaries[lotteryType]
        android.util.Log.d("LotteryRepository", "获取期号边界 - 彩票类型: $lotteryType, 存在: ${boundary != null}, 缓存中的所有类型: ${periodBoundaries.keys}")
        return boundary
    }
    
    /**
     * 清除彩票期号边界缓存（用于测试或重新加载）
     */
    fun clearPeriodBoundaryCache() {
        periodBoundaries.clear()
        android.util.Log.d("LotteryRepository", "期号边界缓存已清除")
    }
    
    /**
     * 获取缓存的历史开奖数据
     * @param lotteryType 彩票类型
     * @return 历史数据列表，按时间倒序排列（最新在前），如果未缓存则返回null
     */
    fun getCachedHistoricalResults(lotteryType: LotteryType): List<DrawResult>? {
        return historicalResultsCache[lotteryType]
    }
    
    /**
     * 清除历史数据缓存（用于测试或重新加载）
     */
    fun clearHistoricalResultsCache() {
        historicalResultsCache.clear()
        android.util.Log.d("LotteryRepository", "历史数据缓存已清除")
    }
    
    /**
     * 获取缓存的开奖结果
     * @param lotteryType 彩票类型
     * @return 开奖结果，如果未缓存则返回null
     */
    fun getCachedDrawResult(lotteryType: LotteryType): DrawResult? {
        return drawResultCache[lotteryType]
    }
    
    /**
     * 保存开奖结果到缓存
     * @param lotteryType 彩票类型
     * @param drawResult 开奖结果
     */
    fun cacheDrawResult(lotteryType: LotteryType, drawResult: DrawResult) {
        drawResultCache[lotteryType] = drawResult
        android.util.Log.d("LotteryRepository", "开奖结果已缓存 - 彩票类型: $lotteryType, 期号: ${drawResult.period}")
    }
    
    /**
     * 清除开奖结果缓存（用于测试或重新加载）
     */
    fun clearDrawResultCache() {
        drawResultCache.clear()
        android.util.Log.d("LotteryRepository", "开奖结果缓存已清除")
    }
    
    private fun generateRandomNumbers(range: IntRange, count: Int, seed: Int): List<Int> {
        val random = kotlin.random.Random(seed)
        val numbers = mutableSetOf<Int>()
        while (numbers.size < count) {
            numbers.add(range.first + random.nextInt(range.last - range.first + 1))
        }
        return numbers.sorted()
    }



    suspend fun getUserTickets(lotteryType: LotteryType): List<UserTicket> {
        return try {
            val key = when (lotteryType) {
                LotteryType.DALETOU -> KEY_USER_TICKETS_DALETOU
                LotteryType.SHUANGSEQIU -> KEY_USER_TICKETS_SHUANGSEQIU
            }
            val json = dataStore.data.map { preferences ->
                preferences[key] ?: "[]"
            }.first()
            val type = object : TypeToken<List<UserTicket>>() {}.type
            gson.fromJson<List<UserTicket>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveUserTicket(ticket: UserTicket) {
        try {
            val key = when (ticket.lotteryType) {
                LotteryType.DALETOU -> KEY_USER_TICKETS_DALETOU
                LotteryType.SHUANGSEQIU -> KEY_USER_TICKETS_SHUANGSEQIU
            }
            dataStore.edit { preferences ->
                val json = preferences[key] ?: "[]"
                val type = object : TypeToken<MutableList<UserTicket>>() {}.type
                val tickets = gson.fromJson<MutableList<UserTicket>>(json, type) ?: mutableListOf()
                
                // 确保票有ID
                val ticketToSave = if (ticket.id.isNullOrEmpty()) {
                    ticket.copy(id = UUID.randomUUID().toString())
                } else {
                    ticket
                }
                
                val existingIndex = tickets.indexOfFirst { it.id == ticketToSave.id }
                if (existingIndex >= 0) {
                    tickets[existingIndex] = ticketToSave
                } else {
                    tickets.add(ticketToSave)
                }
                
                preferences[key] = gson.toJson(tickets)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteUserTicket(ticketId: String) {
        try {
            // 尝试从大乐透数据中删除
            dataStore.edit { preferences ->
                val json = preferences[KEY_USER_TICKETS_DALETOU] ?: "[]"
                val type = object : TypeToken<MutableList<UserTicket>>() {}.type
                val tickets = gson.fromJson<MutableList<UserTicket>>(json, type) ?: mutableListOf()
                tickets.removeIf { it.id == ticketId }
                preferences[KEY_USER_TICKETS_DALETOU] = gson.toJson(tickets)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            // 尝试从双色球数据中删除
            dataStore.edit { preferences ->
                val json = preferences[KEY_USER_TICKETS_SHUANGSEQIU] ?: "[]"
                val type = object : TypeToken<MutableList<UserTicket>>() {}.type
                val tickets = gson.fromJson<MutableList<UserTicket>>(json, type) ?: mutableListOf()
                tickets.removeIf { it.id == ticketId }
                preferences[KEY_USER_TICKETS_SHUANGSEQIU] = gson.toJson(tickets)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 计算彩票中奖结果
     * @param ticket 彩票
     * @param period 期号
     * @return 中奖结果描述
     */
    suspend fun calculatePrize(ticket: UserTicket, period: String): String? {
        // 构建彩票号码格式：前区号码@后区号码
        val frontNumbers = ticket.frontNumbers.joinToString(",")
        val backNumbers = ticket.backNumbers.joinToString(",")
        val ticketNumbers = "$frontNumbers@$backNumbers"
        
        return apiService.calculatePrize(ticket.lotteryType, period, ticketNumbers)
    }
}