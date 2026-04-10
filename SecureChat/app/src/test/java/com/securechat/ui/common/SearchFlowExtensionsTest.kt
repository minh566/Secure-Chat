package com.securechat.ui.common

import com.securechat.domain.model.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchFlowExtensionsTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun toSearchResourceState_happyPath_emitsSearchResultAfterDebounce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val queryFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

        val state = queryFlow.toSearchResourceState(scope, debounceMs = 300) { query ->
            flow { emit(Resource.Success(listOf(query))) }
        }

        scope.launch {
            // Keep collection active for stateIn WhileSubscribed.
            state.collect { }
        }
        advanceUntilIdle()

        queryFlow.tryEmit("tan")
        advanceTimeBy(301)
        advanceUntilIdle()

        val value = state.value
        assertTrue(value is Resource.Success)
        assertEquals(listOf("tan"), (value as Resource.Success<List<String>>).data)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun toSearchResourceState_failure_previousQueryCancelledByFlatMapLatest() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val queryFlow = MutableSharedFlow<String>(extraBufferCapacity = 2)

        val state = queryFlow.toSearchResourceState(scope, debounceMs = 300) { query ->
            flow {
                if (query == "first") {
                    delay(1000)
                }
                emit(Resource.Success(listOf(query)))
            }
        }

        scope.launch {
            // keep collection active for stateIn WhileSubscribed
            state.collect { }
        }
        advanceUntilIdle()

        queryFlow.tryEmit("first")
        advanceTimeBy(301)
        queryFlow.tryEmit("second")
        advanceTimeBy(301)
        advanceUntilIdle()

        val value = state.value
        assertTrue(value is Resource.Success)
        assertEquals(listOf("second"), (value as Resource.Success<List<String>>).data)
    }
}

