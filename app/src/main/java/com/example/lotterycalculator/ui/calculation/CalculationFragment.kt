package com.example.lotterycalculator.ui.calculation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lotterycalculator.data.model.LotteryResult
import com.example.lotterycalculator.data.model.UserTicket
import com.example.lotterycalculator.databinding.FragmentCalculationBinding
import com.example.lotterycalculator.viewmodel.CalculationViewModel
import kotlinx.coroutines.launch

class CalculationFragment : Fragment() {
    private var _binding: FragmentCalculationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CalculationViewModel
    private lateinit var ticketAdapter: TicketAdapter
    
    private var lastPrevClickTime = 0L
    private var lastNextClickTime = 0L
    private val clickDelay = 1000L // 1秒防抖

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CalculationViewModel::class.java]
        
        // 清空之前的计算结果，避免进入时自动弹出弹窗
        viewModel.clearCalculationResults()

        setupTabLayout()
        setupRecyclerView()
        setupButtons()
        setupPeriodSelection()
        setupObservers()
        
        // 使用缓存机制加载数据，避免每次进入都刷新
        viewModel.setCurrentLotteryType(com.example.lotterycalculator.data.model.LotteryType.DALETOU)
    }

    private fun setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("大乐透"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("双色球"))

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.setCurrentLotteryType(com.example.lotterycalculator.data.model.LotteryType.DALETOU)
                    1 -> viewModel.setCurrentLotteryType(com.example.lotterycalculator.data.model.LotteryType.SHUANGSEQIU)
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        ticketAdapter = TicketAdapter(
            onDeleteClick = { ticket ->
                viewModel.deleteUserTicket(ticket.id)
            },
            onNameClick = { ticket ->
                showEditTicketNameDialog(ticket)
            },
            onCalculateClick = { ticket ->
                calculateSingleTicket(ticket)
            }
        )

        binding.ticketsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.ticketsRecyclerView.adapter = ticketAdapter
    }

    private fun setupButtons() {
        binding.addTicketButton.setOnClickListener {
            showAddTicketDialog()
        }

        binding.calculateAllButton.setOnClickListener {
            android.util.Log.d("CalculationFragment", "计算全部按钮被点击 - 启用状态: ${binding.calculateAllButton.isEnabled}")
            
            // 检查是否有用户彩票
            val hasTickets = viewModel.hasUserTickets.value ?: false
            val tickets = viewModel.userTickets.value ?: emptyList()
            android.util.Log.d("CalculationFragment", "是否有彩票: $hasTickets, 彩票数量: ${tickets.size}")
            
            if (!hasTickets || tickets.isEmpty()) {
                // 显示提示信息
                android.widget.Toast.makeText(requireContext(), "彩票不存在", android.widget.Toast.LENGTH_SHORT).show()
                android.util.Log.d("CalculationFragment", "❌ 没有彩票，显示提示并返回")
                return@setOnClickListener
            }
            
            // 有彩票，执行计算
            showLoadingDialog()
            viewModel.calculateAllTickets()
        }

        binding.clearAllButton.setOnClickListener {
            viewModel.clearAllTickets()
        }
        
        binding.prevPeriodButton.setOnClickListener {
            android.util.Log.d("CalculationFragment", "上一期按钮被点击 - 启用状态: ${binding.prevPeriodButton.isEnabled}")
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPrevClickTime > clickDelay) {
                lastPrevClickTime = currentTime
                android.util.Log.d("CalculationFragment", "✅ 上一期按钮点击通过防抖检查")
                viewModel.loadPreviousPeriod()
            } else {
                android.util.Log.d("CalculationFragment", "❌ 上一期按钮点击过快，忽略")
            }
        }
        
        binding.nextPeriodButton.setOnClickListener {
            android.util.Log.d("CalculationFragment", "下一期按钮被点击 - 启用状态: ${binding.nextPeriodButton.isEnabled}")
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNextClickTime > clickDelay) {
                lastNextClickTime = currentTime
                android.util.Log.d("CalculationFragment", "✅ 下一期按钮点击通过防抖检查")
                viewModel.loadNextPeriod()
            } else {
                android.util.Log.d("CalculationFragment", "❌ 下一期按钮点击过快，忽略")
            }
        }
    }

    private fun setupPeriodSelection() {
        binding.selectedPeriodTextView.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val historicalResults = viewModel.getHistoricalDrawResults()
                    android.util.Log.d("CalculationFragment", "获取历史数据数量: ${historicalResults.size}")
                    if (historicalResults.isNotEmpty()) {
                        android.util.Log.d("CalculationFragment", "原始顺序 - 第1期: ${historicalResults.first().period}, 最后1期: ${historicalResults.last().period}")
                    }
                    
                    // 将期号按降序排列（最新在上，最早在下）
                    val periods = historicalResults.map { it.period }.sortedByDescending { period ->
                        try {
                            period.toInt()
                        } catch (e: NumberFormatException) {
                            0
                        }
                    }
                    
                    android.util.Log.d("CalculationFragment", "排序后期号列表: ${periods.take(5)}... (共${periods.size}期)")
                    
                    // 在主线程中显示弹出菜单
                    val popupMenu = android.widget.PopupMenu(requireContext(), binding.selectedPeriodTextView)
                    periods.forEachIndexed { index, period ->
                        popupMenu.menu.add(0, index, 0, period)
                    }
                    popupMenu.setOnMenuItemClickListener { item ->
                        val selectedPeriod = periods[item.itemId]
                        android.util.Log.d("CalculationFragment", "选择期号: $selectedPeriod")
                        viewModel.loadDrawResult(selectedPeriod)
                        true
                    }
                    popupMenu.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 可以显示错误提示
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.userTickets.observe(viewLifecycleOwner) { tickets: List<UserTicket> ->
            ticketAdapter.submitList(tickets)
            binding.emptyTextView.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.calculationResults.observe(viewLifecycleOwner) { results: List<LotteryResult> ->
            // 检查是否有结果需要显示
            if (!results.isNullOrEmpty()) {
                // 检查是否正在计算中
                val isCalculating = viewModel.isCalculating.value ?: false
                if (!isCalculating) {
                    // 检查是否已存在结果对话框
                    val existingDialog = childFragmentManager.findFragmentByTag("CalculationResultsDialog") as? CalculationResultsDialogFragment
                    if (existingDialog == null) {
                        // 没有现有对话框，显示新的结果对话框
                        showCalculationResults(results)
                    } else {
                        // 已有对话框，更新其结果
                        existingDialog.updateResults(results)
                    }
                }
                // 如果正在计算中，稍后会通过isCalculating观察者显示结果
            }
        }

        viewModel.currentDrawResult.observe(viewLifecycleOwner) { drawResult ->
            if (drawResult != null) {
                binding.selectedPeriodTextView.text = "当前开奖期号: " + drawResult.period
                val frontNumbersText = drawResult.frontNumbers.joinToString(", ")
                val backNumbersText = drawResult.backNumbers.joinToString(", ")
                binding.drawNumbersTextView.text = "开奖号码: 前区 $frontNumbersText 后区 $backNumbersText"
            } else {
                // 当开奖结果为null时，清除显示
                binding.selectedPeriodTextView.text = "当前开奖期号: 加载中..."
                binding.drawNumbersTextView.text = "开奖号码: 等待加载"
            }
            ticketAdapter.setDrawResult(drawResult)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading: Boolean ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isCalculating.observe(viewLifecycleOwner) { isCalculating: Boolean ->
            if (!isCalculating) {
                dismissLoadingDialog()
                
                // 计算完成后，检查是否有结果需要显示
                val results = viewModel.calculationResults.value
                if (!results.isNullOrEmpty()) {
                    // 检查是否已存在结果对话框
                    val existingDialog = childFragmentManager.findFragmentByTag("CalculationResultsDialog") as? CalculationResultsDialogFragment
                    if (existingDialog == null) {
                        // 没有现有对话框，显示新的结果对话框
                        showCalculationResults(results)
                    } else {
                        // 已有对话框，更新其结果
                        existingDialog.updateResults(results)
                    }
                }
            }
        }

        viewModel.hasPreviousPeriod.observe(viewLifecycleOwner) { hasPrevious: Boolean ->
            android.util.Log.d("CalculationFragment", "上一期按钮状态更新: $hasPrevious")
            binding.prevPeriodButton.isEnabled = hasPrevious
            // 按钮变灰效果
            binding.prevPeriodButton.alpha = if (hasPrevious) 1.0f else 0.5f
            android.util.Log.d("CalculationFragment", "上一期按钮启用状态: ${binding.prevPeriodButton.isEnabled}, alpha: ${binding.prevPeriodButton.alpha}")
        }

        viewModel.hasNextPeriod.observe(viewLifecycleOwner) { hasNext: Boolean ->
            android.util.Log.d("CalculationFragment", "下一期按钮状态更新: $hasNext")
            binding.nextPeriodButton.isEnabled = hasNext
            // 按钮变灰效果
            binding.nextPeriodButton.alpha = if (hasNext) 1.0f else 0.5f
            android.util.Log.d("CalculationFragment", "下一期按钮启用状态: ${binding.nextPeriodButton.isEnabled}, alpha: ${binding.nextPeriodButton.alpha}")
        }

        viewModel.hasUserTickets.observe(viewLifecycleOwner) { hasTickets: Boolean ->
            android.util.Log.d("CalculationFragment", "计算全部按钮状态更新: 是否有彩票 = $hasTickets")
            binding.calculateAllButton.isEnabled = hasTickets
            // 按钮变灰效果
            binding.calculateAllButton.alpha = if (hasTickets) 1.0f else 0.5f
            android.util.Log.d("CalculationFragment", "计算全部按钮启用状态: ${binding.calculateAllButton.isEnabled}, alpha: ${binding.calculateAllButton.alpha}")
        }
    }

    private fun showAddTicketDialog() {
        val dialog = AddTicketDialogFragment(
            onSave = { ticket ->
                viewModel.saveUserTicket(ticket)
            }
        )
        val currentType = viewModel.currentLotteryType.value ?: com.example.lotterycalculator.data.model.LotteryType.DALETOU
        dialog.setLotteryType(currentType)
        dialog.show(childFragmentManager, "AddTicketDialog")
    }

    private fun showCalculationResults(results: List<com.example.lotterycalculator.data.model.LotteryResult>) {
        val dialog = CalculationResultsDialogFragment(initialResults = results)
        dialog.show(childFragmentManager, "CalculationResultsDialog")
    }

    // 加载中弹窗
    private var loadingDialog: android.app.AlertDialog? = null

    private fun showEditTicketNameDialog(ticket: com.example.lotterycalculator.data.model.UserTicket) {
        val context = requireContext()
        val editText = android.widget.EditText(context)
        editText.setText(ticket.name ?: "")
        editText.hint = "输入彩票名称"
        
        android.app.AlertDialog.Builder(context)
            .setTitle("修改彩票名称")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedTicket = ticket.copy(name = newName)
                    viewModel.saveUserTicket(updatedTicket)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示加载中弹窗
     */
    private fun showLoadingDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle("计算中")
            .setMessage("正在计算中奖结果，请稍候...")
            .setCancelable(false)
        
        loadingDialog = builder.create()
        loadingDialog?.show()
    }
    
    /**
     * 关闭加载中弹窗
     */
    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    /**
     * 计算单个彩票的中奖结果
     * @param ticket 要计算的彩票
     */
    private fun calculateSingleTicket(ticket: UserTicket) {
        // 先显示加载中的弹窗
        val dialog = CalculationResultsDialogFragment(viewModel.calculationResults)
        dialog.show(childFragmentManager, "CalculationResultsDialog")
        
        // 异步计算中奖结果
        viewModel.calculatePrizeForTicket(ticket)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}