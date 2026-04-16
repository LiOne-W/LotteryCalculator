package com.example.lotterycalculator.ui.calculation

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lotterycalculator.R
import com.example.lotterycalculator.data.model.DrawResult
import com.example.lotterycalculator.data.model.UserTicket
import com.example.lotterycalculator.databinding.ItemTicketBinding

class TicketAdapter(
    private val onDeleteClick: (UserTicket) -> Unit,
    private val onNameClick: (UserTicket) -> Unit,
    private val onCalculateClick: (UserTicket) -> Unit
) : ListAdapter<UserTicket, TicketAdapter.TicketViewHolder>(TicketDiffCallback()) {

    private var currentDrawResult: DrawResult? = null

    fun setDrawResult(drawResult: DrawResult?) {
        currentDrawResult = drawResult
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = getItem(position)
        holder.bind(ticket, currentDrawResult)
    }

    inner class TicketViewHolder(private val binding: ItemTicketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: UserTicket, drawResult: DrawResult?) {
            binding.ticketNameTextView.text = ticket.name ?: "未命名彩票"
            binding.ticketNameTextView.setOnClickListener {
                onNameClick(ticket)
            }
            
            // 设置带颜色的前区数字
            binding.frontNumbersTextView.text = createColoredNumbersText(
                numbers = ticket.frontNumbers,
                drawResultNumbers = drawResult?.frontNumbers ?: emptyList(),
                isFrontArea = true,
                context = binding.root.context
            )
            
            // 设置带颜色的后区数字
            binding.backNumbersTextView.text = createColoredNumbersText(
                numbers = ticket.backNumbers,
                drawResultNumbers = drawResult?.backNumbers ?: emptyList(),
                isFrontArea = false,
                context = binding.root.context
            )
            
            if (ticket.isCombination) {
                binding.combinationBadge.visibility = View.VISIBLE
                binding.ticketCountTextView.text = "共${ticket.getTicketCount()}注"
                binding.ticketCountTextView.visibility = View.VISIBLE
            } else {
                binding.combinationBadge.visibility = View.GONE
                binding.ticketCountTextView.visibility = View.GONE
            }

            if (drawResult != null) {
                val matchedFront = ticket.frontNumbers.intersect(drawResult.frontNumbers.toSet()).size
                val matchedBack = ticket.backNumbers.intersect(drawResult.backNumbers.toSet()).size
                binding.matchInfoTextView.text = "匹配: 前区${matchedFront}个, 后区${matchedBack}个"
                binding.matchInfoTextView.visibility = View.VISIBLE
            } else {
                binding.matchInfoTextView.visibility = View.GONE
            }

            binding.calculateButton.setOnClickListener {
                onCalculateClick(ticket)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(ticket)
            }
        }
        
        private fun createColoredNumbersText(
            numbers: List<Int>,
            drawResultNumbers: List<Int>,
            isFrontArea: Boolean,
            context: android.content.Context
        ): CharSequence {
            if (numbers.isEmpty()) {
                return ""
            }
            
            val numbersText = numbers.joinToString(", ")
            val spannableString = SpannableString(numbersText)
            
            var currentPosition = 0
            numbers.forEachIndexed { index, number ->
                val numberStr = number.toString()
                val startIndex = currentPosition
                val endIndex = startIndex + numberStr.length
                
                // 检查这个数字是否在中奖号码中
                val isMatched = drawResultNumbers.contains(number)
                
                // 设置颜色：选中（匹配）的用黄色，未选中的用红色（前区）或蓝色（后区）
                val colorRes = when {
                    isMatched -> R.color.lottery_yellow
                    isFrontArea -> R.color.lottery_red
                    else -> R.color.lottery_blue
                }
                
                val color = ContextCompat.getColor(context, colorRes)
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    startIndex,
                    endIndex,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // 更新当前位置，加上数字长度和逗号/空格
                currentPosition = endIndex
                if (index < numbers.size - 1) {
                    // 跳过逗号和空格
                    currentPosition += 2 // ", "的长度
                }
            }
            
            return spannableString
        }
    }
}

class TicketDiffCallback : DiffUtil.ItemCallback<UserTicket>() {
    override fun areItemsTheSame(oldItem: UserTicket, newItem: UserTicket): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UserTicket, newItem: UserTicket): Boolean {
        return oldItem == newItem
    }
}