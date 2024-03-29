## PaginatedFlow in Kotlin Coroutines: A Handy Tool for Paginated Data

In modern application development, dealing with paginated data sources is a common scenario. 
Whether you're fetching data from a REST API, a database, or any other source, handling pagination efficiently can be challenging. 
Kotlin Coroutines offer a powerful way to handle asynchronous programming, and with the introduction of Flow, 
handling streams of data has become even more seamless.

## Understanding PaginatedFlow

PaginatedFlow is a specialized implementation of Kotlin's Flow interface that I created, 
enabling consumers to signal when more data should be loaded. This is crucial for scenarios where data is fetched in chunks or pages, 
commonly seen in APIs where only a limited number of records are returned per request.

## Usage

```kotlin
fun main() = runBlocking {
    val paginatedFlow = paginatedFlow {
        var i = 1
        var items = listOf(i)
        emit(items)

        onLoadMore {
            items = items.plus(++i)
            emit(items)
        }
    }

    val sharedFlow = paginatedFlow.shareIn(this, Lazily)
    println(sharedFlow.first()) // [1]

    paginatedFlow.loadMore()
    println(sharedFlow.first()) // [1, 2]

    paginatedFlow.loadMore()
    println(sharedFlow.first()) // [1, 2, 3]

    paginatedFlow.loadMore()
    println(sharedFlow.first()) // [1, 2, 3, 4]
}
```

In this example, we create a `PaginatedFlow<List<Int>>` that appends a list after each request. We provide a lambda, similar to the `flow {}` builder, that additionally provides an `onLoadMore` callback. `onLoadMore` is executed every time `loadMore()` is requested.

We then share the flow and await the first value. Finally, we call `loadMore()` multiple times to simulate loading more pages.

*The execution of `onLoadMore` depends on the backpressure strategy in PaginatedFlow implementation.*

## Implementation

Let's dive into the implementation of PaginatedFlow.

```kotlin
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

private class PaginatedFlowImpl<T>(
    private val block: suspend PaginatedFlowCollector<T>.() -> Unit,
) : AbstractFlow<T>(), PaginatedFlow<T> {

    private val loadMoreChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val paginatedCollector = object : PaginatedFlowCollector<T> {
            override suspend fun onLoadMore(action: suspend () -> Unit) {
                loadMoreChannel.collect { action() }
            }

            override suspend fun emit(value: T) {
                collector.emit(value)
            }
        }
        paginatedCollector.block()
    }

    override fun loadMore() {
        loadMoreChannel.tryEmit(Unit)
    }
}
```

In this implementation:

- `PaginatedFlow` is an interface that extends Kotlin's `Flow<T>` interface. It declares a `loadMore()` function to signal when more data needs to be loaded.
- `PaginatedFlowCollector` is another interface that extends `FlowCollector<T>`. It provides an `onLoadMore()` function to handle loading more data asynchronously.
- The `paginatedFlow` function is a builder function that creates instances of `PaginatedFlow`.
- `PaginatedFlowImpl` is the concrete implementation of `PaginatedFlow`. It handles the collection of data and signaling for more data to be loaded.

## Conclusion

I hope you find this idea and implementation as exciting as I do, and that it proves to be a valuable tool in your projects. May PaginatedFlow streamline your pagination handling, making your development journey smoother and more efficient. Here's to building better, more robust applications with Kotlin Coroutines and PaginatedFlow!