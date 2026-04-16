package com.example.lotterycalculator.ui.calculation

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import com.example.lotterycalculator.R
import com.example.lotterycalculator.data.model.LotteryResult

class CalculationResultsDialogFragment(
    private val calculationResults: LiveData<List<LotteryResult>>? = null,
    private val initialResults: List<LotteryResult>? = null
) : DialogFragment() {

    private var dialogView: android.view.View? = null
    private var resultsText: TextView? = null
    private var hasReceivedResult = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("计算结果")
            .setPositiveButton("确定") { _, _ ->
                dismiss()
            }

        val inflater = LayoutInflater.from(requireContext())
        dialogView = inflater.inflate(R.layout.dialog_calculation_results, null)
        resultsText = dialogView?.findViewById<TextView>(R.id.resultsText)
        
        // 如果有初始结果，直接显示
        if (initialResults != null && initialResults.isNotEmpty()) {
            hasReceivedResult = true
            resultsText?.text = formatResultsWithColors(initialResults)
        } else {
            resultsText?.text = "加载中..."
        }
        
        builder.setView(dialogView)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        // 如果有LiveData，观察它的变化
        calculationResults?.observe(this) { results ->
            if (results.isNotEmpty()) {
                hasReceivedResult = true
                resultsText?.text = formatResultsWithColors(results)
            } else if (!hasReceivedResult) {
                // 还没有收到过结果，保持显示"加载中..."
            }
        }
    }

    /**
     * 更新中奖结果显示
     * @param newResults 新的中奖结果列表
     */
    fun updateResults(newResults: List<LotteryResult>) {
        if (newResults.isNotEmpty()) {
            resultsText?.text = formatResultsWithColors(newResults)
        }
    }

    private fun formatResultsWithColors(results: List<LotteryResult>): CharSequence {
        if (results.isEmpty()) {
            return "暂无计算结果"
        }

        val spannableString = SpannableStringBuilder()
        results.forEachIndexed { index, result ->
            val ticketName = result.ticket.name ?: "彩票 ${index + 1}"
            spannableString.append("$ticketName:\n")
            spannableString.append("前区: ${result.ticket.frontNumbers.joinToString(", ")}\n")
            spannableString.append("后区: ${result.ticket.backNumbers.joinToString(", ")}\n")
            spannableString.append("匹配前区: ${result.matchedFrontNumbers.joinToString(", ")}\n")
            spannableString.append("匹配后区: ${result.matchedBackNumbers.joinToString(", ")}\n")
            
            val prizeLevelText = "中奖等级: ${result.prizeLevel.description}\n"
            val startIndex = spannableString.length
            spannableString.append(prizeLevelText)
            val endIndex = spannableString.length
            
            val color = getPrizeLevelColor(result.prizeLevel)
            spannableString.setSpan(
                ForegroundColorSpan(color),
                startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            if (!result.prizeDescription.isNullOrEmpty()) {
                spannableString.append("中奖信息: ${result.prizeDescription}\n")
            }
            
            if (result.prizeAmount != null) {
                spannableString.append("奖金: ${result.prizeAmount}元\n")
            }
            spannableString.append("\n")
        }

        return spannableString
    }

    private fun getPrizeLevelColor(prizeLevel: LotteryResult.PrizeLevel): Int {
        return when (prizeLevel) {
            LotteryResult.PrizeLevel.FIRST -> Color.parseColor("#FF0000")
            LotteryResult.PrizeLevel.SECOND -> Color.parseColor("#FF4500")
            LotteryResult.PrizeLevel.THIRD -> Color.parseColor("#FFA500")
            LotteryResult.PrizeLevel.FOURTH -> Color.parseColor("#FFCC00")
            LotteryResult.PrizeLevel.FIFTH -> Color.parseColor("#FFFF00")
            LotteryResult.PrizeLevel.SIXTH -> Color.parseColor("#ADFF2F")
            LotteryResult.PrizeLevel.SEVENTH -> Color.parseColor("#00FF00")
            LotteryResult.PrizeLevel.EIGHTH -> Color.parseColor("#00FA9A")
            LotteryResult.PrizeLevel.NINTH -> Color.parseColor("#6699CC")
            LotteryResult.PrizeLevel.NO_PRIZE -> Color.parseColor("#808080")
        }
    }
}