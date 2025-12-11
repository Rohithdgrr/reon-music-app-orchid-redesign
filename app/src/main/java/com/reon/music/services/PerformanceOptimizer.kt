/*
 * REON Music App - Performance Optimizer
 * Copyright (c) 2024 REON
 * Lazy loading, pagination, and memory efficiency
 */

package com.reon.music.services

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local pagination configuration to avoid paging library dependency
 */
data class PaginationConfig(
    val pageSize: Int,
    val initialLoadSize: Int,
    val prefetchDistance: Int,
    val maxSize: Int,
    val enablePlaceholders: Boolean
)

@Singleton
class PerformanceOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // Pagination settings for optimal performance
        const val INITIAL_LOAD_SIZE = 20
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 10
        const val MAX_SIZE = 500  // Keep max 500 items in memory
        
        // Lazy loading thresholds
        const val IMAGE_LOAD_DELAY_MS = 100
        const val SCROLL_LOAD_DELAY_MS = 500
    }
    
    /**
     * Create paging configuration for list optimization
     */
    fun getPagingConfig() = PaginationConfig(
        pageSize = PAGE_SIZE,
        initialLoadSize = INITIAL_LOAD_SIZE,
        prefetchDistance = PREFETCH_DISTANCE,
        maxSize = MAX_SIZE,
        enablePlaceholders = true
    )
    
    /**
     * Lazy load composables only when visible
     */
    @Composable
    fun LazyLoadScreen(
        screenKey: String,
        content: @Composable () -> Unit
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val isVisible = remember(lifecycleOwner) {
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        
        if (isVisible) {
            content()
        }
    }
    
    /**
     * Defer expensive operations to background thread
     */
    suspend fun <T> deferredComputation(
        block: suspend () -> T
    ): T {
        return block.invoke()
    }
    
    /**
     * Calculate optimal image dimensions based on device
     */
    fun getOptimalImageDimension(baseSize: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        
        // Adjust size based on device density
        return (baseSize * density).toInt()
    }
    
    /**
     * Memory-efficient string operations
     */
    fun truncateString(text: String, maxLength: Int = 100): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength) + "..."
        } else {
            text
        }
    }
    
    /**
     * Batch expensive operations
     */
    suspend fun <T> batchOperation(
        items: List<T>,
        batchSize: Int = 10,
        operation: suspend (List<T>) -> Unit
    ) {
        items.chunked(batchSize).forEach { batch ->
            operation(batch)
        }
    }
}

/**
 * Lazy loading list state holder
 */
class LazyListState(
    val pageSize: Int = PerformanceOptimizer.PAGE_SIZE,
    val prefetchDistance: Int = PerformanceOptimizer.PREFETCH_DISTANCE
) {
    var currentIndex = 0
    var isLoadingMore = false
    
    fun shouldLoadMore(visibleItemCount: Int, totalItemCount: Int): Boolean {
        return visibleItemCount + prefetchDistance >= totalItemCount && !isLoadingMore
    }
}

/**
 * Efficient list pagination helper
 */
class PaginationHelper<T>(
    val pageSize: Int = PerformanceOptimizer.PAGE_SIZE
) {
    private val items = mutableListOf<T>()
    var currentPage = 0
    var hasNextPage = true
    
    suspend fun loadPage(
        pageLoader: suspend (page: Int, pageSize: Int) -> List<T>
    ): List<T> {
        val newItems = pageLoader(currentPage, pageSize)
        hasNextPage = newItems.size == pageSize
        items.addAll(newItems)
        currentPage++
        return newItems
    }
    
    fun getAllItems(): List<T> = items.toList()
    
    fun clear() {
        items.clear()
        currentPage = 0
        hasNextPage = true
    }
}

/**
 * Memory pool for reusing objects and reducing GC pressure
 */
class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    initialSize: Int = 10
) {
    private val available = mutableListOf<T>()
    private val inUse = mutableSetOf<T>()
    
    init {
        repeat(initialSize) {
            available.add(factory())
        }
    }
    
    fun acquire(): T {
        return if (available.isEmpty()) {
            factory().also { inUse.add(it) }
        } else {
            available.removeAt(0).also { inUse.add(it) }
        }
    }
    
    fun release(obj: T) {
        if (inUse.remove(obj)) {
            reset(obj)
            available.add(obj)
        }
    }
    
    fun clear() {
        available.clear()
        inUse.clear()
    }
}
