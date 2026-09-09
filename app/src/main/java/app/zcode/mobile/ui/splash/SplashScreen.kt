package app.zcode.mobile.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.ui.components.Wordmark
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    hasConnection: Boolean,
    onFinished: (hasConnection: Boolean) -> Unit,
) {
    LaunchedEffect(hasConnection) {
        delay(700)
        onFinished(hasConnection)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Wordmark()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Unofficial remote client",
                color = Mute,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
            )
        }
    }
}
