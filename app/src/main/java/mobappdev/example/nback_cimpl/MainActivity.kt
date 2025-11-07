package mobappdev.example.nback_cimpl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.screens.GameScreen
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.screens.ResultScreen
import mobappdev.example.nback_cimpl.ui.screens.SettingScreen
import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Game : Screen("game")
    object Settings : Screen("settings")
    object Results : Screen("results")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NBack_CImplTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val gameViewModel: GameVM = viewModel(
                        factory = GameVM.Factory
                    )
                    NBackApp(gameViewModel)
                }
            }
        }
    }
}

@Composable
fun NBackApp(vm: GameVM) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(vm = vm, navController = navController)
        }
        composable(Screen.Game.route) {
            GameScreen(vm = vm, navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingScreen(vm = vm, navController = navController)
        }
        composable(Screen.Results.route) {
            ResultScreen(vm = vm, navController = navController)
        }
    }
}
