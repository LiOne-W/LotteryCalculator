package com.example.lotterycalculator.data.model

data class LotteryResult(
    val ticket: UserTicket,
    val drawResult: DrawResult,
    val matchedFrontNumbers: List<Int>,
    val matchedBackNumbers: List<Int>,
    val prizeLevel: PrizeLevel,
    val prizeAmount: Long? = null,
    val prizeDescription: String? = null
) {
    enum class PrizeLevel(val description: String) {
        FIRST("一等奖"),
        SECOND("二等奖"),
        THIRD("三等奖"),
        FOURTH("四等奖"),
        FIFTH("五等奖"),
        SIXTH("六等奖"),
        SEVENTH("七等奖"),
        EIGHTH("八等奖"),
        NINTH("九等奖"),
        NO_PRIZE("未中奖")
    }

    companion object {
        fun calculatePrize(ticket: UserTicket, drawResult: DrawResult, prizeDescription: String? = null): LotteryResult {
            val matchedFront = ticket.frontNumbers.intersect(drawResult.frontNumbers.toSet()).toList()
            val matchedBack = ticket.backNumbers.intersect(drawResult.backNumbers.toSet()).toList()

            val prizeLevel = when (ticket.lotteryType) {
                LotteryType.DALETOU -> calculateDaLETouPrize(
                    matchedFront.size,
                    matchedBack.size
                )
                LotteryType.SHUANGSEQIU -> calculateShuangSeQiuPrize(
                    matchedFront.size,
                    matchedBack.size
                )
            }

            return LotteryResult(
                ticket = ticket,
                drawResult = drawResult,
                matchedFrontNumbers = matchedFront,
                matchedBackNumbers = matchedBack,
                prizeLevel = prizeLevel,
                prizeDescription = prizeDescription
            )
        }

        private fun calculateDaLETouPrize(
            matchedFront: Int,
            matchedBack: Int
        ): PrizeLevel {
            return when {
                matchedFront == 5 && matchedBack == 2 -> PrizeLevel.FIRST
                matchedFront == 5 && matchedBack == 1 -> PrizeLevel.SECOND
                matchedFront == 5 && matchedBack == 0 -> PrizeLevel.THIRD
                matchedFront == 4 && matchedBack == 2 -> PrizeLevel.FOURTH
                matchedFront == 4 && matchedBack == 1 -> PrizeLevel.FIFTH
                matchedFront == 3 && matchedBack == 2 -> PrizeLevel.FIFTH
                matchedFront == 4 && matchedBack == 0 -> PrizeLevel.SIXTH
                matchedFront == 3 && matchedBack == 1 -> PrizeLevel.SIXTH
                matchedFront == 2 && matchedBack == 2 -> PrizeLevel.SIXTH
                matchedFront == 3 && matchedBack == 0 -> PrizeLevel.SEVENTH
                matchedFront == 2 && matchedBack == 1 -> PrizeLevel.SEVENTH
                matchedFront == 1 && matchedBack == 2 -> PrizeLevel.SEVENTH
                matchedFront == 0 && matchedBack == 2 -> PrizeLevel.SEVENTH
                matchedFront == 2 && matchedBack == 0 -> PrizeLevel.EIGHTH
                matchedFront == 1 && matchedBack == 1 -> PrizeLevel.EIGHTH
                matchedFront == 0 && matchedBack == 1 -> PrizeLevel.EIGHTH
                else -> PrizeLevel.NO_PRIZE
            }
        }

        private fun calculateShuangSeQiuPrize(
            matchedFront: Int,
            matchedBack: Int
        ): PrizeLevel {
            return when {
                matchedFront == 6 && matchedBack == 1 -> PrizeLevel.FIRST
                matchedFront == 6 && matchedBack == 0 -> PrizeLevel.SECOND
                matchedFront == 5 && matchedBack == 1 -> PrizeLevel.THIRD
                matchedFront == 5 && matchedBack == 0 -> PrizeLevel.FOURTH
                matchedFront == 4 && matchedBack == 1 -> PrizeLevel.FOURTH
                matchedFront == 4 && matchedBack == 0 -> PrizeLevel.FIFTH
                matchedFront == 3 && matchedBack == 1 -> PrizeLevel.FIFTH
                matchedFront == 2 && matchedBack == 1 -> PrizeLevel.SIXTH
                matchedFront == 1 && matchedBack == 1 -> PrizeLevel.SIXTH
                matchedFront == 0 && matchedBack == 1 -> PrizeLevel.SIXTH
                else -> PrizeLevel.NO_PRIZE
            }
        }
    }
}