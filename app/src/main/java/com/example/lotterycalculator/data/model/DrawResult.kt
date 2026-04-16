package com.example.lotterycalculator.data.model

import java.util.Date

data class DrawResult(
    val id: String,
    val lotteryType: LotteryType,
    val period: String,
    val drawDate: Date,
    val frontNumbers: List<Int>,
    val backNumbers: List<Int>,
    val prizePool: Long? = null,
    val sales: Long? = null,
    val jackpotWinners: Int? = null,
    val jackpotPrize: Long? = null
) {
    companion object {
        fun getDefaultFrontNumbers(lotteryType: LotteryType): List<Int> {
            return when (lotteryType) {
                LotteryType.DALETOU -> listOf(1, 2, 3, 4, 5)
                LotteryType.SHUANGSEQIU -> listOf(1, 2, 3, 4, 5, 6)
            }
        }

        fun getDefaultBackNumbers(lotteryType: LotteryType): List<Int> {
            return when (lotteryType) {
                LotteryType.DALETOU -> listOf(1, 2)
                LotteryType.SHUANGSEQIU -> listOf(1)
            }
        }

        fun getFrontNumberRange(lotteryType: LotteryType): IntRange {
            return when (lotteryType) {
                LotteryType.DALETOU -> 1..35
                LotteryType.SHUANGSEQIU -> 1..33
            }
        }

        fun getBackNumberRange(lotteryType: LotteryType): IntRange {
            return when (lotteryType) {
                LotteryType.DALETOU -> 1..12
                LotteryType.SHUANGSEQIU -> 1..16
            }
        }

        fun getFrontNumberCount(lotteryType: LotteryType): Int {
            return when (lotteryType) {
                LotteryType.DALETOU -> 5
                LotteryType.SHUANGSEQIU -> 6
            }
        }

        fun getBackNumberCount(lotteryType: LotteryType): Int {
            return when (lotteryType) {
                LotteryType.DALETOU -> 2
                LotteryType.SHUANGSEQIU -> 1
            }
        }
    }
}