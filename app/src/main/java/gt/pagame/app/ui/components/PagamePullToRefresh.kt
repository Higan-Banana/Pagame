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
import gt.pagame.app.data.repository.PagameRepository
import gt.pagame.app.data.repository.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

/** Refreshes from the server without replacing the live listeners or clearing content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagamePullToRefresh(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    val connected by rememberInternetAvailable()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    OperationError(error) { error = null }
    PullToRefreshBox(
        modifier = modifier,
        state = state,
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing) {
                refreshing = true
                error = null
                scope.launch {
                    try {
                        // Let Compose observe true before a very fast request can finish.
                        withFrameNanos { }
                        withFrameNanos { }
                        if (connected) PagameRepository.instance.refresh()
                        else error = "Sin conexión. Conéctate a internet para actualizar."
                    } catch (e: TimeoutCancellationException) {
                        error = "La actualización tardó demasiado. Revisa tu conexión e intenta de nuevo."
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = userMessage(e)
                    } finally {
                        refreshing = false
                        // Hide the gesture indicator too, including fast completions and errors.
                        withFrameNanos { }
                        state.animateToHidden()
                    }
                }
            }
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter).semantics {
                    stateDescription = if (refreshing) "Actualizando…" else "Desliza hacia abajo para actualizar"
                },
                isRefreshing = refreshing,
                state = state,
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )
        },
        content = content
    )
}
