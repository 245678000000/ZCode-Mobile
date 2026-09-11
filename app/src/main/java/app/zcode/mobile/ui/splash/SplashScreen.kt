package app.zcode.mobile.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.ui.components.ZGlyph
import app.zcode.mobile.ui.theme.ZTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    hasConnection: Boolean,
    onFinished: (hasConnection: Boolean) -> Unit,
) {
    val c = ZTheme.colors
    LaunchedEffect(hasConnection) {
        delay(500)
        onFinished(hasConnection)
    }
    Box(modifier = Modifier.fillMaxSize().background(c.surface), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ZGlyph(size = 56.dp)
            Spacer(Modifier.height(18.dp))
            Text("ZCode", color = c.fg, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}
