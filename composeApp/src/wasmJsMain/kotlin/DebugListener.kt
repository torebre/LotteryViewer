

interface DebugListener {

    fun rotationChanged(rotationAngle: Float)

    fun markerUpdated(
        markerPointOnWheel: Point2D,
        markerEnd: Point2D
    )

}