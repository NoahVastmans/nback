package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mobappdev.example.nback_cimpl.Screen
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

@Composable
fun ResultScreen(
    vm: GameViewModel,
    navController: NavController
) {
    val score by vm.score.collectAsState()
    val totalMatches by vm.totalMatches.collectAsState()

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("End of the game!", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "You correctly identified $score out of $totalMatches matches.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 32.dp)
            )
            Button(onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }) {
                Text("Play Again")
            }
        }
    }
}
