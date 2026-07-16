package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationTriggered = true
    }

    val maxValue = data.maxOfOrNull { it.second } ?: 1f
    val scale = if (maxValue > 0) maxValue else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val progress by animateFloatAsState(
                    targetValue = if (animationTriggered) item.second / scale else 0f,
                    animationSpec = tween(durationMillis = 800)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "R$ %.0f".format(item.second),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .width(16.dp)
                            .background(
                                color = barColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progress)
                                .background(
                                    color = barColor,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.first,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF4CAF50), // Green
        Color(0xFFE91E63), // Pink
        Color(0xFFFFC107), // Amber/Yellow
        Color(0xFF00BCD4), // Cyan
        Color(0xFF9C27B0), // Purple
        Color(0xFF8BC34A), // Lime
        Color(0xFFE040FB), // Light Purple
        Color(0xFF00E676), // Bright Green
        Color(0xFFFF1744), // Red
        Color(0xFF2979FF), // Electric Blue
        Color(0xFFFFEA00), // Sunny Yellow
        Color(0xFF00B0FF), // Neon Blue
        Color(0xFFAA00FF), // Deep Violet
        Color(0xFFFF6D00), // Orange
        Color(0xFF1DE9B6), // Mint/Teal
        Color(0xFFD500F9), // Fuchsia
        Color(0xFF76FF03), // Neon Lime
        Color(0xFF3D5AFE), // Royal Blue
        Color(0xFFFF3D00), // Scarlet
        Color(0xFF00E5FF), // Aqua/Sky Blue
        Color(0xFFF50057), // Rose
        Color(0xFF651FFF), // Indigo
        Color(0xFF00C853)  // Emerald
    )
) {
    val total = data.sumOf { it.second.toDouble() }.toFloat()
    if (total <= 0) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Sem dados de participação", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationTriggered = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .weight(1.2f)
        ) {
            var startAngle = 0f
            data.forEachIndexed { index, pair ->
                val sweepAngle = (pair.second / total) * 360f
                val color = colors[index % colors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedProgress,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1.8f),
            verticalArrangement = Arrangement.Center
        ) {
            data.forEachIndexed { index, pair ->
                val percent = (pair.second / total) * 100f
                val color = colors[index % colors.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, shape = RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${pair.first}: %.1f%%".format(percent),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalBarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxValue = data.maxOfOrNull { it.second } ?: 1f
    val scale = if (maxValue > 0) maxValue else 1f

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationTriggered = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { item ->
            val progress by animateFloatAsState(
                targetValue = if (animationTriggered) item.second / scale else 0f,
                animationSpec = tween(durationMillis = 800)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.first,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(90.dp),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .background(
                            color = barColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(7.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                color = barColor,
                                shape = RoundedCornerShape(7.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "%.0f".format(item.second),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
