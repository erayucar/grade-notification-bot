import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        var isRunning = remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(isRunning) {
            scope.launch {
                execute(isRunning.value)

            }
        }
        Column {
            Text("Sistem başladı... ")
            Text("15 dakikada bir kontrol edecektir.")
            Text(" Kapatmak istiyorsanız lütfen pencereyi kapatınız")
        }

    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

