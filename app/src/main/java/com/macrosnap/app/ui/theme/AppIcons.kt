package com.macrosnap.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icons missing from material-icons-core; the extended set is too heavy for an unminified debug build. */
object AppIcons {
    val Dumbbell: ImageVector by lazy {
        ImageVector.Builder(name = "Dumbbell", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = addPathNodes(
                    "M20.57 14.86L22 13.43 20.57 12 17 15.57 8.43 7 12 3.43 10.57 2 9.14 3.43 7.71 2 5.57 4.14 " +
                        "4.14 2.71 2.71 4.14l1.43 1.43L2 7.71l1.43 1.43L2 10.57 3.43 12 7 8.43 15.57 17 12 20.57 " +
                        "13.43 22l1.43-1.43L16.29 22l2.14-2.14 1.43 1.43 1.43-1.43-1.43-1.43L22 16.29z",
                ),
                fill = SolidColor(Color.Black),
            )
            .build()
    }

    /** An empty star. Icons.Outlined.Star is a solid star, so it can't show an unset rating. */
    val StarBorder: ImageVector by lazy {
        ImageVector.Builder(name = "StarBorder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = addPathNodes(
                    "M22 9.24l-7.19-0.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24z" +
                        "M12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-0.38L12 6.1l1.71 4.04 4.38 0.38-3.32 2.88 1 4.28L12 15.4z",
                ),
                fill = SolidColor(Color.Black),
            )
            .build()
    }
}
