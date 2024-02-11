import org.khronos.webgl.Float32Array
import org.khronos.webgl.get



fun multiplyMatrices(vararg matrices: Float32Array): Float32Array {
    return matrices.reduce { a, b ->
        multiplyMatrices(a, b)
    }
}

fun multiplyMatrices(a: Float32Array, b: Float32Array): Float32Array {
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


/**
 * TODO As it is now this method does not distinguish between minimum or maximum points, so it cannot be guaranteed to find a minimum
 */
fun bisect(
    valueFunction: (Float) -> Float,
    bracketStart: Float,
    pointInBracket: Float,
    bracketEnd: Float
): Float {
    var a = bracketStart
    var b = pointInBracket
    var c = bracketEnd
    val maxTries = 20

    var tryCounter = 0
    while (tryCounter < maxTries) {
        val alpha = (valueFunction(b) - valueFunction(a)) / (b - a)
        val beta = (valueFunction(c) - valueFunction(a) - alpha * (c - a)) / ((c - a) * (c - b))
        val x = (a + b) / 2 - alpha / (2 * beta)
        a = b
        b = c
        c = x

        if (maxOf(a, b, c) - minOf(a, b, c) < 1e-6) {
            break
        }

        ++tryCounter
    }

//            if (tryCounter == maxTries) {
    // TODO Write warning or throw an exception?
//            }

    return c
}
