package org.example

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PaginatedFlowTest {

    @Test
    fun `loads more data when requested`() = runTest {
        val flow = paginatedFlow<List<Int>> {
            val items = mutableListOf(1)
            emit(items)

            onLoadMore {
                items += 1
                emit(items)
            }
        }

        flow.test {
            assertEquals(listOf(1), awaitItem())
            flow.loadMore()
            assertEquals(listOf(1, 1), awaitItem())
            flow.loadMore()
            assertEquals(listOf(1, 1, 1), awaitItem())
        }
    }

    @Test
    fun `propagates exceptions from flow creation`() = runTest {
        val exception = IllegalStateException("Exception")
        val flow = paginatedFlow<Unit> {
            throw exception
        }

        flow.test {
            assertEquals(exception, awaitError())
        }
    }

    @Test
    fun `propagates exceptions from onLoadMore`() = runTest {
        val exception = IllegalStateException("Exception")
        val flow = paginatedFlow<String> {
            emit("Hello")
            onLoadMore {
                throw exception
            }
        }

        flow.test {
            awaitItem()
            flow.loadMore()
            assertEquals(exception, awaitError())
        }
    }

    @Test
    fun `correctly handles multiple collectors`() = runTest {
        val item = "dog"
        val flow = paginatedFlow {
            onLoadMore { emit(item) }
        }

        turbineScope {
            val testCollector1 = flow.testIn(backgroundScope)
            val testCollector2 = flow.testIn(backgroundScope)
            flow.loadMore()
            assertEquals(item, testCollector1.awaitItem())
            assertEquals(item, testCollector2.awaitItem())
        }
    }
}
