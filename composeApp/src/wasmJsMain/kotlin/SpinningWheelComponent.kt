import org.khronos.webgl.*
import kotlin.math.*
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
    private var rotationAngleInRadians = 0f
    private var rotationMatrix = createRotationMatrix(rotationAngleInRadians)
    private val scaleMatrix = createScalingMatrix()

    private val colorArray: List<Float32Array>

    // https://webglfundamentals.org/webgl/lessons/webgl-points-lines-triangles.html
    private val primitiveType = WebGLRenderingContext.TRIANGLES

    private val radius = 100f
    private val markerLength = 30f
    private val markerStart = Point2D(0f, 120f)
    private val cutoffForMarker: Float

    // TODO Should take into consideration how big the wheel will be
    private val circleStep = (PI / 200).toFloat()

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

        cutoffForMarker = (PI / 2 - findCutoffForMarker(markerLength, radius, markerStart)).toFloat()

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
        val origin = Point2D(
            renderingContext.canvas.width.toFloat() / 2,
            renderingContext.canvas.height.toFloat() / 2
        )
        this.origin = origin
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

    fun render() {
        val sliceAngle = (2 * PI / numberOfSlices.toFloat()).toFloat()
        renderLotteryWheel(sliceAngle)

        createMarker(sliceAngle, markerStart)
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

            //            println("Test50: ${points.length}")
            //            println("Test51: $currentStartAngle")
            //            println("Test53: $origin")
            //            println("Test54: $points")

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

            val matrix = multiplyMatrices(
                translationMatrix,
                createTranslationMatrix(origin),
                rotationMatrix,
                createTranslationMatrix(Point2D(-origin.xCoord, -origin.yCoord)),
                scaleMatrix
            )


            //            val matrix = multiplyMatrices(translationMatrix)
            renderingContext.uniformMatrix3fv(matrixLocation, false, matrix)

            val count = trianglePoints.size / 2
            renderingContext.drawArrays(primitiveType, offset, count)
        }
    }


    private fun createMarker(sliceAngle: Float, markerStart: Point2D) {
        val markerRestingPointAngle = findMarkerRestingPointAngle(sliceAngle)
//        val markerRestingPointAngle = 0.2f

        println("Test51: $markerRestingPointAngle")
        println("Test52: $cutoffForMarker")

        val markerRestingPoint = getMarkerPointForSlice(markerRestingPointAngle, radius)

        // TODO These coordinates are wrong, just here for testing
        val trianglePoints = listOf(
            markerRestingPoint.first, markerRestingPoint.second,
            markerStart.xCoord, markerStart.yCoord,
            markerStart.xCoord + 2f, markerStart.yCoord + 2f
        )

        println("Test50: $trianglePoints")


//        val trianglePoints = listOf(
//            0f, 0f,
//            0f, 100f,
//            100f, 0f
//        )

        val points = Float32Array(trianglePoints.size).also { it.addValues(trianglePoints) }

//            println("Test50: ${points.length}")
//            println("Test51: $currentStartAngle")
//            println("Test53: $origin")
//            println("Test54: $points")

        renderingContext.bufferData(
            WebGLRenderingContext.ARRAY_BUFFER,
            points,
            WebGLRenderingContext.STATIC_DRAW
        )

        renderingContext.uniform4fv(colorLocation, Float32Array(4).also {
            it[0] = 1f
            it[1] = 0f
            it[2] = 0f
            it[3] = 1f
        })

        val size = 2
        val type = WebGLRenderingContext.FLOAT
        val normalize = false
        val stride = 0
        val offset = 0
        renderingContext.vertexAttribPointer(positionAttributeLocation, size, type, normalize, stride, offset)

        val matrix = multiplyMatrices(
            translationMatrix,
//            createTranslationMatrix(origin),
//            rotationMatrix,
//            createTranslationMatrix(Point2D(-origin.xCoord, -origin.yCoord)),
//            scaleMatrix
        )

//        val matrix = createIdentityMatrix()

        renderingContext.uniformMatrix3fv(matrixLocation, false, matrix)

        val count = trianglePoints.size / 2
        renderingContext.drawArrays(primitiveType, offset, count)
    }

    private fun findMarkerRestingPointAngle(sliceAngle: Float): Float {
        var currentAngle = rotationAngleInRadians

        return if (currentAngle > PI / 2) {
            while (currentAngle > cutoffForMarker) {
                currentAngle -= sliceAngle
            }
            currentAngle
        } else {
            while (currentAngle < cutoffForMarker) {
                currentAngle += sliceAngle
            }
            currentAngle + sliceAngle
        }
    }


//    internal fun computeLengthForAngle(angle: Float,
//                                       radius: Float,
////                                       markerLength: Float,
//                                       totalLength: Float): Float {
////        val markerPoint = getMarkerPointForSlice(angle, radius)
////        val length = sqrt(markerPoint.first.pow(2) + (totalLength-markerPoint.second).pow(2))
////        markerLength - length
//
//        return sqrt(radius.pow(2) / (tan(angle).pow(2) - 1f) + (totalLength - radius * sqrt(1f - sin(angle).pow(2))).pow(2))
//    }


    internal fun getMarkerPointForSlice(angle: Float, radius: Float): Pair<Float, Float> {
        val x = sqrt(radius.pow(2) / (tan(angle).pow(2) + 1f))
        val y = sqrt(radius.pow(2) - x.pow(2))

//        println("Test54: $x, $y, $angle, $radius")

        return Pair(x, y)
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

    private fun createIdentityMatrix(): Float32Array {
        return Float32Array(9).also {
            it.addValues(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))
        }
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
            rotationAngleInRadians = it

            while (rotationAngleInRadians < 0f) {
                rotationAngleInRadians += 2 * PI.toFloat()
            }

            while (rotationAngleInRadians >= 2 * PI.toFloat()) {
                rotationAngleInRadians -= 2 * PI.toFloat()
            }

            val c = cos(it)
            val s = sin(it)

            rotationMatrix[0] = c // Original
            rotationMatrix[1] = -s // Original
//            rotationMatrix[2] = -origin.xCoord * c + origin.yCoord * s + origin.xCoord // Original
            rotationMatrix[3] = s // Original
            rotationMatrix[4] = c // Original
//            rotationMatrix[5] = -origin.xCoord * s - origin.yCoord + c + origin.yCoord // Original

            render()
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