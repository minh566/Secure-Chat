package com.securechat.ui.common

import com.securechat.domain.model.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun <T> Flow<String>.toSearchResourceState(
    scope: CoroutineScope,
    debounceMs: Long = 300L,
    search: (String) -> Flow<Resource<List<T>>>
): StateFlow<Resource<List<T>>> {
    return map { it.trim() }
        .debounce(debounceMs)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(Resource.Success(emptyList()))
            } else {
                search(query)
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Success(emptyList())
        )
}

