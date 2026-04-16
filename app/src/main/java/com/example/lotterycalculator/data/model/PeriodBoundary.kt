package com.example.lotterycalculator.data.model

/**
 * 彩票期号边界数据类
 * 用于记录每种彩票类型的最新一期和最早期号
 * @property latestPeriod 最新一期期号
 * @property earliestPeriod 最早一期期号
 */
data class PeriodBoundary(
    val lotteryType: LotteryType,
    val latestPeriod: String,
    val earliestPeriod: String
) {
    /**
     * 判断给定期号是否是最新一期
     */
    /**
     * 将期号字符串转换为整数，如果转换失败返回null
     */
    private fun periodToInt(period: String): Int? {
        return try {
            period.toInt()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * 比较两个期号的大小
     * @return -1 if a < b, 0 if a == b, 1 if a > b
     */
    private fun comparePeriods(a: String, b: String): Int? {
        val aInt = periodToInt(a)
        val bInt = periodToInt(b)
        return if (aInt != null && bInt != null) {
            when {
                aInt < bInt -> -1
                aInt > bInt -> 1
                else -> 0
            }
        } else {
            // 如果无法转换为数字，回退到字符串比较
            when {
                a < b -> -1
                a > b -> 1
                else -> 0
            }
        }
    }
    
    fun isLatestPeriod(period: String): Boolean {
        val comparison = comparePeriods(period, latestPeriod)
        return comparison != null && comparison >= 0  // period >= latestPeriod
    }
    
    /**
     * 判断给定期号是否是最早一期
     */
    fun isEarliestPeriod(period: String): Boolean {
        val comparison = comparePeriods(period, earliestPeriod)
        return comparison != null && comparison <= 0  // period <= earliestPeriod
    }
    
    /**
     * 判断给定期号是否有上一期（即不是最早一期）
     */
    fun hasPreviousPeriod(period: String): Boolean {
        val comparison = comparePeriods(period, earliestPeriod)
        return comparison != null && comparison > 0  // period > earliestPeriod
    }
    
    /**
     * 判断给定期号是否有下一期（即不是最新一期）
     */
    fun hasNextPeriod(period: String): Boolean {
        val comparison = comparePeriods(period, latestPeriod)
        return comparison != null && comparison < 0  // period < latestPeriod
    }
    
    /**
     * 判断给定期号是否在边界范围内（包含边界）
     * @param period 要检查的期号
     * @return true 如果期号在最早和最早期号之间（包含边界）
     */
    fun isWithinBoundaries(period: String): Boolean {
        val compareWithEarliest = comparePeriods(period, earliestPeriod)
        val compareWithLatest = comparePeriods(period, latestPeriod)
        return compareWithEarliest != null && compareWithLatest != null &&
               compareWithEarliest >= 0 && compareWithLatest <= 0  // earliestPeriod <= period <= latestPeriod
    }
    
    /**
     * 判断给定期号是否等于最早期号
     */
    fun isAtEarliest(period: String): Boolean {
        val comparison = comparePeriods(period, earliestPeriod)
        return comparison != null && comparison == 0  // period == earliestPeriod
    }
    
    /**
     * 判断给定期号是否等于最新期号
     */
    fun isAtLatest(period: String): Boolean {
        val comparison = comparePeriods(period, latestPeriod)
        return comparison != null && comparison == 0  // period == latestPeriod
    }
    
    companion object {
        /**
         * 根据最新一期期号计算最早一期期号（向前推29期）
         * 注意：期号必须是数字字符串才能进行此计算
         * @param latestPeriod 最新一期期号
         * @return 最早一期期号
         */
        fun calculateEarliestPeriod(latestPeriod: String): String {
            return try {
                // 尝试将期号解析为整数
                val latest = latestPeriod.toInt()
                
                // 获取末尾3位数字（期数）
                val periodNumber = latest % 1000
                
                // 如果期数小于等于30，则最早一期到****001
                if (periodNumber <= 30) {
                    // 计算年份部分：去掉末尾3位，再加1（回到年份起始）
                    val yearPart = (latest / 1000) * 1000
                    (yearPart + 1).toString()
                } else {
                    // 正常减29
                    (latest - 29).toString()
                }
            } catch (e: NumberFormatException) {
                // 如果期号不是纯数字，无法计算，返回空字符串
                ""
            }
        }
        
        /**
         * 根据彩票类型获取每年最大期数
         * 注意：这是估计值，实际可能略有变化
         * @param lotteryType 彩票类型
         * @return 每年最大期数
         */
        private fun getMaxPeriodsPerYear(lotteryType: LotteryType): Int {
            return when (lotteryType) {
                LotteryType.DALETOU -> 151  // 大乐透每年约151期
                LotteryType.SHUANGSEQIU -> 153  // 双色球每年约153期
            }
        }
        
        /**
         * 计算上一期期号
         * @param currentPeriod 当前期号
         * @param lotteryType 彩票类型
         * @return 上一期期号，如果无法计算则返回null
         */
        fun calculatePreviousPeriod(currentPeriod: String, lotteryType: LotteryType): String? {
            return try {
                val current = currentPeriod.toInt()
                val periodNumber = current % 1000  // 末尾3位
                val yearPart = (current / 1000) * 1000  // 年份部分
                
                if (periodNumber > 1) {
                    // 同一年的上一期
                    (current - 1).toString()
                } else {
                    // 期号为001，需要跨年
                    val prevYear = (yearPart / 1000) - 1  // 前一年年份
                    val maxPeriods = getMaxPeriodsPerYear(lotteryType)
                    // 前一年最后一期
                    val prevYearLastPeriod = (prevYear * 1000) + maxPeriods
                    prevYearLastPeriod.toString()
                }
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        /**
         * 计算下一期期号
         * @param currentPeriod 当前期号
         * @param lotteryType 彩票类型
         * @return 下一期期号，如果无法计算则返回null
         */
        fun calculateNextPeriod(currentPeriod: String, lotteryType: LotteryType): String? {
            return try {
                val current = currentPeriod.toInt()
                val periodNumber = current % 1000  // 末尾3位
                val yearPart = (current / 1000) * 1000  // 年份部分
                val maxPeriods = getMaxPeriodsPerYear(lotteryType)
                
                if (periodNumber < maxPeriods) {
                    // 同一年的下一期
                    (current + 1).toString()
                } else {
                    // 当前是最后一年最后一期，下一期是下一年第一期
                    val nextYear = (yearPart / 1000) + 1  // 下一年年份
                    (nextYear * 1000 + 1).toString()  // 下一年第一期
                }
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        /**
         * 从历史数据列表创建边界
         * @param historicalResults 历史数据列表（可以是任意顺序）
         * @param lotteryType 彩票类型
         */
        fun fromHistoricalResults(historicalResults: List<DrawResult>, lotteryType: LotteryType): PeriodBoundary? {
            if (historicalResults.isEmpty()) {
                return null
            }
            
            // 找出最新一期（最大期号）
            val latestPeriod = historicalResults.maxByOrNull { it.period }?.period ?: historicalResults.first().period
            // 使用计算出的最早一期
            val earliestPeriod = calculateEarliestPeriod(latestPeriod)
            
            if (earliestPeriod.isEmpty()) {
                // 计算失败，回退到历史数据中的最早期号
                val earliestInHistory = historicalResults.minByOrNull { it.period }?.period ?: historicalResults.first().period
                return PeriodBoundary(
                    lotteryType = lotteryType,
                    latestPeriod = latestPeriod,
                    earliestPeriod = earliestInHistory
                )
            }
            
            return PeriodBoundary(
                lotteryType = lotteryType,
                latestPeriod = latestPeriod,
                earliestPeriod = earliestPeriod
            )
        }
    }
}