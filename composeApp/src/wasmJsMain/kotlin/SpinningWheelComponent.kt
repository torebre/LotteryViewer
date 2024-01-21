import org.khronos.webgl.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SpinningWheelComponent(private val numberOfSlices: Int, private val renderingContext: WebGLRenderingContext) {
    private var positionAttributeLocation: Int = 0
    private var resolutionUniformLocation: WebGLUniformLocation? = null
    private var colorLocation: WebGLUniformLocation? = null
    private var matrixLocation: WebGLUniformLocation? = null
    private var positionBuffer: WebGLBuffer? = null
    private var program: WebGLProgram? = null

    private val translationMatrix = createTranslationMatrix()
    private var origin = Point2D(0f, 0f)
    private var rotationMatrix = createRotationMatrix()
    private val scaleMatrix = createScalingMatrix()

    private val colorArray: List<Float32Array>

    // https://webglfundamentals.org/webgl/lessons/webgl-points-lines-triangles.html
    private val primitiveType = WebGLRenderingContext.TRIANGLES
    private val radius = 100f

    private val seed = Random.nextInt()
    private val random = Random(seed)


    init {
        val vertexShader = createShader(
            renderingContext,
            WebGLRenderingContext.VERTEX_SHADER,
            getVertexShader()
        )
        val fragmentShader = createShader(
            renderingContext,
            WebGLRenderingContext.FRAGMENT_SHADER,
            getFragmentShader()
        )

        if (vertexShader != null && fragmentShader != null) {
            createProgram(renderingContext, vertexShader, fragmentShader)?.let {
                program = it
                positionBuffer = renderingContext.createBuffer()
                setup()
//                renderingContext.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, positionBuffer)

            }
        }

        colorArray = createColorArray(numberOfSlices)

    }

    private fun setup() {
        renderingContext.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, positionBuffer)
        val origin = Point2D(
            renderingContext.canvas.width.toFloat() / 2,
            renderingContext.canvas.height.toFloat() / 2
        )
        this.origin = origin
        this.rotationMatrix = createRotationMatrix()

        // TODO Add handling of resizing the canvas
        renderingContext.viewport(0, 0, renderingContext.canvas.width, renderingContext.canvas.height)
        renderingContext.clearColor(0.0f, 0.0f, 0.0f, 0.0f)
        renderingContext.clear(WebGLRenderingContext.COLOR_BUFFER_BIT)

        renderingContext.useProgram(program)
        renderingContext.enableVertexAttribArray(positionAttributeLocation)

        renderingContext.uniform2f(
            resolutionUniformLocation,
            renderingContext.canvas.width.toFloat(),
            renderingContext.canvas.height.toFloat()
        )

    }

    fun render() {
        val sliceAngle = (2 * PI / numberOfSlices.toFloat()).toFloat()
        var currentStartAngle = 0f

        for (i in 0 until numberOfSlices) {
            val slice = createSlice(currentStartAngle, sliceAngle, origin)
            currentStartAngle += sliceAngle

            val points = Float32Array(2 * slice.size).also { it.addValues(slice) }

            println("Test50: ${points.length}")
            println("Test51: $currentStartAngle")
            println("Test53: $origin")
            println("Test54: $points")

            renderingContext.bufferData(
                WebGLRenderingContext.ARRAY_BUFFER,
                points,
                WebGLRenderingContext.STATIC_DRAW
            )

            renderingContext.uniform4fv(colorLocation, colorArray[i])

            val size = 2
            val type = WebGLRenderingContext.FLOAT
            val normalize = false
            val stride = 0
            val offset = 0
            renderingContext.vertexAttribPointer(positionAttributeLocation, size, type, normalize, stride, offset)

            val matrix = multiplyMatrices(translationMatrix, createTranslationMatrix(origin), rotationMatrix, createTranslationMatrix(Point2D(-origin.xCoord, -origin.yCoord)), scaleMatrix)



//            val matrix = multiplyMatrices(translationMatrix)
            renderingContext.uniformMatrix3fv(matrixLocation, false, matrix)

            val count = 6
            renderingContext.drawArrays(primitiveType, offset, count)
        }
    }

    private fun createColorArray(numberOfSlices: Int): List<Float32Array> {
        return (0 until numberOfSlices).map {
            Float32Array(4).also {
                it[0] = random.nextFloat()
                it[1] = random.nextFloat()
                it[2] = random.nextFloat()
                it[3] = 1f
            }
        }.toList()
    }

    private fun createScalingMatrix(scaling: Pair<Float, Float> = Pair(1f, 1f)): Float32Array {
        return Float32Array(9).also {
            it.addValues(floatArrayOf(scaling.first, 0f, 0f, 0f, scaling.second, 0f, 0f, 0f, 1f))
        }
    }

    private fun createRotationMatrix(angleInRadians: Float = 0f): Float32Array {
        val c = cos(angleInRadians)
        val s = sin(angleInRadians)

        return Float32Array(9).also {
            it.addValues(floatArrayOf(c, -s, 0f, s, c, 0f, 0f, 0f, 1f))
        }
    }

//    private fun createRotationMatrix(angleInRadians: Float = 0f): Float32Array {
//        val c = cos(angleInRadians)
//        val s = sin(angleInRadians)
//
//        return Float32Array(9).also {
//            it.addValues(
//                floatArrayOf(
//                    c, -s, -origin.xCoord * c + origin.yCoord * s + origin.xCoord,
//                    s, c, -origin.xCoord * s - origin.yCoord + c + origin.yCoord,
//                    0f, 0f, 1f
//                )
//            )
//        }
//    }

    private fun createTranslationMatrix(translation: Point2D = Point2D(0f, 0f)): Float32Array {
        return Float32Array(9).also {
            it.addValues(
                floatArrayOf(
                    1f,
                    0f,
                    0f,
                    0f,
                    1f,
                    0f,
                    translation.xCoord,
                    translation.yCoord,
                    1f
                )
            )
        }
    }


    private fun createProgram(
        renderingContext: WebGLRenderingContext,
        vertexShader: WebGLShader,
        fragmentShader: WebGLShader
    ): WebGLProgram? {
        val program = renderingContext.createProgram()
        with(renderingContext) {
            attachShader(program, vertexShader)
            attachShader(program, fragmentShader)
            linkProgram(program)

            val success = getProgramParameter(program, WebGLRenderingContext.LINK_STATUS)

            if (success != null) {
                positionAttributeLocation = getAttribLocation(program, "a_position")
                resolutionUniformLocation = getUniformLocation(program, "u_resolution")
                colorLocation = getUniformLocation(program, "u_color")
                matrixLocation = getUniformLocation(program, "u_matrix")

                return program
            }

            deleteProgram(program)
        }

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

        renderingContext.deleteShader(shader)

        return null
    }


    private fun getVertexShader() = """
                  attribute vec2 a_position;
                 
                  uniform vec2 u_resolution;
                  uniform mat3 u_matrix;
                 
                  void main() {
                    vec2 position = (u_matrix * vec3(a_position, 1)).xy;
                  
                    // convert the position from pixels to 0.0 to 1.0
                    vec2 zeroToOne = position / u_resolution;
                 
                    // convert from 0->1 to 0->2
                    vec2 zeroToTwo = zeroToOne * 2.0;
                 
                    // convert from 0->2 to -1->+1 (clip space)
                    vec2 clipSpace = zeroToTwo - 1.0;
                 
                    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);
                  }
        """.trimIndent()

    private fun getFragmentShader() = """
            precision mediump float;
            uniform vec4 u_color;
 
            void main() {
                gl_FragColor = u_color;
            } 
        """.trimIndent()

    private fun multiplyMatrices(vararg matrices: Float32Array): Float32Array {
        return matrices.reduce { a, b ->
            multiplyMatrices(a, b)
        }
    }

    private fun multiplyMatrices(a: Float32Array, b: Float32Array): Float32Array {
        val a00 = a[0 * 3 + 0]
        val a01 = a[0 * 3 + 1]
        val a02 = a[0 * 3 + 2]
        val a10 = a[1 * 3 + 0]
        val a11 = a[1 * 3 + 1]
        val a12 = a[1 * 3 + 2]
        val a20 = a[2 * 3 + 0]
        val a21 = a[2 * 3 + 1]
        val a22 = a[2 * 3 + 2]

        val b00 = b[0 * 3 + 0]
        val b01 = b[0 * 3 + 1]
        val b02 = b[0 * 3 + 2]
        val b10 = b[1 * 3 + 0]
        val b11 = b[1 * 3 + 1]
        val b12 = b[1 * 3 + 2]
        val b20 = b[2 * 3 + 0]
        val b21 = b[2 * 3 + 1]
        val b22 = b[2 * 3 + 2]

        return Float32Array(9).also {
            it.addValues(
                floatArrayOf(
                    b00 * a00 + b01 * a10 + b02 * a20,
                    b00 * a01 + b01 * a11 + b02 * a21,
                    b00 * a02 + b01 * a12 + b02 * a22,
                    b10 * a00 + b11 * a10 + b12 * a20,
                    b10 * a01 + b11 * a11 + b12 * a21,
                    b10 * a02 + b11 * a12 + b12 * a22,
                    b20 * a00 + b21 * a10 + b22 * a20,
                    b20 * a01 + b21 * a11 + b22 * a21,
                    b20 * a02 + b21 * a12 + b22 * a22,
                )
            )
        }
    }

    fun translate(xTranslate: Float?, yTranslate: Float?) {
        if (xTranslate != null) {
            translationMatrix[6] = xTranslate
        }
        if (yTranslate != null) {
            translationMatrix[7] = yTranslate
        }

        render()
    }

    fun scale(scaleFactor: Float?) {
        scaleFactor?.let {
            scaleMatrix[0] = scaleFactor
            scaleMatrix[4] = scaleFactor

            render()
        }
    }

    fun rotate(angleInRadians: Float?) {
        angleInRadians?.let {
            val c = cos(it)
            val s = sin(it)

            rotationMatrix[0] = c // Original
            rotationMatrix[1] = -s // Original
//            rotationMatrix[2] = -origin.xCoord * c + origin.yCoord * s + origin.xCoord // Original
            rotationMatrix[3] = s // Original
            rotationMatrix[4] = c // Original
//            rotationMatrix[5] = -origin.xCoord * s - origin.yCoord + c + origin.yCoord // Original

            println("Test60: $rotationMatrix")

            render()
        }
    }

    private fun createSlice(startAngle: Float, sliceAngle: Float, origin: Point2D): List<Point2D> {
        return listOf(
            origin,
            Point2D(
                origin.xCoord + radius * cos(startAngle),
                origin.yCoord + radius * sin(startAngle)
            ),
            Point2D(
                origin.xCoord + radius * cos(startAngle + sliceAngle),
                origin.yCoord + radius * sin(startAngle + sliceAngle)
            )
        )
    }

}

fun Float32Array.addValues(values: List<Point2D>) {
    values.forEachIndexed { index, point ->
        this[2 * index] = point.xCoord
        this[2 * index + 1] = point.yCoord
    }
}


fun Float32Array.addValues(values: FloatArray) {
    values.forEachIndexed { index, value ->
        this[index] = value
    }
}