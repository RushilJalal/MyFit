package com.example.myfit_new

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun StepProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 30f,
    gradientColors: List<Color> = listOf(
        Color(0xFF4A148C),  // Deep Purple
        Color(0xFFD81B60),  // Mulberry
        Color(0xFF8E24AA),  // Medium Violet
        Color(0xFFAB47BC),  // Light Violet
        Color(0xFFCE93D8),  // Pastel Violet
        Color(0xFF4A148C)   // Deep Purple (to match the start)
    ),
    backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f) // Use Material theme colors
) {
    Canvas(
        modifier = modifier
    ) {
        val canvasSize = size.minDimension
        val radius = (canvasSize - strokeWidth) / 2
        val startAngle = 270f  // Start from top
        val sweepAngle = progress * 360f

        // Draw background circle
        drawArc(
            color = backgroundColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset((size.width - canvasSize) / 2, (size.height - canvasSize) / 2),
            size = Size(canvasSize, canvasSize),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw progress arc with gradient
        drawArc(
            brush = Brush.sweepGradient(
                colors = gradientColors,
                center = Offset(size.width / 2, size.height / 2)
            ),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset((size.width - canvasSize) / 2, (size.height - canvasSize) / 2),
            size = Size(canvasSize, canvasSize),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}