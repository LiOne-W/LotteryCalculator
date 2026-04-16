package com.example.lotterycalculator.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.example.lotterycalculator.R
import com.example.lotterycalculator.databinding.ActivityMainBinding
import com.example.lotterycalculator.ui.display.DisplayFragment
import com.example.lotterycalculator.ui.calculation.CalculationFragment
import com.example.lotterycalculator.viewmodel.DisplayViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    // Toast实例管理
    private var loadingToast: Toast? = null
    private var waitToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, DisplayFragment())
            }
        }
    }
    
    /**
     * 清除所有Toast
     */
    private fun clearToasts() {
        loadingToast?.cancel()
        waitToast?.cancel()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_display -> {
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment !is DisplayFragment) {
                        replaceFragment(DisplayFragment())
                    }
                    true
                }
                R.id.navigation_calculation -> {
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    // 如果当前已经在彩票算奖界面，不执行任何操作
                    if (currentFragment is CalculationFragment) {
                        return@setOnItemSelectedListener true
                    }
                    
                    // 检查是否可以从开奖信息切换到彩票算奖
                    if (canNavigateToCalculation()) {
                        // 清除所有Toast
                        clearToasts()
                        replaceFragment(CalculationFragment())
                        true
                    } else {
                        // 显示相应的提示
                        showNavigationRestrictedToast()
                        false  // 返回false表示不处理此选择，保持当前选中项
                    }
                }
                else -> false
            }
        }
    }
    
    /**
     * 显示导航受限的提示，根据具体情况显示不同的Toast
     */
    private fun showNavigationRestrictedToast() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is DisplayFragment) return
        
        try {
            val viewModel = ViewModelProvider(this).get(DisplayViewModel::class.java)
            val bothLoaded = viewModel.bothLotteriesLoaded.value ?: false
            
            if (!bothLoaded) {
                // 显示加载中Toast
                showLoadingInProgressToast()
            } else {
                // 显示等待1秒Toast
                showWaitAfterLoadToast()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "获取ViewModel失败", e)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }
    
    /**
     * 检查是否可以从开奖信息切换到彩票算奖
     * 只有当大乐透和双色球都加载完成时才能切换
     */
    private fun canNavigateToCalculation(): Boolean {
        android.util.Log.d("MainActivity", "=== canNavigateToCalculation 调用 ===")
        
        // 获取当前显示的Fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        android.util.Log.d("MainActivity", "当前Fragment: $currentFragment")
        
        // 如果当前不是DisplayFragment，允许切换（可能是其他情况）
        if (currentFragment !is DisplayFragment) {
            android.util.Log.d("MainActivity", "当前不是DisplayFragment，允许切换")
            return true
        }
        
        android.util.Log.d("MainActivity", "当前是DisplayFragment，检查加载状态")
        
        // 尝试获取DisplayViewModel（使用Activity作用域，与DisplayFragment保持一致）
        return try {
            val viewModel = ViewModelProvider(this).get(DisplayViewModel::class.java)
            // 检查加载状态 - 只使用bothLotteriesLoaded，确保下拉刷新时的状态重置生效
            val bothLoaded = viewModel.bothLotteriesLoaded.value ?: false
            
            android.util.Log.d("MainActivity", "获取到ViewModel，加载状态: $bothLoaded")
            
            // 检查是否都加载完成
            if (!bothLoaded) {
                android.util.Log.d("MainActivity", "彩票未加载完成，不允许切换")
                return false
            }
            
            // 检查加载完成时间是否已过1秒
            val lastLoadedTime = viewModel.lastLoadedTime.value ?: 0L
            val currentTime = System.currentTimeMillis()
            val timeSinceLoaded = currentTime - lastLoadedTime
            
            android.util.Log.d("MainActivity", "加载完成时间检查 - 最后加载时间: $lastLoadedTime, 当前时间: $currentTime, 时间差: ${timeSinceLoaded}ms")
            
            if (timeSinceLoaded < 1000) {
                android.util.Log.d("MainActivity", "加载完成时间不足1秒（${timeSinceLoaded}ms），不允许切换")
                return false
            }
            
            android.util.Log.d("MainActivity", "✅ 允许切换 - 加载完成且已过1秒")
            return true
        } catch (e: Exception) {
            // 如果无法获取ViewModel，允许切换（避免阻塞用户）
            android.util.Log.e("MainActivity", "获取ViewModel失败", e)
            e.printStackTrace()
            true
        }
    }
    
    /**
     * 显示加载中的提示
     */
    private fun showLoadingInProgressToast() {
        android.util.Log.d("MainActivity", "显示加载中Toast")
        
        // 取消之前的Toast
        loadingToast?.cancel()
        
        // 创建新的Toast
        loadingToast = Toast.makeText(
            this,
            "大乐透和双色球正在加载中，请稍后再试",
            Toast.LENGTH_SHORT
        )
        loadingToast?.show()
    }
    
    /**
     * 显示加载完成但需要等待的提示
     */
    private fun showWaitAfterLoadToast() {
        android.util.Log.d("MainActivity", "显示等待Toast")
        
        // 取消之前的Toast
        waitToast?.cancel()
        
        // 创建新的Toast
        waitToast = Toast.makeText(
            this,
            "加载中，请稍后再切换",
            Toast.LENGTH_SHORT
        )
        waitToast?.show()
    }
}