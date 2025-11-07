package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.MatchResult

@Composable
fun GameScreen(
    vm: GameViewModel,
    navController: NavController
) {
    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()
    val isGameOver by vm.isGameOver.collectAsState()
    val matchResult by vm.matchResult.collectAsState()
    val progress by vm.progress.collectAsState()

    if (isGameOver) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        vm.startGame()
    }

    val buttonColors = when (matchResult) {
        MatchResult.CORRECT -> ButtonDefaults.buttonColors(containerColor = Color.Green)
        MatchResult.INCORRECT -> ButtonDefaults.buttonColors(containerColor = Color.Red)
        else -> ButtonDefaults.buttonColors()
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressAnimation")

    Scaffold(
        snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .padding(horizontal = 16.dp)
                    .height(12.dp)
            )
            Text(
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                text = "Score = $score",
                style = MaterialTheme.typography.headlineLarge
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    items(9) { tileIndex ->
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .aspectRatio(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (gameState.gameType == GameType.Visual && tileIndex + 1 == gameState.eventValue) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            // Content of the square
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { vm.checkMatch() },
                    enabled = gameState.gameType == GameType.Audio || gameState.gameType == GameType.AudioVisual,
                    colors = buttonColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sound_on),
                        contentDescription = "Sound",
                        modifier = Modifier.size(80.dp)
                    )
                }
                Button(
                    onClick = { vm.checkMatch() },
                    enabled = gameState.gameType == GameType.Visual || gameState.gameType == GameType.AudioVisual,
                    colors = buttonColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.visual),
                        contentDescription = "Visual",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    Surface(){
        GameScreen(FakeVM(), navController = rememberNavController())
    }
}
