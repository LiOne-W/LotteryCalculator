package com.example.lotterycalculator.ui.display

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lotterycalculator.R
import com.example.lotterycalculator.data.model.DrawResult
import com.example.lotterycalculator.data.model.LotteryType
import com.example.lotterycalculator.databinding.FragmentHistoryDisplayBinding
import com.example.lotterycalculator.viewmodel.DisplayViewModel

class HistoryDisplayFragment : Fragment() {
    private var _binding: FragmentHistoryDisplayBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DisplayViewModel
    private var lotteryType: LotteryType = LotteryType.DALETOU

    companion object {
        private const val ARG_LOTTERY_TYPE = "lottery_type"

        fun newInstance(lotteryType: LotteryType): HistoryDisplayFragment {
            val fragment = HistoryDisplayFragment()
            val args = Bundle()
            args.putSerializable(ARG_LOTTERY_TYPE, lotteryType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            lotteryType = it.getSerializable(ARG_LOTTERY_TYPE) as LotteryType
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DisplayViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        loadHistoryData()
        setupObservers()
    }

    private fun setupToolbar() {
        val title = when (lotteryType) {
            LotteryType.DALETOU -> "大乐透历史开奖"
            LotteryType.SHUANGSEQIU -> "双色球历史开奖"
        }
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadHistoryData() {
        viewModel.setCurrentLotteryType(lotteryType)
        // 加载最近30期数据
        viewModel.loadHistoryDrawResults()
    }

    private fun setupObservers() {
        viewModel.historyDrawResults.observe(viewLifecycleOwner) { results ->
            val adapter = HistoryAdapter(results)
            binding.historyRecyclerView.adapter = adapter
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.errorTextView.text = it
                binding.errorTextView.visibility = View.VISIBLE
            } ?: run {
                binding.errorTextView.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class HistoryAdapter(private val results: List<DrawResult>) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val result = results[position]
            holder.periodView.text = result.period
            val frontNumbersFormatted = result.frontNumbers.joinToString(", ") { num -> "%02d".format(num) }
            val backNumbersFormatted = result.backNumbers.joinToString(", ") { num -> "%02d".format(num) }
            val numbers = "$frontNumbersFormatted + $backNumbersFormatted"
            holder.numbersView.text = numbers
            
            // 设置文本颜色 - 使用主题属性
            val typedArray = holder.itemView.context.obtainStyledAttributes(
                intArrayOf(
                    android.R.attr.textColorPrimary,
                    android.R.attr.textColorSecondary,
                    android.R.attr.colorBackground
                )
            )
            
            val textColorPrimary = typedArray.getColor(0, ContextCompat.getColor(holder.itemView.context, android.R.color.black))
            val textColorSecondary = typedArray.getColor(1, ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
            val backgroundColor = typedArray.getColor(2, ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            
            typedArray.recycle()
            
            holder.periodView.setTextColor(textColorPrimary)
            holder.numbersView.setTextColor(textColorSecondary)
            
            // 设置列表项背景色
            holder.itemView.setBackgroundColor(backgroundColor)
        }

        override fun getItemCount(): Int = results.size

        inner class ViewHolder(itemView: View) : 
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val periodView: android.widget.TextView = itemView.findViewById(android.R.id.text1)
            val numbersView: android.widget.TextView = itemView.findViewById(android.R.id.text2)
        }
    }
}