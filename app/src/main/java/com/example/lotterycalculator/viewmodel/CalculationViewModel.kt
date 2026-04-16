package com.example.lotterycalculator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lotterycalculator.data.model.*
import com.example.lotterycalculator.repository.LotteryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class CalculationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LotteryRepository(application.applicationContext)

    private val _currentLotteryType = MutableLiveData<LotteryType>(LotteryType.DALETOU)
    val currentLotteryType: LiveData<LotteryType> = _currentLotteryType

    private val _userTickets = MutableLiveData<List<UserTicket>>(emptyList())
    val userTickets: LiveData<List<UserTicket>> = _userTickets

    private val _currentDrawResult = MutableLiveData<DrawResult?>()
    val currentDrawResult: LiveData<DrawResult?> = _currentDrawResult

    private val _calculationResults = MutableLiveData<List<LotteryResult>>(emptyList())
    val calculationResults: LiveData<List<LotteryResult>> = _calculationResults

    private val _historicalDrawResults = MutableLiveData<List<DrawResult>>(emptyList())
    val historicalDrawResults: LiveData<List<DrawResult>> = _historicalDrawResults

    private val _hasPreviousPeriod = MutableLiveData<Boolean>(true)
    val hasPreviousPeriod: LiveData<Boolean> = _hasPreviousPeriod

    private val _hasNextPeriod = MutableLiveData<Boolean>(true)
    val hasNextPeriod: LiveData<Boolean> = _hasNextPeriod

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _isCalculating = MutableLiveData<Boolean>(false)
    val isCalculating: LiveData<Boolean> = _isCalculating
    
    private val _calculationProgress = MutableLiveData<Int>(0)
    val calculationProgress: LiveData<Int> = _calculationProgress
    
    private val _hasUserTickets = MutableLiveData<Boolean>(false)
    val hasUserTickets: LiveData<Boolean> = _hasUserTickets

    fun setCurrentLotteryType(lotteryType: LotteryType) {
        _currentLotteryType.value = lotteryType
        
        // 检查缓存中是否有该彩票类型的开奖结果
        val cachedResult = repository.getCachedDrawResult(lotteryType)
        if (cachedResult != null) {
            // 使用缓存数据
            _currentDrawResult.value = cachedResult
            _userTickets.value = emptyList()
            _hasUserTickets.value = false
            _historicalDrawResults.value = emptyList()
            updatePeriodButtonStates()
            
            // 加载用户彩票（从本地数据库）
            loadUserTickets()
            // 加载历史数据（可能需要更新按钮状态）
            loadHistoricalDrawResults()
        } else {
            // 没有缓存，先清除旧数据，避免显示错误的彩票类型数据
            _currentDrawResult.value = null
            _userTickets.value = emptyList()
            _hasUserTickets.value = false
            _historicalDrawResults.value = emptyList()
            updatePeriodButtonStates()
            
            // 启动延时加载协程
            viewModelScope.launch {
                // 添加1-2秒随机延时（用户要求）
                val delayMillis = (1000L..2000L).random()
                delay(delayMillis)
                
                // 延时后检查当前彩票类型是否仍然与请求一致
                if (_currentLotteryType.value == lotteryType) {
                    loadUserTickets()
                    loadLatestDrawResult()
                }
            }
        }
    }

    fun loadUserTickets() {
        _isLoading.value = true
        viewModelScope.launch {
            val requestedLotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
            try {
                val tickets = repository.getUserTickets(requestedLotteryType)
                // 检查当前彩票类型是否与请求时一致，避免显示错误类型的数据
                if (_currentLotteryType.value == requestedLotteryType) {
                    _userTickets.value = tickets
                    _hasUserTickets.value = tickets.isNotEmpty()
                } else {
                    // 如果类型已改变，丢弃结果
                    _userTickets.value = emptyList()
                    _hasUserTickets.value = false
                }
            } catch (e: Exception) {
                // 处理错误，设置用户彩票列表为空
                e.printStackTrace()
                _userTickets.value = emptyList()
                _hasUserTickets.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLatestDrawResult() {
        _isLoading.value = true
        viewModelScope.launch {
            val requestedLotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
            try {
                val drawResult = repository.getLatestDrawResult(requestedLotteryType)
                // 检查当前彩票类型是否与请求时一致，避免显示错误类型的数据
                if (_currentLotteryType.value == requestedLotteryType) {
                    _currentDrawResult.value = drawResult
                    // 先加载历史数据，确保期号边界被更新
                    loadHistoricalDrawResults()
                    // 再更新按钮状态
                    updatePeriodButtonStates()
                } else {
                    // 如果类型已改变，丢弃结果
                    _currentDrawResult.value = null
                    updatePeriodButtonStates()
                }
            } catch (e: Exception) {
                // 处理错误，设置当前开奖结果为null
                e.printStackTrace()
                _currentDrawResult.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadHistoricalDrawResults() {
        viewModelScope.launch {
            val requestedLotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
            android.util.Log.d("CalculationViewModel", "开始加载历史数据，彩票类型: $requestedLotteryType")
            
            try {
                // 调用API获取历史数据（不更新期号边界，边界已在开奖信息中设置）
                val historicalResults = repository.getHistoricalDrawResults(requestedLotteryType, updateBoundary = false)
                android.util.Log.d("CalculationViewModel", "API获取历史数据结果: ${historicalResults.size} 条")
                
                // 检查当前彩票类型是否与请求时一致
                if (_currentLotteryType.value == requestedLotteryType) {
                    _historicalDrawResults.value = historicalResults
                    updatePeriodButtonStates()
                } else {
                    // 如果类型已改变，丢弃结果
                    android.util.Log.d("CalculationViewModel", "彩票类型已改变，丢弃历史数据")
                    _historicalDrawResults.value = emptyList()
                    updatePeriodButtonStates()
                }
            } catch (e: Exception) {
                android.util.Log.e("CalculationViewModel", "加载历史数据失败", e)
                // 加载失败时从缓存获取
                val cachedResults = repository.getCachedHistoricalResults(requestedLotteryType)
                if (_currentLotteryType.value == requestedLotteryType) {
                    _historicalDrawResults.value = cachedResults ?: emptyList()
                    updatePeriodButtonStates()
                }
            }
        }
    }
    
    private fun updatePeriodButtonStates() {
        val currentResult = _currentDrawResult.value
        
        android.util.Log.d("CalculationViewModel", "updatePeriodButtonStates called - currentResult: $currentResult")
        
        if (currentResult == null) {
            // 当前期号未加载，无法确定按钮状态，保持当前状态
            android.util.Log.d("CalculationViewModel", "当前期号未加载，保持按钮状态不变")
            return
        }
        
        val currentPeriod = currentResult.period
        val lotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
        
        // 获取期号边界（应该在开奖信息中已更新）
        val periodBoundary = repository.getPeriodBoundary(lotteryType)
        if (periodBoundary != null) {
            android.util.Log.d("CalculationViewModel", "使用期号边界判断 - 最新期: ${periodBoundary.latestPeriod}, 最早期: ${periodBoundary.earliestPeriod}")
            
            // 检查当前期号是否在边界范围内
            if (!periodBoundary.isWithinBoundaries(currentPeriod)) {
                android.util.Log.w("CalculationViewModel", "当前期号 $currentPeriod 超出边界范围（最早: ${periodBoundary.earliestPeriod}, 最新: ${periodBoundary.latestPeriod}），禁用所有按钮")
                _hasPreviousPeriod.value = false
                _hasNextPeriod.value = false
                return
            }
            
            // 根据用户要求的简单逻辑判断按钮状态
            val isAtEarliest = periodBoundary.isAtEarliest(currentPeriod)
            val isAtLatest = periodBoundary.isAtLatest(currentPeriod)
            
            android.util.Log.d("CalculationViewModel", "当前期号位置 - 在最早期: $isAtEarliest, 在最新期: $isAtLatest")
            
            // 1. 当前期号是最早一期：左按钮禁用，右按钮激活
            // 2. 当前期号是最新一期：右按钮禁用，左按钮激活  
            // 3. 当前期号在中间：左右按钮都激活
            _hasPreviousPeriod.value = !isAtEarliest  // 不是最早期就有上一期
            _hasNextPeriod.value = !isAtLatest        // 不是最新期就有下一期
            
            android.util.Log.d("CalculationViewModel", "按钮状态 - 有上一期: ${_hasPreviousPeriod.value}, 有下一期: ${_hasNextPeriod.value}")
        } else {
            // 期号边界未缓存（首次使用或未在开奖信息中刷新）
            android.util.Log.w("CalculationViewModel", "期号边界未缓存，无法判断按钮状态，禁用所有按钮")
            _hasPreviousPeriod.value = false
            _hasNextPeriod.value = false
        }
    }

    fun saveUserTicket(ticket: UserTicket) {
        viewModelScope.launch {
            repository.saveUserTicket(ticket)
            loadUserTickets()
        }
    }

    fun deleteUserTicket(ticketId: String) {
        viewModelScope.launch {
            repository.deleteUserTicket(ticketId)
            loadUserTickets()
        }
    }

    fun calculatePrizeForTicket(ticket: UserTicket) {
        val drawResult = _currentDrawResult.value
        
        viewModelScope.launch {
            if (drawResult == null) {
                // 如果开奖结果还未加载，先显示加载中状态
                _calculationResults.value = emptyList()
                return@launch
            }
            
            try {
                // 调用API获取中奖描述
                val prizeDescription = repository.calculatePrize(ticket, drawResult.period)
                // 使用API返回的中奖描述创建结果
                val result = com.example.lotterycalculator.data.model.LotteryResult.calculatePrize(ticket, drawResult, prizeDescription)
                _calculationResults.value = listOf(result)
            } catch (e: Exception) {
                e.printStackTrace()
                // API调用失败时，使用本地计算结果
                val result = com.example.lotterycalculator.data.model.LotteryResult.calculatePrize(ticket, drawResult)
                _calculationResults.value = listOf(result)
            }
        }
    }

    fun calculateAllTickets() {
        val drawResult = _currentDrawResult.value ?: return
        val tickets = _userTickets.value ?: emptyList()
        
        if (tickets.isEmpty()) {
            _calculationResults.value = emptyList()
            return
        }
        
        // 设置计算中状态
        _isCalculating.value = true
        _calculationProgress.value = 0
        
        viewModelScope.launch {
            try {
                val results = mutableListOf<com.example.lotterycalculator.data.model.LotteryResult>()
                val totalTickets = tickets.size
                var processedTickets = 0
                
                for (ticket in tickets) {
                    if (ticket.isCombination) {
                        // 处理组合票
                        val combinations = ticket.generateAllCombinations()
                        for (combo in combinations) {
                            val prizeResult = calculateTicketPrize(combo, drawResult)
                            if (prizeResult != null) {
                                results.add(prizeResult)
                            }
                            // 每个请求之间加0.5秒延时
                            delay(500)
                        }
                    } else {
                        // 处理普通票
                        val prizeResult = calculateTicketPrize(ticket, drawResult)
                        if (prizeResult != null) {
                            results.add(prizeResult)
                        }
                        // 每个请求之间加0.5秒延时
                        delay(500)
                    }
                    
                    // 更新进度
                    processedTickets++
                    _calculationProgress.value = (processedTickets * 100) / totalTickets
                }
                
                // 所有彩票计算完成后，一次性更新结果
                _calculationResults.value = results
            } finally {
                // 计算完成，设置计算中状态为false
                _isCalculating.value = false
                _calculationProgress.value = 100
            }
        }
    }
    
    /**
     * 计算单个彩票的中奖结果
     * @param ticket 彩票
     * @param drawResult 开奖结果
     * @return 中奖结果
     */
    private suspend fun calculateTicketPrize(ticket: UserTicket, drawResult: com.example.lotterycalculator.data.model.DrawResult): com.example.lotterycalculator.data.model.LotteryResult {
        return try {
            // 调用API计算中奖结果
            val prizeDescription = repository.calculatePrize(ticket, drawResult.period)
            
            // 不管API是否成功，都使用本地计算的结果（保持一致性）
            val result = com.example.lotterycalculator.data.model.LotteryResult.calculatePrize(ticket, drawResult)
            
            // 如果API调用成功，使用API返回的描述
            if (prizeDescription != null) {
                // 创建一个新的LotteryResult，使用API的描述
                result.copy(
                    // 注意：LotteryResult类需要支持修改prizeLevel的description
                    // 由于当前LotteryResult的prizeLevel是枚举，我们暂时使用本地计算的结果
                    // 实际应用中可能需要修改LotteryResult类来支持自定义描述
                )
            } else {
                result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 异常时回退到本地计算
            com.example.lotterycalculator.data.model.LotteryResult.calculatePrize(ticket, drawResult)
        }
    }

    fun clearAllTickets() {
        viewModelScope.launch {
            val tickets = _userTickets.value ?: emptyList()
            tickets.forEach { ticket ->
                repository.deleteUserTicket(ticket.id)
            }
            loadUserTickets()
            _calculationResults.value = emptyList()
        }
    }

    fun clearCalculationResults() {
        _calculationResults.value = emptyList()
    }

    fun loadDrawResult(period: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val requestedLotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
            try {
                val drawResult = if (period == "最新一期") {
                    repository.getLatestDrawResult(requestedLotteryType)
                } else {
                    // 优先使用aim_lottery接口获取指定期号
                    try {
                        android.util.Log.d("CalculationViewModel", "尝试使用aim_lottery接口获取期号: $period")
                        repository.getDrawResultByPeriod(requestedLotteryType, period)
                    } catch (e: Exception) {
                        android.util.Log.d("CalculationViewModel", "aim_lottery接口失败，回退到历史数据查找: ${e.message}")
                        // 回退到历史数据查找
                        val cachedResults = repository.getCachedHistoricalResults(requestedLotteryType)
                        val foundInCache = cachedResults?.find { it.period == period }
                        
                        if (foundInCache != null) {
                            android.util.Log.d("CalculationViewModel", "在缓存中找到期号: $period")
                            foundInCache
                        } else {
                            android.util.Log.d("CalculationViewModel", "缓存中未找到期号: $period，尝试从API获取历史数据")
                            // 缓存中未找到，回退到API调用历史数据
                            val historicalResults = repository.getHistoricalDrawResults(requestedLotteryType)
                            historicalResults.find { it.period == period } ?: repository.getLatestDrawResult(requestedLotteryType)
                        }
                    }
                }
                // 检查当前彩票类型是否与请求时一致，避免显示错误类型的数据
                if (_currentLotteryType.value == requestedLotteryType) {
                    _currentDrawResult.value = drawResult
                    updatePeriodButtonStates()
                } else {
                    // 如果类型已改变，丢弃结果
                    _currentDrawResult.value = null
                    updatePeriodButtonStates()
                }
            } catch (e: Exception) {
                // 处理错误，设置当前开奖结果为null
                e.printStackTrace()
                _currentDrawResult.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPreviousPeriod() {
        android.util.Log.d("CalculationViewModel", "=== loadPreviousPeriod called ===")
        android.util.Log.d("CalculationViewModel", "当前按钮状态 - 有上一期: ${_hasPreviousPeriod.value}, 有下一期: ${_hasNextPeriod.value}")
        
        val currentResult = _currentDrawResult.value ?: return.also { 
            android.util.Log.d("CalculationViewModel", "❌ 当前开奖结果为空，无法导航")
        }
        
        val lotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
        android.util.Log.d("CalculationViewModel", "当前彩票类型: $lotteryType, 当前期号: ${currentResult.period}")
        
        // 检查是否有上一期（按钮应该已经处理了，但再次确认）
        if (!(_hasPreviousPeriod.value ?: false)) {
            android.util.Log.d("CalculationViewModel", "❌ 没有上一期，按钮已被禁用")
            return
        }
        
        // 计算上一期期号
        val previousPeriod = PeriodBoundary.calculatePreviousPeriod(currentResult.period, lotteryType)
        if (previousPeriod == null) {
            android.util.Log.d("CalculationViewModel", "❌ 无法计算上一期期号")
            return
        }
        
        android.util.Log.d("CalculationViewModel", "计算得到上一期期号: $previousPeriod")
        
        // 使用aim_lottery接口获取指定期号的开奖结果
        viewModelScope.launch {
            try {
                android.util.Log.d("CalculationViewModel", "开始请求aim_lottery接口，期号: $previousPeriod")
                val drawResult = repository.getDrawResultByPeriod(lotteryType, previousPeriod)
                android.util.Log.d("CalculationViewModel", "✅ 成功获取上一期开奖结果，期号: ${drawResult.period}")
                
                // 更新当前开奖结果
                _currentDrawResult.value = drawResult
                // 更新按钮状态
                updatePeriodButtonStates()
                
                // 重新计算用户彩票中奖结果（左右按钮导航时不弹出结果页面）
                // calculateAllTickets()
            } catch (e: Exception) {
                android.util.Log.e("CalculationViewModel", "❌ 获取期号 $previousPeriod 开奖结果失败", e)
                // 可以显示错误提示，但暂时只记录日志
            }
        }
    }
    
    fun loadNextPeriod() {
        android.util.Log.d("CalculationViewModel", "=== loadNextPeriod called ===")
        android.util.Log.d("CalculationViewModel", "当前按钮状态 - 有上一期: ${_hasPreviousPeriod.value}, 有下一期: ${_hasNextPeriod.value}")
        
        val currentResult = _currentDrawResult.value ?: return.also { 
            android.util.Log.d("CalculationViewModel", "❌ 当前开奖结果为空，无法导航")
        }
        
        val lotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
        android.util.Log.d("CalculationViewModel", "当前彩票类型: $lotteryType, 当前期号: ${currentResult.period}")
        
        // 检查是否有下一期（按钮应该已经处理了，但再次确认）
        if (!(_hasNextPeriod.value ?: false)) {
            android.util.Log.d("CalculationViewModel", "❌ 没有下一期，按钮已被禁用")
            return
        }
        
        // 计算下一期期号
        val nextPeriod = PeriodBoundary.calculateNextPeriod(currentResult.period, lotteryType)
        if (nextPeriod == null) {
            android.util.Log.d("CalculationViewModel", "❌ 无法计算下一期期号")
            return
        }
        
        android.util.Log.d("CalculationViewModel", "计算得到下一期期号: $nextPeriod")
        
        // 使用aim_lottery接口获取指定期号的开奖结果
        viewModelScope.launch {
            try {
                android.util.Log.d("CalculationViewModel", "开始请求aim_lottery接口，期号: $nextPeriod")
                val drawResult = repository.getDrawResultByPeriod(lotteryType, nextPeriod)
                android.util.Log.d("CalculationViewModel", "✅ 成功获取下一期开奖结果，期号: ${drawResult.period}")
                
                // 更新当前开奖结果
                _currentDrawResult.value = drawResult
                
                // 更新按钮状态
                updatePeriodButtonStates()
                
                // 重新计算用户彩票中奖结果（左右按钮导航时不弹出结果页面）
                // calculateAllTickets()
            } catch (e: Exception) {
                android.util.Log.e("CalculationViewModel", "❌ 获取期号 $nextPeriod 开奖结果失败", e)
                // 可以显示错误提示，但暂时只记录日志
            }
        }
    }

    suspend fun getHistoricalDrawResults(): List<DrawResult> {
        val lotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
        return repository.getHistoricalDrawResults(lotteryType)
    }
}