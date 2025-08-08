package com.example.taptarget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TapTargetGame()
                }
            }
        }
    }
}

@Composable
fun TapTargetGame() {
    var isRunning by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var timeLeftMs by remember { mutableStateOf(30_000L) }

    val targetRadiusDp: Dp = 24.dp

    var targetCenter by remember { mutableStateOf(Offset(200f, 200f)) }
    var velocity by remember { mutableStateOf(Offset(4f, 6f)) }

    var canvasSize by remember { mutableStateOf(Offset(1080f, 1920f)) }

    val density = LocalDensity.current

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        val frameDelayMs = 16L
        while (isRunning && timeLeftMs > 0) {
            val radiusPx = with(density) { targetRadiusDp.toPx() }
            val next = targetCenter + velocity
            var vx = velocity.x
            var vy = velocity.y
            val maxX = canvasSize.x - radiusPx
            val maxY = canvasSize.y - radiusPx
            val minX = radiusPx
            val minY = radiusPx
            var newX = next.x
            var newY = next.y
            if (newX < minX) { newX = minX; vx = -vx }
            if (newX > maxX) { newX = maxX; vx = -vx }
            if (newY < minY) { newY = minY; vy = -vy }
            if (newY > maxY) { newY = maxY; vy = -vy }
            targetCenter = Offset(newX, newY)
            velocity = Offset(vx, vy)

            delay(frameDelayMs)
            timeLeftMs -= frameDelayMs
        }
        isRunning = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Score: ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            val seconds = (timeLeftMs / 1000).coerceAtLeast(0)
            Text(text = "Time: s", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GameCanvas(
                targetCenter = targetCenter,
                targetRadiusDp = targetRadiusDp,
                onCanvasSized = { w, h -> canvasSize = Offset(w, h) },
                onTap = { tap ->
                    if (!isRunning) return@GameCanvas
                    val radiusPx = with(density) { targetRadiusDp.toPx() }
                    val dist = hypot((tap.x - targetCenter.x), (tap.y - targetCenter.y))
                    if (dist <= radiusPx) {
                        score += 1
                        val nx = Random.nextFloat().coerceIn(0f, 1f) * (canvasSize.x - 2 * radiusPx) + radiusPx
                        val ny = Random.nextFloat().coerceIn(0f, 1f) * (canvasSize.y - 2 * radiusPx) + radiusPx
                        targetCenter = Offset(nx, ny)
                        val speed = 4f + score.coerceAtMost(20) * 0.3f
                        val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
                        velocity = Offset(speed * kotlin.math.cos(angle), speed * kotlin.math.sin(angle))
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                score = 0
                timeLeftMs = 30_000
                isRunning = true
            }) { Text(if (isRunning) "Restart" else "Start") }
            Button(onClick = { isRunning = false }) { Text("Stop") }
        }
    }
}

@Composable
private fun GameCanvas(
    targetCenter: Offset,
    targetRadiusDp: Dp,
    onCanvasSized: (widthPx: Float, heightPx: Float) -> Unit,
    onTap: (Offset) -> Unit,
) {
    var lastSize by remember { mutableStateOf(Offset.Zero) }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            onTap(down.position)
                        }
                    }
                }
            }
    ) {
        if (size.width != lastSize.x || size.height != lastSize.y) {
            lastSize = Offset(size.width, size.height)
            onCanvasSized(size.width, size.height)
        }

        // Background
        drawRect(color = Color(0xFFF2F2F2))

        // Target
        val radiusPx = targetRadiusDp.toPx()
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = radiusPx,
            center = targetCenter
        )
        drawCircle(
            color = Color.White,
            radius = radiusPx * 0.5f,
            center = targetCenter
        )
    }
}
