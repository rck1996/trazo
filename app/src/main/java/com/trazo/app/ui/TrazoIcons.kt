package com.trazo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class TrazoIconKind {
    ADD, TODAY, TASK, CALENDAR, HABIT, FOCUS,
    EDIT, SCHEDULE, ARCHIVE, DELETE,
    CHEVRON_LEFT, CHEVRON_RIGHT, ARROW_RIGHT,
    MICROPHONE, NOTIFICATION, EXPORT, IMPORT, SETTINGS, CLOSE, CHECK
}

/** A dependency-free icon set drawn with the same rounded ink stroke used by Trazo. */
@Composable
internal fun TrazoIcon(
    kind: TrazoIconKind,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: Dp = 20.dp,
    description: String? = null
) {
    val ink = color ?: Ink
    val minimal = LocalMinimalMode.current
    val semantics = if (description == null) Modifier else Modifier.semantics { contentDescription = description }
    Canvas(
        modifier.size(size)
            .then(if (minimal) Modifier else Modifier.rotate(-1.2f))
            .then(semantics)
    ) {
        val w = this.size.width
        val h = this.size.height
        val unit = minOf(w, h)
        val strokeWidth = unit * .085f
        val stroke = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun point(x: Float, y: Float) = Offset(w * x, h * y)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(ink, point(x1, y1), point(x2, y2), strokeWidth, StrokeCap.Round)
        fun path(build: Path.() -> Unit) = drawPath(Path().apply(build), ink, style = stroke)

        when (kind) {
            TrazoIconKind.ADD -> { line(.5f, .18f, .5f, .82f); line(.18f, .5f, .82f, .5f) }
            TrazoIconKind.TODAY -> {
                drawCircle(ink, unit * .20f, center, style = stroke)
                listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).forEach { angle ->
                    val r1 = unit * .34f; val r2 = unit * .44f
                    val rad = Math.toRadians(angle.toDouble())
                    drawLine(ink, center + Offset((kotlin.math.cos(rad) * r1).toFloat(), (kotlin.math.sin(rad) * r1).toFloat()),
                        center + Offset((kotlin.math.cos(rad) * r2).toFloat(), (kotlin.math.sin(rad) * r2).toFloat()), strokeWidth, StrokeCap.Round)
                }
            }
            TrazoIconKind.TASK -> {
                drawRoundRect(ink, point(.16f, .14f), Size(w * .68f, h * .72f), CornerRadius(unit * .08f), style = stroke)
                line(.28f, .48f, .40f, .61f); line(.40f, .61f, .68f, .34f)
            }
            TrazoIconKind.CALENDAR, TrazoIconKind.SCHEDULE -> {
                drawRoundRect(ink, point(.14f, .20f), Size(w * .72f, h * .64f), CornerRadius(unit * .07f), style = stroke)
                line(.15f, .39f, .85f, .39f); line(.32f, .12f, .32f, .29f); line(.68f, .12f, .68f, .29f)
                if (kind == TrazoIconKind.SCHEDULE) {
                    drawCircle(ink, unit * .16f, point(.63f, .62f), style = stroke)
                    line(.63f, .62f, .63f, .52f); line(.63f, .62f, .71f, .66f)
                } else {
                    drawCircle(ink, unit * .035f, point(.35f, .58f)); drawCircle(ink, unit * .035f, point(.62f, .58f))
                }
            }
            TrazoIconKind.HABIT -> {
                drawArc(ink, 205f, 260f, false, point(.15f, .16f), Size(w * .70f, h * .70f), style = stroke)
                path { moveTo(w * .17f, h * .29f); lineTo(w * .16f, h * .52f); lineTo(w * .36f, h * .42f) }
            }
            TrazoIconKind.FOCUS -> {
                drawCircle(ink, unit * .34f, center, style = stroke)
                drawCircle(ink, unit * .11f, center)
            }
            TrazoIconKind.EDIT -> {
                path { moveTo(w * .22f, h * .73f); lineTo(w * .29f, h * .51f); lineTo(w * .67f, h * .16f); lineTo(w * .84f, h * .33f); lineTo(w * .47f, h * .69f); close() }
                line(.20f, .82f, .53f, .82f)
            }
            TrazoIconKind.ARCHIVE -> {
                drawRoundRect(ink, point(.16f, .30f), Size(w * .68f, h * .52f), CornerRadius(unit * .05f), style = stroke)
                drawRoundRect(ink, point(.11f, .17f), Size(w * .78f, h * .18f), CornerRadius(unit * .04f), style = stroke)
                line(.50f, .43f, .50f, .67f); line(.38f, .56f, .50f, .68f); line(.62f, .56f, .50f, .68f)
            }
            TrazoIconKind.DELETE -> {
                drawRoundRect(ink, point(.27f, .29f), Size(w * .46f, h * .55f), CornerRadius(unit * .04f), style = stroke)
                line(.20f, .25f, .80f, .25f); line(.39f, .16f, .61f, .16f)
                line(.42f, .40f, .42f, .70f); line(.58f, .40f, .58f, .70f)
            }
            TrazoIconKind.CHEVRON_LEFT -> path { moveTo(w * .64f, h * .18f); lineTo(w * .34f, h * .50f); lineTo(w * .64f, h * .82f) }
            TrazoIconKind.CHEVRON_RIGHT -> path { moveTo(w * .36f, h * .18f); lineTo(w * .66f, h * .50f); lineTo(w * .36f, h * .82f) }
            TrazoIconKind.ARROW_RIGHT -> { line(.16f, .50f, .82f, .50f); line(.62f, .29f, .83f, .50f); line(.62f, .71f, .83f, .50f) }
            TrazoIconKind.MICROPHONE -> {
                drawRoundRect(ink, point(.36f, .12f), Size(w * .28f, h * .47f), CornerRadius(unit * .14f), style = stroke)
                drawArc(ink, 0f, 180f, false, point(.23f, .33f), Size(w * .54f, h * .39f), style = stroke)
                line(.50f, .72f, .50f, .87f); line(.34f, .87f, .66f, .87f)
            }
            TrazoIconKind.NOTIFICATION -> {
                path { moveTo(w * .23f, h * .70f); quadraticTo(w * .31f, h * .60f, w * .31f, h * .40f); quadraticTo(w * .31f, h * .17f, w * .50f, h * .17f); quadraticTo(w * .69f, h * .17f, w * .69f, h * .40f); quadraticTo(w * .69f, h * .60f, w * .77f, h * .70f); close() }
                drawArc(ink, 10f, 160f, false, point(.40f, .68f), Size(w * .20f, h * .16f), style = stroke)
            }
            TrazoIconKind.EXPORT, TrazoIconKind.IMPORT -> {
                drawRoundRect(ink, point(.18f, .50f), Size(w * .64f, h * .34f), CornerRadius(unit * .05f), style = stroke)
                val up = kind == TrazoIconKind.EXPORT
                line(.50f, if (up) .66f else .16f, .50f, if (up) .16f else .66f)
                line(.35f, if (up) .31f else .51f, .50f, if (up) .16f else .66f)
                line(.65f, if (up) .31f else .51f, .50f, if (up) .16f else .66f)
            }
            TrazoIconKind.SETTINGS -> {
                drawCircle(ink, unit * .25f, center, style = stroke)
                drawCircle(ink, unit * .07f, center, style = stroke)
                listOf(0f, 90f, 180f, 270f).forEach { angle ->
                    val rad = Math.toRadians(angle.toDouble())
                    val inner = unit * .31f; val outer = unit * .43f
                    drawLine(
                        ink,
                        center + Offset((kotlin.math.cos(rad) * inner).toFloat(), (kotlin.math.sin(rad) * inner).toFloat()),
                        center + Offset((kotlin.math.cos(rad) * outer).toFloat(), (kotlin.math.sin(rad) * outer).toFloat()),
                        strokeWidth,
                        StrokeCap.Round
                    )
                }
            }
            TrazoIconKind.CLOSE -> { line(.24f, .24f, .76f, .76f); line(.76f, .24f, .24f, .76f) }
            TrazoIconKind.CHECK -> { line(.18f, .51f, .41f, .73f); line(.41f, .73f, .82f, .27f) }
        }
    }
}
