package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    private fun materialIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    val LocalFireDepartment: ImageVector by lazy {
        materialIcon("LocalFireDepartment") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.5f, 0.67f)
                reflectiveCurveToRelative(0.74f, 2.65f, 0.74f, 4.8f)
                curveToRelative(0f, 2.06f, -1.35f, 3.73f, -3.41f, 3.73f)
                curveToRelative(-2.07f, 0f, -3.63f, -1.67f, -3.63f, -3.73f)
                lineToRelative(0.03f, -0.36f)
                curveTo(5.21f, 7.51f, 4f, 10.62f, 4f, 14f)
                curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
                reflectiveCurveToRelative(8f, -3.58f, 8f, -8f)
                curveTo(20f, 8.61f, 17.41f, 3.8f, 13.5f, 0.67f)
                close()
                moveTo(11.71f, 19f)
                curveToRelative(-1.78f, 0f, -3.22f, -1.4f, -3.22f, -3.14f)
                curveToRelative(0f, -1.62f, 1.05f, -2.76f, 2.81f, -3.12f)
                curveToRelative(1.77f, -0.36f, 3.6f, -1.21f, 4.62f, -2.58f)
                curveToRelative(0.39f, 1.29f, 0.59f, 2.65f, 0.59f, 4.04f)
                curveToRelative(0f, 2.65f, -2.15f, 4.8f, -4.8f, 4.8f)
                close()
            }
        }
    }

    val Bolt: ImageVector by lazy {
        materialIcon("Bolt") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 21f)
                horizontalLineToRelative(-1f)
                lineToRelative(1f, -7f)
                horizontalLineTo(7.5f)
                curveToRelative(-0.88f, 0f, -0.33f, -0.75f, -0.31f, -0.78f)
                curveTo(8.48f, 10.94f, 10.42f, 7.54f, 13.01f, 3f)
                horizontalLineToRelative(1f)
                lineToRelative(-1f, 7f)
                horizontalLineToRelative(3.51f)
                curveToRelative(0.4f, 0f, 0.62f, 0.19f, 0.4f, 0.66f)
                curveTo(12.97f, 17.55f, 11f, 21f, 11f, 21f)
                close()
            }
        }
    }

    val Sms: ImageVector by lazy {
        materialIcon("Sms") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(18f)
                lineToRelative(4f, -4f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(20f, 16f)
                horizontalLineTo(5.17f)
                lineTo(4f, 17.17f)
                verticalLineTo(4f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(12f)
                close()
                moveTo(7f, 9f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(7f)
                close()
                moveTo(11f, 9f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(15f, 9f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                close()
            }
        }
    }

    val Description: ImageVector by lazy {
        materialIcon("Description") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(16f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 1.99f, 2f)
                horizontalLineTo(18f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(8f)
                lineToRelative(-6f, -6f)
                close()
                moveTo(16f, 18f)
                horizontalLineTo(8f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                close()
                moveTo(16f, 14f)
                horizontalLineTo(8f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                close()
                moveTo(13f, 9f)
                verticalLineTo(3.5f)
                lineTo(18.5f, 9f)
                horizontalLineTo(13f)
                close()
            }
        }
    }

    val FileUpload: ImageVector by lazy {
        materialIcon("FileUpload") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 16f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(4f)
                lineToRelative(-7f, -7f)
                lineToRelative(-7f, 7f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(6f)
                close()
                moveTo(5f, 18f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                verticalLineToRelative(-2f)
                close()
            }
        }
    }

    val NotificationsActive: ImageVector by lazy {
        materialIcon("NotificationsActive") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 22f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                horizontalLineToRelative(-4f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                close()
                moveTo(18f, 16f)
                verticalLineToRelative(-5f)
                curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
                verticalLineTo(4f)
                curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                reflectiveCurveTo(10.5f, 3.17f, 10.5f, 4f)
                verticalLineToRelative(0.68f)
                curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
                verticalLineToRelative(5f)
                lineToRelative(-2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-1f)
                lineToRelative(-2f, -2f)
                close()
            }
        }
    }

    val PictureAsPdf: ImageVector by lazy {
        materialIcon("PictureAsPdf") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 2f)
                horizontalLineTo(8f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(4f, 6f)
                horizontalLineTo(2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                close()
            }
        }
    }

    val Science: ImageVector by lazy {
        materialIcon("Science") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.8f, 18.4f)
                lineTo(14f, 10.67f)
                verticalLineTo(6.5f)
                lineToRelative(1.35f, -1.69f)
                curveTo(15.61f, 4.48f, 15.38f, 4f, 14.96f, 4f)
                horizontalLineTo(9.04f)
                curveToRelative(-0.42f, 0f, -0.65f, 0.48f, -0.39f, 0.81f)
                lineTo(10f, 6.5f)
                verticalLineToRelative(4.17f)
                lineTo(4.2f, 18.4f)
                curveTo(3.71f, 19.06f, 4.18f, 20f, 5f, 20f)
                horizontalLineToRelative(14f)
                curveToRelative(0.82f, 0f, 1.29f, -0.94f, 0.8f, -1.6f)
                close()
            }
        }
    }

    val TableChart: ImageVector by lazy {
        materialIcon("TableChart") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 10.02f)
                horizontalLineToRelative(5f)
                verticalLineTo(21f)
                horizontalLineToRelative(-5f)
                verticalLineTo(10.02f)
                close()
                moveTo(17f, 21f)
                horizontalLineToRelative(3f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineToRelative(-9f)
                horizontalLineToRelative(-5f)
                verticalLineToRelative(11f)
                close()
                moveTo(20f, 3f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(19f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(3f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(3f)
                verticalLineTo(10f)
                horizontalLineTo(3f)
                verticalLineToRelative(9f)
                close()
            }
        }
    }

    val AutoAwesome: ImageVector by lazy {
        materialIcon("AutoAwesome") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 9f)
                lineToRelative(1.25f, -2.75f)
                lineTo(23f, 5f)
                lineToRelative(-2.75f, -1.25f)
                lineTo(19f, 1f)
                lineToRelative(-1.25f, 2.75f)
                lineTo(15f, 5f)
                lineToRelative(2.75f, 1.25f)
                close()
                moveTo(11.5f, 9.5f)
                lineTo(9f, 4f)
                lineTo(6.5f, 9.5f)
                lineTo(1f, 12f)
                lineToRelative(5.5f, 2.5f)
                lineTo(9f, 20f)
                lineToRelative(2.5f, -5.5f)
                lineTo(17f, 12f)
                close()
            }
        }
    }

    val DeleteOutline: ImageVector by lazy {
        materialIcon("DeleteOutline") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f)
                horizontalLineTo(6f)
                verticalLineToRelative(12f)
                close()
                moveTo(8f, 9f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(10f)
                horizontalLineTo(8f)
                verticalLineTo(9f)
                close()
                moveTo(15.5f, 4f)
                lineToRelative(-1f, -1f)
                horizontalLineToRelative(-5f)
                lineToRelative(-1f, 1f)
                horizontalLineTo(5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(14f)
                verticalLineTo(4f)
                close()
            }
        }
    }

    val Tune: ImageVector by lazy {
        materialIcon("Tune") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 17f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-2f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(10f)
                verticalLineTo(5f)
                horizontalLineTo(3f)
                close()
                moveTo(13f, 21f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-8f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(2f)
                close()
                moveTo(7f, 9f)
                verticalLineToRelative(2f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineTo(9f)
                horizontalLineTo(7f)
                close()
                moveTo(21f, 13f)
                verticalLineToRelative(-2f)
                horizontalLineTo(11f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(10f)
                close()
                moveTo(15f, 9f)
                horizontalLineToRelative(2f)
                verticalLineTo(7f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineToRelative(-4f)
                verticalLineTo(3f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(6f)
                close()
            }
        }
    }

    val AccountBalance: ImageVector by lazy {
        materialIcon("AccountBalance") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 10f)
                verticalLineToRelative(7f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(-7f)
                horizontalLineTo(4f)
                close()
                moveTo(10f, 10f)
                verticalLineToRelative(7f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(-7f)
                horizontalLineToRelative(-3f)
                close()
                moveTo(2f, 22f)
                horizontalLineToRelative(19f)
                verticalLineToRelative(-3f)
                horizontalLineTo(2f)
                verticalLineToRelative(3f)
                close()
                moveTo(16f, 10f)
                verticalLineToRelative(7f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(-7f)
                horizontalLineToRelative(-3f)
                close()
                moveTo(11.5f, 1f)
                lineTo(2f, 6f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(19f)
                verticalLineTo(6f)
                lineToRelative(-9.5f, -5f)
                close()
            }
        }
    }

    val DirectionsCar: ImageVector by lazy {
        materialIcon("DirectionsCar") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.92f, 6.01f)
                curveTo(18.72f, 5.42f, 18.16f, 5f, 17.5f, 5f)
                horizontalLineToRelative(-11f)
                curveToRelative(-0.66f, 0f, -1.21f, 0.42f, -1.42f, 1.01f)
                lineTo(3f, 12f)
                verticalLineToRelative(8f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(1f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-1f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(1f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(1f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-8f)
                lineToRelative(-2.08f, -5.99f)
                close()
                moveTo(6.5f, 16f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(5.67f, 13f, 6.5f, 13f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(7.33f, 16f, 6.5f, 16f)
                close()
                moveTo(17.5f, 16f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(16.67f, 13f, 17.5f, 13f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(18.33f, 16f, 17.5f, 16f)
                close()
                moveTo(5f, 11f)
                lineToRelative(1.5f, -4.5f)
                horizontalLineToRelative(11f)
                lineTo(19f, 11f)
                horizontalLineTo(5f)
                close()
            }
        }
    }

    val LocalGroceryStore: ImageVector by lazy {
        materialIcon("LocalGroceryStore") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 18f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                reflectiveCurveTo(5.9f, 22f, 7f, 22f)
                reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                close()
                moveTo(1f, 2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                lineToRelative(3.6f, 7.59f)
                lineToRelative(-1.35f, 2.45f)
                curveToRelative(-0.16f, 0.28f, -0.25f, 0.61f, -0.25f, 0.96f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(-2f)
                horizontalLineTo(7.42f)
                curveToRelative(-0.14f, 0f, -0.25f, -0.11f, -0.25f, -0.25f)
                lineToRelative(0.03f, -0.12f)
                lineToRelative(0.9f, -1.63f)
                horizontalLineToRelative(7.45f)
                curveToRelative(0.75f, 0f, 1.41f, -0.41f, 1.75f, -1.03f)
                lineToRelative(3.58f, -6.49f)
                curveToRelative(0.08f, -0.14f, 0.12f, -0.31f, 0.12f, -0.48f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                horizontalLineTo(5.21f)
                lineToRelative(-0.94f, -2f)
                horizontalLineTo(1f)
                close()
                moveTo(17f, 18f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                reflectiveCurveToRelative(0.89f, 2f, 1.99f, 2f)
                reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                close()
            }
        }
    }

    val Movie: ImageVector by lazy {
        materialIcon("Movie") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 4f)
                lineToRelative(2f, 4f)
                horizontalLineToRelative(-3f)
                lineToRelative(-2f, -4f)
                horizontalLineToRelative(-2f)
                lineToRelative(2f, 4f)
                horizontalLineToRelative(-3f)
                lineToRelative(-2f, -4f)
                horizontalLineTo(8f)
                lineToRelative(2f, 4f)
                horizontalLineTo(7f)
                lineTo(5f, 4f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(2f, 18f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                horizontalLineToRelative(-4f)
                close()
            }
        }
    }

    val ReceiptLong: ImageVector by lazy {
        materialIcon("ReceiptLong") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.5f, 3.5f)
                lineTo(18f, 2f)
                lineToRelative(-1.5f, 1.5f)
                lineTo(15f, 2f)
                lineToRelative(-1.5f, 1.5f)
                lineTo(12f, 2f)
                lineToRelative(-1.5f, 1.5f)
                lineTo(9f, 2f)
                lineTo(7.5f, 3.5f)
                lineTo(6f, 2f)
                verticalLineToRelative(14f)
                horizontalLineTo(3f)
                verticalLineToRelative(3f)
                curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
                horizontalLineToRelative(12f)
                curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
                verticalLineTo(2f)
                lineToRelative(-1.5f, 1.5f)
                close()
                moveTo(19f, 19f)
                curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
                reflectiveCurveToRelative(-1f, -0.45f, -1f, -1f)
                verticalLineToRelative(-3f)
                horizontalLineTo(8f)
                verticalLineTo(5f)
                horizontalLineToRelative(11f)
                verticalLineToRelative(14f)
                close()
            }
        }
    }

    val Restaurant: ImageVector by lazy {
        materialIcon("Restaurant") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 9f)
                horizontalLineTo(9f)
                verticalLineTo(2f)
                horizontalLineTo(7f)
                verticalLineToRelative(7f)
                horizontalLineTo(5f)
                verticalLineTo(2f)
                horizontalLineTo(3f)
                verticalLineToRelative(7f)
                curveToRelative(0f, 2.12f, 1.66f, 3.84f, 3.75f, 3.97f)
                verticalLineTo(22f)
                horizontalLineToRelative(2.5f)
                verticalLineToRelative(-9.03f)
                curveTo(11.34f, 12.84f, 13f, 11.12f, 13f, 9f)
                verticalLineTo(2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(7f)
                close()
                moveTo(16f, 6f)
                verticalLineToRelative(8f)
                horizontalLineToRelative(2.5f)
                verticalLineToRelative(8f)
                horizontalLineTo(21f)
                verticalLineTo(2f)
                curveToRelative(-2.76f, 0f, -5f, 2.24f, -5f, 4f)
                close()
            }
        }
    }

    val ShoppingBag: ImageVector by lazy {
        materialIcon("ShoppingBag") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 6f)
                horizontalLineToRelative(-2f)
                curveToRelative(0f, -2.21f, -1.79f, -4f, -4f, -4f)
                reflectiveCurveTo(8f, 3.79f, 8f, 6f)
                horizontalLineTo(6f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(8f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(12f, 4f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
                horizontalLineToRelative(-4f)
                curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
                close()
                moveTo(18f, 20f)
                horizontalLineTo(6f)
                verticalLineTo(8f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                reflectiveCurveToRelative(1f, -0.45f, 1f, -1f)
                verticalLineTo(8f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(2f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                reflectiveCurveToRelative(1f, -0.45f, 1f, -1f)
                verticalLineTo(8f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(12f)
                close()
            }
        }
    }
}
