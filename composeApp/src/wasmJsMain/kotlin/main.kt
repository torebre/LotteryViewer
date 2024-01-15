import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.browser.document
import org.khronos.webgl.*
import org.w3c.dom.HTMLCanvasElement

/**
 * Example taken from here: https://webglfundamentals.org/webgl/lessons/webgl-fundamentals.html
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
//    CanvasBasedWindow(canvasElementId = "ComposeTarget") { App() }

    // window.addEventListener("load",
    //    { event ->
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    with(canvas) {
        width = 500
        height = 500
    }

    // TODO Only here for testing
    document.children.item(0)?.appendChild(canvas)

    val webGlContext = detectWebGlContext(canvas)

    webGlContext?.let { renderingContext ->
        val vertexShaderSource = """
            // an attribute will receive data from a buffer
            attribute vec4 a_position;
 
            // all shaders have a main function
            void main() {
                // gl_Position is a special variable a vertex shader
                // is responsible for setting
                gl_Position = a_position;
            }
        """.trimIndent()

        val fragmentShaderSource = """
             // fragment shaders don't have a default precision so we need
            // to pick one. mediump is a good default
            precision mediump float;
 
            void main() {
                // gl_FragColor is a special variable a fragment shader
                // is responsible for setting
                gl_FragColor = vec4(1, 0, 0.5, 1); // return reddish-purple
            } 
            
        """.trimIndent()

        val vertexShader = createShader(renderingContext, WebGLRenderingContext.VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = createShader(renderingContext, WebGLRenderingContext.FRAGMENT_SHADER, fragmentShaderSource)

        if (vertexShader != null && fragmentShader != null) {
            val program = createProgram(renderingContext, vertexShader, fragmentShader)

            val positionAttributeLocation = renderingContext.getAttribLocation(program, "a_position")
            val positionBuffer = renderingContext.createBuffer()

            renderingContext.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, positionBuffer)

            val temp2 = Float32Array(6)
            temp2[0] = 0.0f
            temp2[1] = 0.0f
            temp2[2] = 0.0f
            temp2[3] = 0.5f
            temp2[4] = 0.7f
            temp2[5] = 0.0f

            renderingContext.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
                temp2,
                WebGLRenderingContext.STATIC_DRAW)

            renderingContext.viewport(0, 0, renderingContext.canvas.width, renderingContext.canvas.height)

            renderingContext.clearColor(0.0f, 0.0f, 0.0f, 0.0f)
            renderingContext.clear(WebGLRenderingContext.COLOR_BUFFER_BIT)

            renderingContext.useProgram(program)
            renderingContext.enableVertexAttribArray(positionAttributeLocation)

            renderingContext.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, positionBuffer)

            val size = 2
            val type = WebGLRenderingContext.FLOAT
            val normalize = false
            val stride = 0
            val offset = 0

            renderingContext.vertexAttribPointer(positionAttributeLocation, size, type, normalize, stride, offset)

            val primitiveType = WebGLRenderingContext.TRIANGLES
            val count =3
            renderingContext.drawArrays(primitiveType, offset, count)

        }

    }

}



private fun createProgram(
    renderingContext: WebGLRenderingContext,
    vertexShader: WebGLShader,
    fragmentShader: WebGLShader
): WebGLProgram? {
    val program = renderingContext.createProgram()
    renderingContext.attachShader(program, vertexShader)
    renderingContext.attachShader(program, fragmentShader)
    renderingContext.linkProgram(program)
    val success = renderingContext.getProgramParameter(program, WebGLRenderingContext.LINK_STATUS)
    if (success != null) {
        return program
    }

    println(renderingContext.getProgramInfoLog(program))
    renderingContext.deleteProgram(program)

    return null
}

private fun createShader(
    renderingContext: WebGLRenderingContext,
    vertexShader: Int,
    vertexShaderSource: String
): WebGLShader? {
    val shader = renderingContext.createShader(vertexShader)
    renderingContext.shaderSource(shader, vertexShaderSource)
    renderingContext.compileShader(shader)
    val success = renderingContext.getShaderParameter(shader, WebGLRenderingContext.COMPILE_STATUS)
    if (success != null) {
        return shader
    }

    println(renderingContext.getShaderInfoLog(shader))
    renderingContext.deleteShader(shader)

    return null
}
