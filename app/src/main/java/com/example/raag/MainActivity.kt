package com.example.raag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.raag.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.bindToService(this)

        setContent {
            MusicPlayerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MusicPlayerApp(viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.unbindFromService(this)
        super.onDestroy()
    }
}

@Composable
fun MusicPlayerApp(viewModel: MusicPlayerViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "songList") {
        composable("songList") {
            SongListScreen(navController = navController, viewModel = viewModel)
        }
        composable(
            "player/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong("songId") ?: 0L
            PlayerScreen(
                songId = songId,
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}
