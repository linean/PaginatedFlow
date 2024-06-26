package org.example

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Simple [Flow] that can be used to wrap paginated data.
 * Example usage:
 * ```
 * val flow = paginatedFlow {
 *     val items = mutableListOf(1)
 *     emit(items)
 *
 *     onLoadMore {
 *         items += 1
 *         emit(items)
 *     }
 * }
 * ```
 */
interface PaginatedFlow<T> : Flow<T> {

    fun loadMore()
}

interface PaginatedFlowCollector<T> : FlowCollector<T> {

    suspend fun onLoadMore(action: suspend () -> Unit)
}

fun <T> paginatedFlow(
    block: suspend PaginatedFlowCollector<T>.() -> Unit,
): PaginatedFlow<T> {
    return PaginatedFlowImpl(block)
}

@OptIn(ExperimentalCoroutinesApi::class)
private class PaginatedFlowImpl<T>(
    private val block: suspend PaginatedFlowCollector<T>.() -> Unit,
) : AbstractFlow<T>(), PaginatedFlow<T> {

    private val loadMoreFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val paginatedCollector = object : PaginatedFlowCollector<T> {
            override suspend fun onLoadMore(action: suspend () -> Unit) {
                loadMoreFlow.collect { action() }
            }

            override suspend fun emit(value: T) {
                collector.emit(value)
            }
        }
        paginatedCollector.block()
    }

    override fun loadMore() {
        loadMoreFlow.tryEmit(Unit)
    }
}