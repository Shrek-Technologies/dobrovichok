package ru.dobrovichek.android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object DobrovichekColors {
    val BackgroundCream = Color(0xFFFDF8F5)
    val BackgroundSoftBlue = Color(0xFFE8F6FC)
    val NavyText = Color(0xFF2C3E50)
    val GreySecondary = Color(0xFF7B8794)
    val BluePrimary = Color(0xFF1BA8E0)
    val BlueLight = Color(0xFF6BCFF5)
    val OrangeCoral = Color(0xFFFF8A5C)
    val OrangeSoft = Color(0xFFFFF0E8)
    val PeachSurface = Color(0xFFFFE8DC)
    val MintSoft = Color(0xFFE8F8EE)
    val MintBorder = Color(0xFF5CB88A)
    val SkySoft = Color(0xFFE3F2FD)
    val SkyBorder = Color(0xFF42A5F5)
    val CardBorderSubtle = Color(0xFFE8E0DC)
    val White = Color.White
}

private val DobrovichekLightScheme = lightColorScheme(
    primary = DobrovichekColors.BluePrimary,
    onPrimary = Color.White,
    primaryContainer = DobrovichekColors.SkySoft,
    secondary = DobrovichekColors.OrangeCoral,
    onSecondary = Color.White,
    secondaryContainer = DobrovichekColors.OrangeSoft,
    background = DobrovichekColors.BackgroundCream,
    surface = Color.White,
    onBackground = DobrovichekColors.NavyText,
    onSurface = DobrovichekColors.NavyText,
    outline = DobrovichekColors.CardBorderSubtle,
    error = Color(0xFFE53935)
)

val DobrovichekButtonShape = RoundedCornerShape(28.dp)
val DobrovichekCardShape = RoundedCornerShape(20.dp)

@Composable
fun DobrovichekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DobrovichekLightScheme,
        typography = Typography(),
        content = content
    )
}

@Composable
fun DobrovichekWardBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DobrovichekColors.BackgroundCream,
                        DobrovichekColors.BackgroundSoftBlue.copy(alpha = 0.35f),
                        DobrovichekColors.OrangeSoft.copy(alpha = 0.25f)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            ),
        content = content
    )
}

fun primaryBlueGradient(): Brush = Brush.linearGradient(
    colors = listOf(DobrovichekColors.BluePrimary, DobrovichekColors.BlueLight),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

fun orangeSoftGradient(): Brush = Brush.linearGradient(
    colors = listOf(DobrovichekColors.OrangeCoral, Color(0xFFFFB07A)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(DobrovichekButtonShape)
            .background(primaryBlueGradient())
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SoftOrangeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(DobrovichekButtonShape)
            .background(orangeSoftGradient())
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
