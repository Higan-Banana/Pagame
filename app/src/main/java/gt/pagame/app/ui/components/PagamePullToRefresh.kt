package gt.pagame.app.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/** Connects the reusable gesture UI to its ViewModel. */
@Composable
fun PagameRefreshContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val viewModel: RefreshViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connected by rememberInternetAvailable()
    OperationError(state.error, viewModel::dismissError)
    PagamePullToRefresh(modifier, state.refreshing, { viewModel.refresh(connected) }, content)
}

/** Refreshes from the server without replacing the live listeners or clearing content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagamePullToRefresh(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    // A local flag only animates the gesture while the ViewModel processes the request.
    var gesturePending by remember { mutableStateOf(false) }
    val showRefreshing = refreshing || gesturePending
    LaunchedEffect(showRefreshing) {
        if (!showRefreshing) {
            withFrameNanos { }
            state.animateToHidden()
        }
    }
    PullToRefreshBox(
        modifier = modifier,
        state = state,
        isRefreshing = showRefreshing,
        onRefresh = {
            if (!showRefreshing) {
                gesturePending = true
                scope.launch {
                    try {
                        withFrameNanos { }
                        withFrameNanos { }
                        onRefresh()
                    } finally { gesturePending = false }
                }
            }
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter).semantics {
                    stateDescription = if (showRefreshing) "Actualizando…" else "Desliza hacia abajo para actualizar"
                },
                isRefreshing = showRefreshing,
                state = state,
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )
        },
        content = content
    )
}
