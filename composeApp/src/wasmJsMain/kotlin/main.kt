import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement

/**
 * Example taken from here: https://webglfundamentals.org/webgl/lessons/webgl-fundamentals.html
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
//    CanvasBasedWindow(canvasElementId = "ComposeTarget") { App() }

    // window.addEventListener("load",
    //    { event ->

    val spinningWheelComponent = document.getElementById("wheel-component")?.let { canvas ->
        val webGlContext = detectWebGlContext(canvas as HTMLCanvasElement)
        webGlContext?.let { renderingContext ->
            SpinningWheelComponent(renderingContext)
        }
    }

    if (spinningWheelComponent != null) {
        spinningWheelComponent.render()
        document.getElementById("x-position")?.let {
            val xSlider = it as HTMLInputElement

            xSlider.addEventListener("input") {
                spinningWheelComponent.translate(xSlider.value.toFloatOrNull(), null)
            }
        }

        document.getElementById("y-position")?.let {
            val ySlider = it as HTMLInputElement

            ySlider.addEventListener("input") {
                spinningWheelComponent.translate(null, ySlider.value.toFloatOrNull())
            }
        }

        document.getElementById("rotation")?.let {
            val rotationSlider = it as HTMLInputElement

            rotationSlider.addEventListener("input") {
                spinningWheelComponent.rotate(rotationSlider.value.toFloatOrNull())
            }
        }

    }


}
