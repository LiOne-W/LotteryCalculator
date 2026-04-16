package com.example.lotterycalculator.ui.calculation

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.lotterycalculator.R
import com.example.lotterycalculator.data.model.LotteryType
import com.example.lotterycalculator.data.model.UserTicket
import com.example.lotterycalculator.databinding.DialogNumberPickerBinding
import com.google.android.material.chip.Chip
import java.util.*

class AddTicketDialogFragment(
    private val onSave: (UserTicket) -> Unit
) : DialogFragment() {

    private var _binding: DialogNumberPickerBinding? = null
    private val binding get() = _binding!!
    private var currentLotteryType: LotteryType = LotteryType.DALETOU
    private val selectedFrontNumbers = mutableSetOf<Int>()
    private val selectedBackNumbers = mutableSetOf<Int>()
    private var dialog: AlertDialog? = null

    fun setLotteryType(lotteryType: LotteryType) {
        currentLotteryType = lotteryType
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogNumberPickerBinding.inflate(LayoutInflater.from(requireContext()))

        val builder = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("添加彩票")
            .setPositiveButton("保存", null) // 先设置为null，稍后手动设置
            .setNegativeButton("取消") { _, _ ->
                dismiss()
            }

        val createdDialog = builder.create()
        dialog = createdDialog
        createdDialog.setOnShowListener {
            val saveButton = createdDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                saveTicket()
            }
            updateSaveButtonState(saveButton)
        }

        setupNumberChips()
        updateSelectionInfo()
        
        return createdDialog
    }

    private fun setupNumberChips() {
        binding.frontChipGroup.removeAllViews()
        binding.backChipGroup.removeAllViews()

        val frontRange = when (currentLotteryType) {
            LotteryType.DALETOU -> 1..35
            LotteryType.SHUANGSEQIU -> 1..33
        }
        val backRange = when (currentLotteryType) {
            LotteryType.DALETOU -> 1..12
            LotteryType.SHUANGSEQIU -> 1..16
        }

        for (i in frontRange) {
            val chip = Chip(requireContext()).apply {
                text = i.toString()
                isCheckable = true
                isChecked = selectedFrontNumbers.contains(i)
                updateChipStyle(this, isChecked)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedFrontNumbers.add(i)
                    } else {
                        selectedFrontNumbers.remove(i)
                    }
                    updateChipStyle(this, isChecked)
                    updateSelectionInfo()
                    updateSaveButtonState(dialog?.getButton(AlertDialog.BUTTON_POSITIVE))
                }
            }
            binding.frontChipGroup.addView(chip)
        }

        for (i in backRange) {
            val chip = Chip(requireContext()).apply {
                text = i.toString()
                isCheckable = true
                isChecked = selectedBackNumbers.contains(i)
                updateChipStyle(this, isChecked)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedBackNumbers.add(i)
                    } else {
                        selectedBackNumbers.remove(i)
                    }
                    updateChipStyle(this, isChecked)
                    updateSelectionInfo()
                    updateSaveButtonState(dialog?.getButton(AlertDialog.BUTTON_POSITIVE))
                }
            }
            binding.backChipGroup.addView(chip)
        }
    }
    
    private fun updateChipStyle(chip: Chip, isSelected: Boolean) {
        if (isSelected) {
            // 选中状态：亮色系底深色字
            chip.setChipBackgroundColorResource(R.color.lottery_blue)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            // 未选中状态：深色底亮色字
            chip.setChipBackgroundColorResource(R.color.dark_background)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }

    private fun updateSelectionInfo() {
        val frontMin = when (currentLotteryType) {
            LotteryType.DALETOU -> 5
            LotteryType.SHUANGSEQIU -> 6
        }
        val backMin = when (currentLotteryType) {
            LotteryType.DALETOU -> 2
            LotteryType.SHUANGSEQIU -> 1
        }

        val frontText = "前区: ${selectedFrontNumbers.size}/$frontMin"
        val backText = "后区: ${selectedBackNumbers.size}/$backMin"
        binding.selectionInfoTextView.text = "$frontText | $backText"
    }

    private fun updateSaveButtonState(button: android.widget.Button?) {
        if (button == null) return
        
        val frontMin = when (currentLotteryType) {
            LotteryType.DALETOU -> 5
            LotteryType.SHUANGSEQIU -> 6
        }
        val backMin = when (currentLotteryType) {
            LotteryType.DALETOU -> 2
            LotteryType.SHUANGSEQIU -> 1
        }

        val isValid = selectedFrontNumbers.size >= frontMin && selectedBackNumbers.size >= backMin
        button.isEnabled = isValid
    }

    private fun saveTicket() {
        val frontMin = when (currentLotteryType) {
            LotteryType.DALETOU -> 5
            LotteryType.SHUANGSEQIU -> 6
        }
        val backMin = when (currentLotteryType) {
            LotteryType.DALETOU -> 2
            LotteryType.SHUANGSEQIU -> 1
        }

        if (selectedFrontNumbers.size < frontMin || selectedBackNumbers.size < backMin) {
            Toast.makeText(requireContext(), "请选择足够的号码", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取彩票名称
        val ticketName = binding.ticketNameEditText.text.toString().trim()

        val ticket = UserTicket(
            lotteryType = currentLotteryType,
            name = if (ticketName.isEmpty()) null else ticketName,
            frontNumbers = selectedFrontNumbers.toList().sorted(),
            backNumbers = selectedBackNumbers.toList().sorted(),
            isCombination = false
        )

        if (ticket.isValid) {
            onSave(ticket)
            dismiss()
        } else {
            Toast.makeText(requireContext(), "号码无效，请检查号码范围和数量", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}