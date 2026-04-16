package com.example.lotterycalculator.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lotterycalculator.data.model.DrawResult
import com.example.lotterycalculator.data.model.LotteryType
import com.example.lotterycalculator.data.model.PeriodBoundary
import com.example.lotterycalculator.repository.LotteryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class DisplayViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "DisplayViewModel"
    }
    
    private val repository = LotteryRepository(application.applicationContext)

    private val _currentLotteryType = MutableLiveData<LotteryType>(LotteryType.DALETOU)
    val currentLotteryType: LiveData<LotteryType> = _currentLotteryType

    private val _currentDrawResult = MutableLiveData<DrawResult?>()
    val currentDrawResult: LiveData<DrawResult?> = _currentDrawResult

    private val _historyDrawResults = MutableLiveData<List<DrawResult>>(emptyList())
    val historyDrawResults: LiveData<List<DrawResult>> = _historyDrawResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _daletouLatestResult = MutableLiveData<DrawResult?>(null)
    private val _shuangseqiuLatestResult = MutableLiveData<DrawResult?>(null)
    
    // 跟踪大乐透和双色球是否都加载完成
    private val _bothLotteriesLoaded = MutableLiveData<Boolean>(false)
    
    // 记录最后加载完成的时间（毫秒时间戳）
    private val _lastLoadedTime = MutableLiveData<Long>(0L)

    val daletouLatestResult: LiveData<DrawResult?> = _daletouLatestResult
    val shuangseqiuLatestResult: LiveData<DrawResult?> = _shuangseqiuLatestResult
    val bothLotteriesLoaded: LiveData<Boolean> = _bothLotteriesLoaded
    val lastLoadedTime: LiveData<Long> = _lastLoadedTime

    /**
     * 更新大乐透和双色球是否都加载完成的状态
     * 当两个彩票的最新结果都不为null时，表示都加载完成
     */
    private fun updateBothLoadedStatus() {
        val daletouResult = _daletouLatestResult.value
        val shuangseqiuResult = _shuangseqiuLatestResult.value
        val daletouLoaded = daletouResult != null
        val shuangseqiuLoaded = shuangseqiuResult != null
        val bothLoaded = daletouLoaded && shuangseqiuLoaded
        
        Log.d(TAG, "检查加载状态 - 大乐透结果: ${daletouResult?.period}, 双色球结果: ${shuangseqiuResult?.period}")
        Log.d(TAG, "检查加载状态 - 大乐透已加载: $daletouLoaded, 双色球已加载: $shuangseqiuLoaded, 都完成: $bothLoaded")
        
        if (_bothLotteriesLoaded.value != bothLoaded) {
            _bothLotteriesLoaded.value = bothLoaded
            Log.d(TAG, "✅ 更新加载状态 - 大乐透: $daletouLoaded, 双色球: $shuangseqiuLoaded, 都完成: $bothLoaded")
            
            // 如果状态变为true（加载完成），记录当前时间
            if (bothLoaded) {
                val currentTime = System.currentTimeMillis()
                _lastLoadedTime.value = currentTime
                Log.d(TAG, "✅ 记录加载完成时间: $currentTime")
            }
        }
    }
    
    /**
     * 重置加载状态（在下拉刷新时调用）
     */
    fun resetLoadingStatus() {
        Log.d(TAG, "重置加载状态")
        _bothLotteriesLoaded.value = false
        _lastLoadedTime.value = 0L
    }

    fun setCurrentLotteryType(lotteryType: LotteryType) {
        _currentLotteryType.value = lotteryType
    }

    fun loadDrawResult(period: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val lotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
                val drawResult = if (period == "最新一期") {
                    repository.getLatestDrawResult(lotteryType)
                } else {
                    repository.getDrawResultByPeriod(lotteryType, period)
                }
                withContext(Dispatchers.Main) {
                    _currentDrawResult.value = drawResult
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _error.value = "加载数据失败: " + e.message
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun loadLatestDrawResult() {
        loadDrawResult("最新一期")
    }

    fun loadAllLatestResults() {
        // 为了向后兼容，顺序调用两个独立函数
        loadDaletouLatest()
        loadShuangseqiuLatest()
    }

    fun loadDaletouLatest() {
        // 注意：不设置_isLoading和_error，由调用方控制
        viewModelScope.launch {
            Log.d(TAG, "开始加载大乐透最新号码")
            val daletouResult = try {
                val result = repository.getLatestDrawResult(LotteryType.DALETOU)
                Log.d(TAG, "大乐透加载成功，期号: ${result.period}")
                
                // 更新期号边界（最新一期）
                val earliestPeriod = PeriodBoundary.calculateEarliestPeriod(result.period)
                if (earliestPeriod.isNotEmpty()) {
                    val boundary = PeriodBoundary(
                        lotteryType = LotteryType.DALETOU,
                        latestPeriod = result.period,
                        earliestPeriod = earliestPeriod
                    )
                    // 更新到Repository的缓存中
                    repository.updatePeriodBoundary(boundary)
                    Log.d(TAG, "更新大乐透期号边界 - 最新期: ${result.period}, 最早期: $earliestPeriod")
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "大乐透加载失败", e)
                null
            }
            
            // 使用withContext确保在主线程更新UI
            withContext(Dispatchers.Main) {
                _daletouLatestResult.value = daletouResult ?: getZeroFilledDrawResult(LotteryType.DALETOU)
                if (daletouResult == null) {
                    Log.w(TAG, "大乐透显示补全号码")
                }
                // 更新加载状态
                updateBothLoadedStatus()
            }
        }
    }
    
    fun loadShuangseqiuLatest() {
        // 注意：不设置_isLoading和_error，由调用方控制
        viewModelScope.launch {
            Log.d(TAG, "开始加载双色球最新号码")
            val shuangseqiuResult = try {
                val result = repository.getLatestDrawResult(LotteryType.SHUANGSEQIU)
                Log.d(TAG, "双色球加载成功，期号: ${result.period}")
                
                // 更新期号边界（最新一期）
                val earliestPeriod = PeriodBoundary.calculateEarliestPeriod(result.period)
                if (earliestPeriod.isNotEmpty()) {
                    val boundary = PeriodBoundary(
                        lotteryType = LotteryType.SHUANGSEQIU,
                        latestPeriod = result.period,
                        earliestPeriod = earliestPeriod
                    )
                    // 更新到Repository的缓存中
                    repository.updatePeriodBoundary(boundary)
                    Log.d(TAG, "更新双色球期号边界 - 最新期: ${result.period}, 最早期: $earliestPeriod")
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "双色球加载失败", e)
                null
            }
            
            // 使用withContext确保在主线程更新UI
            withContext(Dispatchers.Main) {
                _shuangseqiuLatestResult.value = shuangseqiuResult ?: getZeroFilledDrawResult(LotteryType.SHUANGSEQIU)
                if (shuangseqiuResult == null) {
                    Log.w(TAG, "双色球显示补全号码")
                }
                // 更新加载状态
                updateBothLoadedStatus()
            }
        }
    }

    fun loadHistoryDrawResults() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val lotteryType = _currentLotteryType.value ?: LotteryType.DALETOU
                val results = repository.getHistoricalDrawResults(lotteryType, updateBoundary = false)
                
                withContext(Dispatchers.Main) {
                    if (results.isNotEmpty()) {
                        _historyDrawResults.value = results
                    } else {
                        // 如果网络获取失败，显示空列表
                        _historyDrawResults.value = emptyList()
                        _error.value = "未获取到历史数据"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _error.value = "加载数据失败: " + e.message
                    // 显示空列表
                    _historyDrawResults.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun generateMockHistoryResults(lotteryType: LotteryType, count: Int): List<DrawResult> {
        val results = mutableListOf<DrawResult>()
        val basePeriod = 2024001

        for (i in 0 until count) {
            val period = (basePeriod - i).toString()
            val result = when (lotteryType) {
                LotteryType.DALETOU -> DrawResult(
                    id = period,
                    lotteryType = lotteryType,
                    period = period,
                    drawDate = Date(),
                    frontNumbers = generateRandomNumbers(1, 35, 5),
                    backNumbers = generateRandomNumbers(1, 12, 2),
                    prizePool = 1000000000L - i * 10000000L,
                    sales = 500000000L - i * 5000000L,
                    jackpotWinners = if (i % 5 == 0) 1 else 0,
                    jackpotPrize = if (i % 5 == 0) 50000000L else null
                )
                LotteryType.SHUANGSEQIU -> DrawResult(
                    id = period,
                    lotteryType = lotteryType,
                    period = period,
                    drawDate = Date(),
                    frontNumbers = generateRandomNumbers(1, 33, 6),
                    backNumbers = generateRandomNumbers(1, 16, 1),
                    prizePool = 800000000L - i * 8000000L,
                    sales = 400000000L - i * 4000000L,
                    jackpotWinners = if (i % 6 == 0) 1 else 0,
                    jackpotPrize = if (i % 6 == 0) 80000000L else null
                )
            }
            results.add(result)
        }

        return results
    }

    private fun generateRandomNumbers(min: Int, max: Int, count: Int): List<Int> {
        val numbers = mutableSetOf<Int>()
        val random = java.util.Random()

        while (numbers.size < count) {
            numbers.add(random.nextInt(max - min + 1) + min)
        }

        return numbers.toList().sorted()
    }

    private fun getMockLatestDrawResult(lotteryType: LotteryType): DrawResult {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val period = "${year}${month.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}"
        
        return when (lotteryType) {
            LotteryType.DALETOU -> DrawResult(
                id = "1",
                lotteryType = lotteryType,
                period = period,
                drawDate = Date(),
                frontNumbers = listOf(1, 5, 12, 23, 30),
                backNumbers = listOf(3, 8),
                prizePool = 1000000000L,
                sales = 500000000L,
                jackpotWinners = 2,
                jackpotPrize = 50000000L
            )
            LotteryType.SHUANGSEQIU -> DrawResult(
                id = "2",
                lotteryType = lotteryType,
                period = period,
                drawDate = Date(),
                frontNumbers = listOf(3, 8, 15, 20, 25, 30),
                backNumbers = listOf(12),
                prizePool = 800000000L,
                sales = 400000000L,
                jackpotWinners = 1,
                jackpotPrize = 80000000L
            )
        }
    }

    private fun getMockHistoricalDrawResult(lotteryType: LotteryType, period: String): DrawResult {
        return when (lotteryType) {
            LotteryType.DALETOU -> DrawResult(
                id = period,
                lotteryType = lotteryType,
                period = period,
                drawDate = Date(),
                frontNumbers = listOf(2, 7, 14, 21, 29),
                backNumbers = listOf(4, 9),
                prizePool = 950000000L,
                sales = 480000000L,
                jackpotWinners = 0,
                jackpotPrize = null
            )
            LotteryType.SHUANGSEQIU -> DrawResult(
                id = period,
                lotteryType = lotteryType,
                period = period,
                drawDate = Date(),
                frontNumbers = listOf(4, 9, 16, 22, 27, 33),
                backNumbers = listOf(11),
                prizePool = 850000000L,
                sales = 420000000L,
                jackpotWinners = 2,
                jackpotPrize = 40000000L
            )
        }
    }

    private fun getZeroFilledDrawResult(lotteryType: LotteryType): DrawResult {
        val currentDate = Date()
        return when (lotteryType) {
            LotteryType.DALETOU -> DrawResult(
                id = "error",
                lotteryType = lotteryType,
                period = "null",
                drawDate = currentDate,
                frontNumbers = List(5) { 0 },  // 5个0
                backNumbers = List(2) { 0 },   // 2个0
                prizePool = 0L,
                sales = 0L,
                jackpotWinners = 0,
                jackpotPrize = null
            )
            LotteryType.SHUANGSEQIU -> DrawResult(
                id = "error",
                lotteryType = lotteryType,
                period = "null",
                drawDate = currentDate,
                frontNumbers = List(6) { 0 },  // 6个0
                backNumbers = List(1) { 0 },   // 1个0
                prizePool = 0L,
                sales = 0L,
                jackpotWinners = 0,
                jackpotPrize = null
            )
        }
    }
}