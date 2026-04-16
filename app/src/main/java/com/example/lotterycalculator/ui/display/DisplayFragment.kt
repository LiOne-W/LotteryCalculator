package com.example.lotterycalculator.ui.display

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.lotterycalculator.data.model.DrawResult
import com.example.lotterycalculator.data.model.LotteryType
import com.example.lotterycalculator.databinding.FragmentDisplayBinding
import com.example.lotterycalculator.viewmodel.DisplayViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class DisplayFragment : Fragment() {
    private var _binding: FragmentDisplayBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DisplayViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 使用Activity作用域的ViewModel，确保数据在导航间保持
        viewModel = ViewModelProvider(requireActivity())[DisplayViewModel::class.java]

        setupCardClickListeners()
        setupObservers()
        setupRefreshLayout()
        // 分别调用两个独立函数，避免一个被丢弃的问题
        loadBothLatestResults()
    }

    private fun shouldLoadData(result: DrawResult?): Boolean {
        return result == null
    }

    private fun loadBothLatestResults() {
        Log.d("DisplayFragment", "开始加载最新开奖号码")
        
        // 检查大乐透是否需要加载
        val shouldLoadDaletou = shouldLoadData(viewModel.daletouLatestResult.value)
        // 检查双色球是否需要加载
        val shouldLoadShuangseqiu = shouldLoadData(viewModel.shuangseqiuLatestResult.value)
        
        if (!shouldLoadDaletou && !shouldLoadShuangseqiu) {
            Log.d("DisplayFragment", "数据已存在，跳过加载")
            binding.progressBar.visibility = View.GONE
            return
        }
        
        // 显示进度条
        binding.progressBar.visibility = View.VISIBLE
        
        // 使用顺序执行，确保不会超过API的QPS限制（普通会员QPS为1）
        viewLifecycleOwner.lifecycleScope.launch {
            if (shouldLoadDaletou) {
                Log.d("DisplayFragment", "顺序执行：开始加载大乐透")
                // 先加载大乐透
                try {
                    viewModel.loadDaletouLatest()
                } catch (e: Exception) {
                    Log.e("DisplayFragment", "大乐透加载异常", e)
                    e.printStackTrace()
                    // 即使大乐透加载失败，也继续执行
                }
            } else {
                Log.d("DisplayFragment", "大乐透数据已存在，跳过加载")
            }
            
            // 等待1.5秒，确保不会超过QPS限制
            if (shouldLoadDaletou && shouldLoadShuangseqiu) {
                Log.d("DisplayFragment", "等待1.5秒后加载双色球")
                delay(1500)
            }
            
            if (shouldLoadShuangseqiu) {
                Log.d("DisplayFragment", "顺序执行：开始加载双色球")
                // 然后加载双色球
                try {
                    viewModel.loadShuangseqiuLatest()
                } catch (e: Exception) {
                    Log.e("DisplayFragment", "双色球加载异常", e)
                    e.printStackTrace()
                    // 即使双色球加载失败，也继续执行
                }
            } else {
                Log.d("DisplayFragment", "双色球数据已存在，跳过加载")
            }
        }
        
        // 5秒后隐藏进度条（确保顺序执行有足够时间完成加载）
        viewLifecycleOwner.lifecycleScope.launch {
            delay(5000)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun setupCardClickListeners() {
        binding.daletouCard.setOnClickListener {
            navigateToHistory(LotteryType.DALETOU)
        }

        binding.shuangseqiuCard.setOnClickListener {
            navigateToHistory(LotteryType.SHUANGSEQIU)
        }
    }

    private fun setupRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshLatestResults()
        }
        
        // 设置刷新指示器颜色
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun refreshLatestResults() {
        Log.d("DisplayFragment", "刷新最新开奖号码")
        
        // 在下拉刷新开始时，重置加载状态
        viewModel.resetLoadingStatus()
        
        // 隐藏进度条（如果有）
        binding.progressBar.visibility = View.GONE

        // 在下拉刷新开始时，将数据显示为00，表示正在更新
        binding.daletouPeriod.text = "加载中..."
        binding.shuangseqiuPeriod.text = "加载中..."
        
        // 大乐透：5个前区 + 2个后区
        val daletouFrontZeros = List(5) { "00" }.joinToString(", ")
        val daletouBackZeros = List(2) { "00" }.joinToString(", ")
        binding.daletouNumbers.text = "$daletouFrontZeros + $daletouBackZeros"
        
        // 双色球：6个前区 + 1个后区
        val shuangseqiuFrontZeros = List(6) { "00" }.joinToString(", ")
        val shuangseqiuBackZeros = "00"
        binding.shuangseqiuNumbers.text = "$shuangseqiuFrontZeros + $shuangseqiuBackZeros"
        
        // 使用顺序执行，确保不会超过API的QPS限制（普通会员QPS为1）
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("DisplayFragment", "刷新 - 顺序执行：开始加载大乐透")
            // 先加载大乐透
            try {
                viewModel.loadDaletouLatest()
            } catch (e: Exception) {
                Log.e("DisplayFragment", "刷新 - 大乐透加载异常", e)
                e.printStackTrace()
                // 即使大乐透加载失败，也继续执行
            }
            
            // 等待1.5秒，确保不会超过QPS限制
            Log.d("DisplayFragment", "刷新 - 等待1.5秒后加载双色球")
            delay(1500)
            
            // 然后加载双色球
            Log.d("DisplayFragment", "刷新 - 顺序执行：开始加载双色球")
            try {
                viewModel.loadShuangseqiuLatest()
            } catch (e: Exception) {
                Log.e("DisplayFragment", "刷新 - 双色球加载异常", e)
                e.printStackTrace()
                // 即使双色球加载失败，也继续执行
            }
        }
        
        // 4秒后停止刷新动画（确保顺序执行有足够时间完成）
        viewLifecycleOwner.lifecycleScope.launch {
            delay(4000)
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun navigateToHistory(lotteryType: LotteryType) {
        val fragment = HistoryDisplayFragment.newInstance(lotteryType)
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupObservers() {
        viewModel.daletouLatestResult.observe(viewLifecycleOwner) { result: DrawResult? ->
            if (result != null) {
                binding.daletouPeriod.text = result.period
                val frontNumbersFormatted = result.frontNumbers.joinToString(", ") { num -> "%02d".format(num) }
                val backNumbersFormatted = result.backNumbers.joinToString(", ") { num -> "%02d".format(num) }
                val numbers = "$frontNumbersFormatted + $backNumbersFormatted"
                binding.daletouNumbers.text = numbers
            } else {
                binding.daletouPeriod.text = "null"
                val daletouFrontZeros = List(5) { "00" }.joinToString(", ")
                val daletouBackZeros = List(2) { "00" }.joinToString(", ")
                binding.daletouNumbers.text = "$daletouFrontZeros + $daletouBackZeros"
            }
        }

        viewModel.shuangseqiuLatestResult.observe(viewLifecycleOwner) { result: DrawResult? ->
            if (result != null) {
                binding.shuangseqiuPeriod.text = result.period
                val frontNumbersFormatted = result.frontNumbers.joinToString(", ") { num -> "%02d".format(num) }
                val backNumbersFormatted = result.backNumbers.joinToString(", ") { num -> "%02d".format(num) }
                val numbers = "$frontNumbersFormatted + $backNumbersFormatted"
                binding.shuangseqiuNumbers.text = numbers
            } else {
                binding.shuangseqiuPeriod.text = "null"
                val shuangseqiuFrontZeros = List(6) { "00" }.joinToString(", ")
                val shuangseqiuBackZeros = "00"
                binding.shuangseqiuNumbers.text = "$shuangseqiuFrontZeros + $shuangseqiuBackZeros"
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading: Boolean ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error: String? ->
            // 根据用户要求，去掉底部错误提示
            // error?.let {
            //     binding.errorTextView.text = it
            //     binding.errorTextView.visibility = View.VISIBLE
            // } ?: run {
            //     binding.errorTextView.visibility = View.GONE
            // }
            // 直接隐藏错误提示
            binding.errorTextView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}