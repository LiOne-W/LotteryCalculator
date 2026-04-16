package com.example.lotterycalculator.network

import com.example.lotterycalculator.data.model.DrawResult
import com.example.lotterycalculator.data.model.LotteryType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.Date

class LotteryScraperService {
    
    suspend fun getLatestDrawResult(lotteryType: LotteryType): DrawResult? {
        val url = when (lotteryType) {
            LotteryType.DALETOU -> "https://www.zhcw.com/kjxx/dlt/"
            LotteryType.SHUANGSEQIU -> "https://www.zhcw.com/kjxx/ssq/"
        }
        
        return try {
            val doc = Jsoup.connect(url).get()
            parseLatestResult(doc, lotteryType)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getHistoryDrawResults(lotteryType: LotteryType, count: Int): List<DrawResult> {
        val url = when (lotteryType) {
            LotteryType.DALETOU -> "https://www.zhcw.com/kjxx/dlt/"
            LotteryType.SHUANGSEQIU -> "https://www.zhcw.com/kjxx/ssq/"
        }
        
        return try {
            val doc = Jsoup.connect(url).get()
            parseHistoryResults(doc, lotteryType, count)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun parseLatestResult(doc: Document, lotteryType: LotteryType): DrawResult? {
        // 解析最新一期开奖结果
        // 尝试不同的选择器来适配网站结构
        var latestResultElement = doc.selectFirst(".kjlist_box table tbody tr:first-child")
        if (latestResultElement == null) {
            // 尝试其他可能的选择器
            latestResultElement = doc.selectFirst("table.kjlist_table tbody tr:first-child")
        }
        if (latestResultElement == null) {
            latestResultElement = doc.selectFirst("table tbody tr:first-child")
        }
        if (latestResultElement == null) return null
        
        // 尝试不同的列位置来获取期号
        var period = latestResultElement.selectFirst("td:nth-child(1)")?.text()
        if (period.isNullOrEmpty()) {
            period = latestResultElement.selectFirst("td:nth-child(2)")?.text()
        }
        if (period.isNullOrEmpty()) return null
        
        // 尝试不同的列位置来获取号码
        var numbersElement = latestResultElement.selectFirst("td:nth-child(3)")
        if (numbersElement == null) {
            numbersElement = latestResultElement.selectFirst("td:nth-child(4)")
        }
        if (numbersElement == null) {
            numbersElement = latestResultElement.selectFirst("td:nth-child(2)")
        }
        if (numbersElement == null) return null
        
        // 提取号码，处理不同的分隔符
        val numbersText = numbersElement.text()
        val numbers = numbersText
            .split(" ", "-", "+", ",")
            .filter { it.isNotEmpty() && it.all { char -> char.isDigit() } }
        
        val frontNumbers = mutableListOf<Int>()
        val backNumbers = mutableListOf<Int>()
        
        when (lotteryType) {
            LotteryType.DALETOU -> {
                // 大乐透：前5个号码，后2个号码
                for (i in 0 until 5) {
                    if (i < numbers.size) {
                        numbers[i].toIntOrNull()?.let { frontNumbers.add(it) }
                    }
                }
                for (i in 5 until 7) {
                    if (i < numbers.size) {
                        numbers[i].toIntOrNull()?.let { backNumbers.add(it) }
                    }
                }
            }
            LotteryType.SHUANGSEQIU -> {
                // 双色球：前6个号码，后1个号码
                for (i in 0 until 6) {
                    if (i < numbers.size) {
                        numbers[i].toIntOrNull()?.let { frontNumbers.add(it) }
                    }
                }
                for (i in 6 until 7) {
                    if (i < numbers.size) {
                        numbers[i].toIntOrNull()?.let { backNumbers.add(it) }
                    }
                }
            }
        }
        
        // 确保获取到足够的号码
        if (frontNumbers.isEmpty()) return null
        
        return DrawResult(
            id = period,
            lotteryType = lotteryType,
            period = period,
            drawDate = Date(),
            frontNumbers = frontNumbers,
            backNumbers = backNumbers
        )
    }
    
    private fun parseHistoryResults(doc: Document, lotteryType: LotteryType, count: Int): List<DrawResult> {
        val results = mutableListOf<DrawResult>()
        
        // 解析历史开奖结果
        var historyElements = doc.select(".kjlist_box table tbody tr")
        if (historyElements.isEmpty()) {
            historyElements = doc.select("table.kjlist_table tbody tr")
        }
        if (historyElements.isEmpty()) {
            historyElements = doc.select("table tbody tr")
        }
        
        for (i in 0 until minOf(count, historyElements.size)) {
            val element = historyElements[i]
            
            // 尝试不同的列位置来获取期号
            var period = element.selectFirst("td:nth-child(1)")?.text()
            if (period.isNullOrEmpty()) {
                period = element.selectFirst("td:nth-child(2)")?.text()
            }
            if (period.isNullOrEmpty()) continue
            
            // 尝试不同的列位置来获取号码
            var numbersElement = element.selectFirst("td:nth-child(3)")
            if (numbersElement == null) {
                numbersElement = element.selectFirst("td:nth-child(4)")
            }
            if (numbersElement == null) {
                numbersElement = element.selectFirst("td:nth-child(2)")
            }
            if (numbersElement == null) continue
            
            // 提取号码，处理不同的分隔符
            val numbersText = numbersElement.text()
            val numbers = numbersText
                .split(" ", "-", "+", ",")
                .filter { it.isNotEmpty() && it.all { char -> char.isDigit() } }
            
            val frontNumbers = mutableListOf<Int>()
            val backNumbers = mutableListOf<Int>()
            
            when (lotteryType) {
                LotteryType.DALETOU -> {
                    for (j in 0 until 5) {
                        if (j < numbers.size) {
                            numbers[j].toIntOrNull()?.let { frontNumbers.add(it) }
                        }
                    }
                    for (j in 5 until 7) {
                        if (j < numbers.size) {
                            numbers[j].toIntOrNull()?.let { backNumbers.add(it) }
                        }
                    }
                }
                LotteryType.SHUANGSEQIU -> {
                    for (j in 0 until 6) {
                        if (j < numbers.size) {
                            numbers[j].toIntOrNull()?.let { frontNumbers.add(it) }
                        }
                    }
                    for (j in 6 until 7) {
                        if (j < numbers.size) {
                            numbers[j].toIntOrNull()?.let { backNumbers.add(it) }
                        }
                    }
                }
            }
            
            // 确保获取到足够的号码
            if (frontNumbers.isEmpty()) continue
            
            results.add(DrawResult(
                id = period,
                lotteryType = lotteryType,
                period = period,
                drawDate = Date(),
                frontNumbers = frontNumbers,
                backNumbers = backNumbers
            ))
        }
        
        return results
    }
    
    private fun minOf(a: Int, b: Int): Int {
        return if (a < b) a else b
    }
}