package com.example.lotterycalculator.data.model

import java.util.Date
import java.util.UUID

data class UserTicket(
    val id: String = UUID.randomUUID().toString(),
    val lotteryType: LotteryType,
    val name: String? = null,
    val frontNumbers: List<Int>,
    val backNumbers: List<Int>,
    val createdAt: Date = Date(),
    val isCombination: Boolean = false,
    val frontCombinationCount: Int = if (isCombination) frontNumbers.size else 0,
    val backCombinationCount: Int = if (isCombination) backNumbers.size else 0
) {
    val isValid: Boolean
        get() {
            val frontRange = DrawResult.getFrontNumberRange(lotteryType)
            val backRange = DrawResult.getBackNumberRange(lotteryType)
            val frontCount = if (isCombination) frontCombinationCount else DrawResult.getFrontNumberCount(lotteryType)
            val backCount = if (isCombination) backCombinationCount else DrawResult.getBackNumberCount(lotteryType)

            return frontNumbers.size >= frontCount &&
                    backNumbers.size >= backCount &&
                    frontNumbers.all { it in frontRange } &&
                    backNumbers.all { it in backRange } &&
                    frontNumbers.distinct().size == frontNumbers.size &&
                    backNumbers.distinct().size == backNumbers.size
        }

    fun getTicketCount(): Int {
        if (!isCombination) return 1

        val frontCount = DrawResult.getFrontNumberCount(lotteryType)
        val backCount = DrawResult.getBackNumberCount(lotteryType)

        return combination(frontNumbers.size, frontCount) * combination(backNumbers.size, backCount)
    }

    private fun combination(n: Int, k: Int): Int {
        if (k > n) return 0
        if (k == 0 || k == n) return 1

        var result = 1
        for (i in 1..k) {
            result = result * (n - k + i) / i
        }
        return result
    }

    fun generateAllCombinations(): List<UserTicket> {
        if (!isCombination) return listOf(this)

        val frontCount = DrawResult.getFrontNumberCount(lotteryType)
        val backCount = DrawResult.getBackNumberCount(lotteryType)

        val frontCombinations = combinations(frontNumbers, frontCount)
        val backCombinations = combinations(backNumbers, backCount)

        return frontCombinations.flatMap { frontCombo ->
            backCombinations.map { backCombo ->
                UserTicket(
                    lotteryType = lotteryType,
                    name = name,
                    frontNumbers = frontCombo,
                    backNumbers = backCombo,
                    createdAt = createdAt,
                    isCombination = false
                )
            }
        }
    }

    private fun combinations(numbers: List<Int>, k: Int): List<List<Int>> {
        val result = mutableListOf<List<Int>>()
        val temp = mutableListOf<Int>()

        fun backtrack(start: Int) {
            if (temp.size == k) {
                result.add(temp.toList())
                return
            }
            for (i in start until numbers.size) {
                temp.add(numbers[i])
                backtrack(i + 1)
                temp.removeAt(temp.size - 1)
            }
        }

        backtrack(0)
        return result
    }
}