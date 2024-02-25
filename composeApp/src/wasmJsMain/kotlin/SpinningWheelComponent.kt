import kotlinx.coroutines.*
import org.khronos.webgl.*
import kotlin.math.*
import kotlin.random.Random

class SpinningWheelComponent(
    private val numberOfSlices: Int,
    private val renderingContext: WebGLRenderingContext
) {
    private var positionAttributeLocation: Int = 0
    private var resolutionUniformLocation: WebGLUniformLocation? = null
    private var colorLocation: WebGLUniformLocation? = null
    private var matrixLocation: WebGLUniformLocation? = null
    private var positionBuffer: WebGLBuffer? = null
    private var program: WebGLProgram? = null

    private val translationMatrix = createTranslationMatrix()
    private val origin: Point2D = Point2D(
        renderingContext.canvas.width.toFloat() / 2,
        renderingContext.canvas.height.toFloat() / 2
    )

    var rotationAngleInRadians = 0f
        get() = field

    private var rotationMatrix = createRotationMatrix(rotationAngleInRadians)
    private val scaleMatrix = createScalingMatrix()

    private val colorArray: List<Float32Array>

    // https://webglfundamentals.org/webgl/lessons/webgl-points-lines-triangles.html
    private val primitiveType = WebGLRenderingContext.TRIANGLES

    private val radius: Float
    private val markerLength = 50f
    private val markerWidth = 10f
    private val markerStart: Point2D
    private val cutoffForMarker: Float

    // TODO Should take into consideration how big the wheel will be
    private val circleStep: Float
    private val sliceAngle = (2 * PI / numberOfSlices.toFloat()).toFloat()

    private val size = 2
    private val type = WebGLRenderingContext.FLOAT
    private val normalize = false
    private val stride = 0
    private val offset = 0

    private val quarterCircle = PI / 2
    private val speedDelayMs = 100L

    private val seed = Random.nextInt()
    private val random = Random(seed)

    private val debugListeners = mutableListOf<DebugListener>()


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

        radius = (min(renderingContext.canvas.height, renderingContext.canvas.width).div(2) - 100).toFloat()
        markerStart = Point2D(origin.xCoord, origin.yCoord - radius - 20)
        cutoffForMarker = (PI / 2 - findCutoffForMarker(markerLength, radius, markerStart)).toFloat()
        circleStep = (PI / (2 * numberOfSlices)).toFloat()

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
        this.rotationMatrix = createRotationMatrix(rotationAngleInRadians)

        with(renderingContext) {
            bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, positionBuffer)
            // TODO Add handling of resizing the canvas
            viewport(0, 0, canvas.width, canvas.height)
            clearColor(0.0f, 0.0f, 0.0f, 0.0f)
            clear(WebGLRenderingContext.COLOR_BUFFER_BIT)

            useProgram(program)
            enableVertexAttribArray(positionAttributeLocation)

            uniform2f(
                resolutionUniformLocation,
                canvas.width.toFloat(),
                canvas.height.toFloat()
            )
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun doSpin() {
        GlobalScope.launch(Dispatchers.Default) {
            var currentRotation = 0f
            var currentSpeed = (PI / 4).toFloat()
            val slowdown = 0.95f

            while (currentSpeed > PI / 1000) {
                val speed = sqrt(currentSpeed)
                rotate(-currentRotation)
                render()
                currentRotation += speed
                currentSpeed *= slowdown

                delay(speedDelayMs)
            }

            moveWheelToMarkerDownPosition()
        }

    }

    fun render() {
        renderLotteryWheel(sliceAngle)
        createMarker(rotationAngleInRadians, sliceAngle, markerStart)
    }

    private fun renderLotteryWheel(sliceAngle: Float) {
        var currentStartAngle = 0f

        for (i in 0 until numberOfSlices) {
            val slice = createSlice(currentStartAngle, sliceAngle, origin)
            currentStartAngle += sliceAngle

            val trianglePoints = slice.flatMap {
                sequenceOf(
                    it.a.xCoord, it.a.yCoord,
                    it.b.xCoord, it.b.yCoord,
                    it.c.xCoord, it.c.yCoord
                )
            }.toList()

            val points = Float32Array(trianglePoints.size).also { it.addValues(trianglePoints) }

            renderingContext.bufferData(
                WebGLRenderingContext.ARRAY_BUFFER,
                points,
                WebGLRenderingContext.STATIC_DRAW
            )

            renderingContext.uniform4fv(colorLocation, colorArray[i])
            renderingContext.vertexAttribPointer(positionAttributeLocation, size, type, normalize, stride, offset)

            val matrix = multiplyMatrices(
                translationMatrix,
                createTranslationMatrix(origin),
                rotationMatrix,
                createTranslationMatrix(Point2D(-origin.xCoord, -origin.yCoord)),
                scaleMatrix
            )

            renderingContext.uniformMatrix3fv(matrixLocation, false, matrix)

            val count = trianglePoints.size / 2
            renderingContext.drawArrays(primitiveType, offset, count)
        }
    }

    private fun findMarkerPointOnWheel(
        rotationAngleInRadians: Float,
        sliceAngle: Float,
        markerStart: Point2D
    ): Point2D {
        var currentAngle = rotationAngleInRadians

        while (currentAngle > PI / 2) {
            currentAngle -= sliceAngle
        }

        var angleFromTop = PI / 2 - currentAngle
        var potentialMarkerRestingPoint = Point2D(markerStart.xCoord, markerStart.yCoord + markerLength)

        while (angleFromTop > 0) {
            val x = (sin(angleFromTop) * radius).toFloat()
            val y = (cos(angleFromTop) * radius).toFloat()

            potentialMarkerRestingPoint = Point2D(origin.xCoord + x, origin.yCoord - y)

            val lengthFromMarkerStartingPoint = sqrt(
                (markerStart.xCoord - potentialMarkerRestingPoint.xCoord).pow(2) + (markerStart.yCoord - potentialMarkerRestingPoint.yCoord).pow(
                    2
                )
            )

            if (lengthFromMarkerStartingPoint > markerLength) {
                break
            }

            angleFromTop -= sliceAngle
        }

        return potentialMarkerRestingPoint
    }

    private fun clampAngle(angle: Float): Float {
        var updatedAngle = angle

        while (updatedAngle < 0f) {
            updatedAngle += 2 * PI.toFloat()
        }

        while (updatedAngle >= 2 * PI.toFloat()) {
            updatedAngle -= 2 * PI.toFloat()
        }

        return updatedAngle
    }

    private suspend fun moveWheelToMarkerDownPosition() {
        val start = rotationAngleInRadians
        var current = rotationAngleInRadians

        while (!(current <= quarterCircle && current + sliceAngle > quarterCircle)) {
            current = clampAngle(current + sliceAngle)
        }

        val necessaryRotation = quarterCircle - current

        println("Necessary rotation: $necessaryRotation")

        val speed = (PI / 100).toFloat()
        var currentRotation = 0f

        while (currentRotation < necessaryRotation) {
            rotate(start + currentRotation)
            render()
            currentRotation += speed
            delay(speedDelayMs)
        }

    }


    private fun createMarker(rotationAngleInRadians: Float, sliceAngle: Float, markerStart: Point2D) {
        var currentAngle = rotationAngleInRadians

        while (currentAngle > PI / 2) {
            currentAngle -= sliceAngle
        }

        val angleFromTop = PI / 2 - currentAngle

        println("Angle from top: $angleFromTop")

        val markerTriangles = listOf(
            markerStart.xCoord, markerStart.yCoord,
            markerStart.xCoord, markerStart.yCoord + markerLength,
            markerStart.xCoord + markerWidth, markerStart.yCoord + markerLength,
            markerStart.xCoord, markerStart.yCoord,
            markerStart.xCoord + markerWidth, markerStart.yCoord,
            markerStart.xCoord + markerWidth, markerStart.yCoord + markerLength,
        )

        println("Radius: $radius. Cos for angle from top: $angleFromTop. Marker length: $markerLength")

        val rotationAngle =
            atan((radius * sin(angleFromTop) / (origin.yCoord - markerStart.yCoord - radius * cos(angleFromTop)))).toFloat()
//        val rotationAngle = acos(radius*cos(angleFromTop) / markerLength).toFloat()

        println("Marker rotation angle: $rotationAngle")

        val rotationMatrix = createRotationMatrix(rotationAngle)

        val points = Float32Array(markerTriangles.size).also { it.addValues(markerTriangles) }

        renderingContext.bufferData(
            WebGLRenderingContext.ARRAY_BUFFER,
            points,
            WebGLRenderingContext.STATIC_DRAW
        )

        renderingContext.uniform4fv(colorLocation, getMarkerColor())

        renderingContext.vertexAttribPointer(positionAttributeLocation, size, type, normalize, stride, offset)

        val matrix = multiplyMatrices(
            translationMatrix,
            createTranslationMatrix(markerStart),
            rotationMatrix,
            createTranslationMatrix(Point2D(-markerStart.xCoord, -markerStart.yCoord)),
            scaleMatrix
        )

        renderingContext.uniformMatrix3fv(matrixLocation, false, matrix)

        val count = markerTriangles.size / 2
        renderingContext.drawArrays(primitiveType, offset, count)
    }


    private fun getMarkerColor(): Float32Array {
        return Float32Array(4).also {
            it[0] = 1f
            it[1] = 1f
            it[2] = 0f
            it[3] = 1f
        }
    }

    private fun createRandomColorArray(numberOfSlices: Int): List<Float32Array> {
        return (0 until numberOfSlices).map {
            Float32Array(4).also {
                it[0] = random.nextFloat()
                it[1] = random.nextFloat()
                it[2] = random.nextFloat()
                it[3] = 1f
            }
        }.toList()
    }

    private fun createColorArray(numberOfSlices: Int): List<Float32Array> {
        val unevenNumberOfSlices = numberOfSlices % 2 == 1
        var red = true

        return (0 until numberOfSlices).map {
            if (unevenNumberOfSlices && it == numberOfSlices - 1) {
                Float32Array(4).also {
                    it[0] = 0f
                    it[1] = 1f
                    it[2] = 0f
                    it[3] = 1f
                }
            } else {
                Float32Array(4).also {
                    it[0] = if (red) 1f else 0f
                    it[1] = 0f
                    it[2] = 0f
                    it[3] = 1f
                    red = !red
                }
            }
        }.toList()
    }

    private fun createScalingMatrix(scaling: Pair<Float, Float> = Pair(1f, 1f)): Float32Array {
        return Float32Array(9).also {
            it.addValues(floatArrayOf(scaling.first, 0f, 0f, 0f, scaling.second, 0f, 0f, 0f, 1f))
        }
    }

    private fun createRotationMatrix(angleInRadians: Float): Float32Array {
        val c = cos(angleInRadians)
        val s = sin(angleInRadians)

        return Float32Array(9).also {
            it.addValues(floatArrayOf(c, -s, 0f, s, c, 0f, 0f, 0f, 1f))
        }
    }

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

    private fun rotateDiff(diffAngleInRadians: Float) {
        rotate(diffAngleInRadians + rotationAngleInRadians)
    }

    fun rotate(angleInRadians: Float?) {
        angleInRadians?.let { rotationAngle ->
            rotationAngleInRadians = clampAngle(rotationAngle)

            val c = cos(rotationAngle)
            val s = sin(rotationAngle)

            rotationMatrix[0] = c
            rotationMatrix[1] = -s
            rotationMatrix[3] = s
            rotationMatrix[4] = c

            render()

            debugListeners.forEach { debugListener ->
                debugListener.rotationChanged(rotationAngleInRadians)
            }
        }
    }

    private fun createSlice(startAngle: Float, sliceAngle: Float, origin: Point2D): List<Triangle2D> {
        var currentStart = startAngle
        val stop = startAngle + sliceAngle
        val triangles = mutableListOf<Triangle2D>()

        while (true) {
            val newValue = currentStart + circleStep
            if (newValue < stop) {
                triangles.add(createTriangle2D(origin, currentStart, circleStep))
            } else {
                triangles.add(createTriangle2D(origin, currentStart, stop - currentStart))
                break
            }
            currentStart = newValue
        }

        return triangles
    }


    private fun createTriangle2D(origin: Point2D, startAngle: Float, sliceAngle: Float): Triangle2D {
        return Triangle2D(
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


    companion object {

        /**
         * The angle returned here is from the top and to the right.
         */
        fun findCutoffForMarker(
            markerLength: Float,
            lotteryWheelRadius: Float,
            markerStart: Point2D
        ): Float {
            return bisect(
                { inputAngle ->
                    (markerLength - computeLengthForAngle(inputAngle, lotteryWheelRadius, markerStart)).pow(2)
                },
                0f,
                PI.toFloat() / 16f,
                PI.toFloat() / 2f,
            )

        }

        fun computeLengthForAngle(
            angle: Float,
            radius: Float,
            markerStart: Point2D
        ): Float {
            val y = sqrt(radius.pow(2) / (tan(angle).pow(2) + 1f))
            val x = sqrt(radius.pow(2) - y.pow(2))

            return sqrt((x - markerStart.xCoord).pow(2) + (y - markerStart.yCoord).pow(2))
        }
    }


    fun addDebugListener(debugListener: DebugListener) {
        debugListeners.add(debugListener)
    }

    fun removeDebugListener(debugListener: DebugListener) {
        debugListeners.remove(debugListener)
    }

}

fun Float32Array.addValues(values: List<Point2D>) {
    values.forEachIndexed { index, point ->
        this[2 * index] = point.xCoord
        this[2 * index + 1] = point.yCoord
    }
}

fun Float32Array.addValues(values: List<Float>) {
    values.forEachIndexed { index, value ->
        this[index] = value
    }
}

fun Float32Array.addValues(values: FloatArray) {
    values.forEachIndexed { index, value ->
        this[index] = value
    }
}