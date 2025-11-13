package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import kotlin.math.roundToInt

@Composable
fun SettingScreen(
    vm: GameViewModel,
    navController: NavController
) {
    val gameState by vm.gameState.collectAsState()

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("N-Back: ${gameState.nBack}")
                Slider(
                    value = gameState.nBack.toFloat(),
                    onValueChange = { vm.setNBack(it.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3
                )

                Text("Number of Events: ${gameState.numberOfEvents}")
                Slider(
                    value = gameState.numberOfEvents.toFloat(),
                    onValueChange = { vm.setNumberOfEvents(it.roundToInt()) },
                    valueRange = 10f..50f,
                    steps = 39
                )

                Text("Time Between Events: ${gameState.eventInterval / 1000}s")
                Slider(
                    value = gameState.eventInterval.toFloat(),
                    onValueChange = { vm.setEventInterval(it.toLong()) },
                    valueRange = 1000f..5000f,
                    steps = 3
                )

                Text("Number of Letters (Audio): ${gameState.numberOfCombinations}")
                Slider(
                    value = gameState.numberOfCombinations.toFloat(),
                    onValueChange = { vm.setNumberOfCombinations(it.roundToInt()) },
                    valueRange = 5f..26f,
                    steps = 20
                )

                Text("Grid Size (Visual): ${gameState.gridSize}x${gameState.gridSize}")
                Slider(
                    value = gameState.gridSize.toFloat(),
                    onValueChange = { vm.setGridSize(it.roundToInt()) },
                    valueRange = 3f..5f,
                    steps = 1
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { vm.resetSettings() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reset All Settings")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = { navController.popBackStack() }
            ) {
                Text("Back")
            }
        }
    }
}

@Preview
@Composable
fun SettingScreenPreview() {
    Surface {
        SettingScreen(
            vm = FakeVM(),
            navController = rememberNavController()
        )
    }
}
