import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement

/**
 * Example taken from here: https://webglfundamentals.org/webgl/lessons/webgl-fundamentals.html
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val numberOfSlices = 20

    val spinningWheelComponent = document.getElementById("wheel-component")?.let { canvas ->
        val webGlContext = detectWebGlContext(canvas as HTMLCanvasElement)
        webGlContext?.let { renderingContext ->
            SpinningWheelComponent(numberOfSlices, renderingContext)
        }
    }

//    spinningWheelComponent?.let {
//        CanvasBasedWindow(canvasElementId = "ComposeTarget") { App(it) }
//    }

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

        document.getElementById("scaling")?.let {
            val scalingSlider = it as HTMLInputElement

            scalingSlider.addEventListener("scaling") {
                spinningWheelComponent.scale(scalingSlider.value.toFloatOrNull())
            }
        }

        document.getElementById("rotation-angle")?.let {
            spinningWheelComponent.addDebugListener(object : DebugListener {

                override fun rotationChanged(rotationAngle: Float) {
                    it.textContent = "Rotation angle: ${spinningWheelComponent.rotationAngleInRadians}"
                }

                override fun markerUpdated(
                    markerPointOnWheel: Point2D,
                    markerEnd: Point2D
                ) {
                    // Do nothing
                }

            })
        }

        document.getElementById("marker-debug")?.let {
            spinningWheelComponent.addDebugListener(object: DebugListener {
                override fun rotationChanged(rotationAngle: Float) {
                    // Do nothing
                }

                override fun markerUpdated(
                    markerPointOnWheel: Point2D,
                    markerEnd: Point2D
                ) {
                    it.innerHTML = """
                        Markerpoint on wheel: $markerPointOnWheel
                        <br/>
                        Marker end: $markerEnd
                        """
                }
            })
        }

        document.getElementById("spin-button")?.let {
            val buttonElement = it as HTMLButtonElement

            buttonElement.addEventListener("click") { _ ->
                spinningWheelComponent.doSpin()
            }

        }


    }


}
