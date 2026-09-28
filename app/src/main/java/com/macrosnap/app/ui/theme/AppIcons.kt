package com.macrosnap.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icons missing from material-icons-core; the extended set is too heavy for an unminified debug build. */
object AppIcons {
    val Dumbbell: ImageVector by lazy {
        icon(
            "Dumbbell",
            "M20.57 14.86L22 13.43 20.57 12 17 15.57 8.43 7 12 3.43 10.57 2 9.14 3.43 7.71 2 5.57 4.14 " +
                "4.14 2.71 2.71 4.14l1.43 1.43L2 7.71l1.43 1.43L2 10.57 3.43 12 7 8.43 15.57 17 12 20.57 " +
                "13.43 22l1.43-1.43L16.29 22l2.14-2.14 1.43 1.43 1.43-1.43-1.43-1.43L22 16.29z",
        )
    }

    /** An empty star. Icons.Outlined.Star is a solid star, so it can't show an unset rating. */
    val StarBorder: ImageVector by lazy {
        icon(
            "StarBorder",
            "M22 9.24l-7.19-0.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24z" +
                "M12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-0.38L12 6.1l1.71 4.04 4.38 0.38-3.32 2.88 1 4.28L12 15.4z",
        )
    }

    val Camera: ImageVector by lazy {
        icon(
            "Camera",
            "M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z" +
                "M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9z" +
                "m3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z",
        )
    }

    val Image: ImageVector by lazy {
        icon(
            "Image",
            "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2z" +
                "M8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
        )
    }

    val Barcode: ImageVector by lazy {
        icon("Barcode", "M2 5h2v14H2zM5 5h1v14H5zM7 5h3v14H7zM11 5h1v14h-1zM14 5h2v14h-2zM17 5h1v14h-1zM19 5h3v14h-3z")
    }

    val ChatBubble: ImageVector by lazy {
        icon("ChatBubble", "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z")
    }

    val Restaurant: ImageVector by lazy {
        icon(
            "Restaurant",
            "M11 9H9V2H7v7H5V2H3v7c0 2.12 1.66 3.84 3.75 3.97V22h2.5v-9.03C11.34 12.84 13 11.12 13 9V2h-2v7z" +
                "m5-3v8h2.5v8H21V2c-2.76 0-5 2.24-5 4z",
        )
    }

    val BarChart: ImageVector by lazy {
        icon("BarChart", "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z")
    }

    val WaterDrop: ImageVector by lazy {
        icon("WaterDrop", "M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8z")
    }

    val Moon: ImageVector by lazy {
        icon(
            "Moon",
            "M12.34 2.02C6.59 1.82 2 6.42 2 12c0 5.52 4.48 10 10 10 3.71 0 6.93-2.02 8.66-5.02-7.51-.25-12.09-8.43-8.32-14.96z",
        )
    }

    val Flame: ImageVector by lazy {
        icon(
            "Flame",
            "M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36" +
                "C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67z" +
                "M11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58" +
                ".39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z",
        )
    }

    val Pill: ImageVector by lazy {
        icon("Pill", "M12.35 18.01L18.01 12.35A4.5 4.5 0 0 0 11.65 5.99L5.99 11.65A4.5 4.5 0 0 0 12.35 18.01z")
    }

    /** Four-pointed sparkle, used wherever the AI does the work. */
    val Sparkle: ImageVector by lazy {
        icon("Sparkle", "M12 2l2.2 7.8L22 12l-7.8 2.2L12 22l-2.2-7.8L2 12l7.8-2.2z")
    }

    val Leaf: ImageVector by lazy {
        icon(
            "Leaf",
            "M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66.95-2.3c.48.17.98.3 1.34.3C19 20 22 3 22 3" +
                "c-1 2-8 2.25-13 3.25S2 11.5 2 13.5s1.75 3.75 1.75 3.75C7 8 17 8 17 8z",
        )
    }

    val Scale: ImageVector by lazy {
        icon(
            "Scale",
            "M5 3h14c1.1 0 2 .9 2 2v14c0 1.1-.9 2-2 2H5c-1.1 0-2-.9-2-2V5c0-1.1.9-2 2-2z" +
                "m7 3c-2.5 0-4.6 1.2-5.9 3l2.4 2.4c.8-1.1 2-1.9 3.5-1.9s2.7.8 3.5 1.9L17.9 9c-1.3-1.8-3.4-3-5.9-3z",
        )
    }

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
            .build()
}
