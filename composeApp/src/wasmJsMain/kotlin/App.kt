import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.khronos.webgl.WebGLRenderingContext
import org.w3c.dom.HTMLCanvasElement

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(spinningWheelComponent: SpinningWheelComponent) {
    MaterialTheme {

//        var showContent by remember { mutableStateOf(false) }
//        val greeting = remember { Greeting().greet() }
//        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = { showContent = !showContent }) {
//                Text("Click me!")
//            }
//            AnimatedVisibility(showContent) {
//                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//                    Image(painterResource("compose-multiplatform.xml"), null)
//                    Text("Compose: $greeting")
//                }
//            }
//        }

        Column(modifier = Modifier.height(50.dp)) {
            Text("Rotation angle: ${spinningWheelComponent.rotationAngleInRadians}")
        }

    }
}

    fun runWebGl() {
       window.addEventListener("load"
       ) { event ->
           val canvas = document.createElement("canvas") as HTMLCanvasElement
           detectWebGlContext(canvas)


       }

    }

fun detectWebGlContext(canvas: HTMLCanvasElement): WebGLRenderingContext? {
    return detectWebGlContext("webgl", canvas)?: detectWebGlContext("experimental-webl", canvas)
}

private fun detectWebGlContext(contextId: String, canvas: HTMLCanvasElement): WebGLRenderingContext? {
    return canvas.getContext(contextId) as WebGLRenderingContext?
}