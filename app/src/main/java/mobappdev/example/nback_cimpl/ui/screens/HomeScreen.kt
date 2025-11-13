package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.Screen
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: GameViewModel,
    navController: NavController
) {
    val highscore by vm.highscore.collectAsState()
    val gameState by vm.gameState.collectAsState()

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(32.dp),
                text = "High-Score = $highscore",
                style = MaterialTheme.typography.headlineLarge
            )

            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("N-Back: ${gameState.nBack}")
                Slider(
                    value = gameState.nBack.toFloat(),
                    onValueChange = { vm.setNBack(it.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Number of Events: ${gameState.numberOfEvents}")
                Text("Time Between Events: ${gameState.eventInterval / 1000}s")
            }

            Button(onClick = { navController.navigate(Screen.Settings.route) }) {
                Text("More Settings")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Start Game",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                vm.resetGame()
                                vm.setGameType(GameType.Audio)
                                navController.navigate(Screen.Game.route)
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.sound_on),
                                contentDescription = "Sound",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text("Audio Mode")
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                vm.resetGame()
                                vm.setGameType(GameType.Visual)
                                navController.navigate(Screen.Game.route)
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.visual),
                                contentDescription = "Visual",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text("Visual Mode")
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            vm.resetGame()
                            vm.setGameType(GameType.AudioVisual)
                            navController.navigate(Screen.Game.route)
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                    ) {
                        Text("Dual", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Dual Mode")
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    Surface(){
        HomeScreen(FakeVM(), navController = rememberNavController())
    }
}
