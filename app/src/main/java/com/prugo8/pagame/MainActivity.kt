package com.prugo8.pagame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.prugo8.pagame.navigation.PagameNavGraph
import com.prugo8.pagame.ui.theme.PagameTheme
import com.prugo8.pagame.viewmodel.PagameViewModel

class  MainActivity : ComponentActivity() {
    private val viewModel: PagameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PagameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PagameNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
