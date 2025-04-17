package com.example.mc_a3.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.mc_a3.model.LocationData
import kotlin.math.abs

@Composable
fun SignalMatrixVisualization(
    locationData: LocationData,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val signalMatrix = locationData.signalMatrix

    Column(modifier = modifier) {
        Text(
            text = "Signal Matrix: ${locationData.name}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // The minimum signal level that we expect (-100 dBm is very weak)
            val minSignalLevel = -100
            // The maximum signal level we expect (-40 dBm is very strong)
            val maxSignalLevel = -40
            
            // The width of each bar
            val barWidth = canvasWidth / signalMatrix.size
            
            // Draw each signal level as a bar
            signalMatrix.forEachIndexed { index, signalLevel ->
                val normalizedLevel = normalizeSignalLevel(signalLevel, minSignalLevel, maxSignalLevel)
                val barHeight = canvasHeight * normalizedLevel
                
                drawBar(
                    index = index,
                    barWidth = barWidth,
                    barHeight = barHeight,
                    canvasHeight = canvasHeight,
                    color = barColor
                )
            }
        }
        
        // Display range information
        val min = signalMatrix.minOrNull() ?: 0
        val max = signalMatrix.maxOrNull() ?: 0
        val avg = signalMatrix.average().toInt()
        
        Text(
            text = "Signal Range: Min: $min dBm, Max: $max dBm, Avg: $avg dBm",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun normalizeSignalLevel(signalLevel: Int, minSignalLevel: Int, maxSignalLevel: Int): Float {
    return (abs(signalLevel) - abs(maxSignalLevel)).toFloat() / 
           (abs(minSignalLevel) - abs(maxSignalLevel))
}

private fun DrawScope.drawBar(
    index: Int,
    barWidth: Float,
    barHeight: Float,
    canvasHeight: Float,
    color: Color
) {
    val left = index * barWidth
    val top = canvasHeight - barHeight
    
    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(barWidth * 0.9f, barHeight)
    )
}