package com.alsaif.mushaf.ui.theme

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface

// ===== الهوية: أسود فاخر + ذهبي =====
val Black = Color(0xFF0A0A0A)        // الخلفية
val Surface = Color(0xFF151412)      // البطاقات
val Surface2 = Color(0xFF1E1C18)     // عناصر مرفوعة
val Gold = Color(0xFFD4AF37)         // الذهبي الأساسي
val GoldLight = Color(0xFFF1D57E)    // لمعة الذهب
val GoldDeep = Color(0xFFA8862A)     // ظل الذهب
val Ink = Color(0xFFF4EBD6)          // النص الأساسي (عاجي دافئ)
val Muted = Color(0xFF9A9080)        // نص ثانوي
val Line = Color(0xFF2C281F)         // الفواصل
val PageBg = Color(0xFF100F0D)       // صفحة المصحف

val GoldGradient = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep))
val GoldBorder = Brush.linearGradient(listOf(GoldLight.copy(alpha = 0.9f), GoldDeep.copy(alpha = 0.5f), GoldLight.copy(alpha = 0.9f)))
val HeaderGradient = Brush.verticalGradient(listOf(Color(0xFF1C1810), Black))

/**
 * الخطوط: ضع الملفات داخل app/src/main/assets/fonts/ (من Google Fonts):
 *  - AmiriQuran-Regular.ttf  لنص المصحف
 *  - ArefRuqaa-Bold.ttf      لاسم التطبيق
 * إذا لم توجد، يستخدم التطبيق خط النظام تلقائياً.
 */
object AppFonts {
    var quran: FontFamily = FontFamily.Serif
        private set
    var logo: FontFamily = FontFamily.Serif
        private set

    fun init(context: Context) {
        quran = load(context, "fonts/AmiriQuran-Regular.ttf") ?: FontFamily.Serif
        logo = load(context, "fonts/ArefRuqaa-Bold.ttf") ?: FontFamily.Serif
    }

    private fun load(context: Context, path: String): FontFamily? = try {
        FontFamily(Typeface(android.graphics.Typeface.createFromAsset(context.assets, path)))
    } catch (e: Exception) {
        null
    }
}

@Composable
fun MushafTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Black,
            secondary = GoldLight,
            onSecondary = Black,
            background = Black,
            onBackground = Ink,
            surface = Surface,
            onSurface = Ink,
            surfaceVariant = Surface2,
            onSurfaceVariant = Muted,
            outline = Line,
        ),
        content = content,
    )
}

/** شعار مصحف السيف: سيف قائم من مصحف مفتوح داخل نجمة ثمانية */
@Composable
fun MushafLogo(
    modifier: Modifier,
    background: Color = Black,
    accent: Color = Gold,
    blade: Color = GoldLight,
    drawBackground: Boolean = true,
) {
    Canvas(modifier) {
        val s = size.minDimension / 200f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        if (drawBackground) {
            drawRoundRect(background, size = Size(200 * s, 200 * s), cornerRadius = CornerRadius(46 * s))
            drawRoundRect(
                GoldBorder, topLeft = p(3f, 3f), size = Size(194 * s, 194 * s),
                cornerRadius = CornerRadius(43 * s), style = Stroke(3 * s),
            )
        }
        val square = Size(112 * s, 112 * s)
        drawRect(accent, topLeft = p(44f, 44f), size = square, style = Stroke(3.5f * s))
        rotate(45f, pivot = p(100f, 100f)) {
            drawRect(accent, topLeft = p(44f, 44f), size = square, style = Stroke(3.5f * s))
        }
        drawCircle(background, radius = 50 * s, center = p(100f, 100f))
        drawCircle(accent.copy(alpha = 0.35f), radius = 50 * s, center = p(100f, 100f), style = Stroke(1.2f * s))
        val bladePath = Path().apply {
            moveTo(100 * s, 56 * s); lineTo(105 * s, 66 * s); lineTo(105 * s, 114 * s)
            lineTo(95 * s, 114 * s); lineTo(95 * s, 66 * s); close()
        }
        drawPath(bladePath, blade)
        drawRoundRect(accent, topLeft = p(85f, 113f), size = Size(30 * s, 5 * s), cornerRadius = CornerRadius(2.5f * s))
        drawRoundRect(accent, topLeft = p(97.5f, 118f), size = Size(5 * s, 9 * s), cornerRadius = CornerRadius(1.5f * s))
        val book = Path().apply {
            moveTo(62 * s, 128 * s)
            quadraticBezierTo(81 * s, 119 * s, 100 * s, 129 * s)
            quadraticBezierTo(119 * s, 119 * s, 138 * s, 128 * s)
            lineTo(138 * s, 138 * s)
            quadraticBezierTo(119 * s, 129 * s, 100 * s, 139 * s)
            quadraticBezierTo(81 * s, 129 * s, 62 * s, 138 * s)
            close()
        }
        drawPath(book, accent)
    }
}

/** شارة النجمة الثمانية لأرقام السور */
@Composable
fun StarBadge(modifier: Modifier, color: Color = Gold) {
    Canvas(modifier) {
        val side = size.minDimension * 0.6f
        val tl = Offset((size.width - side) / 2, (size.height - side) / 2)
        drawRect(color, topLeft = tl, size = Size(side, side), style = Stroke(1.5f * density))
        rotate(45f) { drawRect(color, topLeft = tl, size = Size(side, side), style = Stroke(1.5f * density)) }
    }
}

/** زخرفة فاصلة ذهبية: خط رفيع بمعيّن في الوسط */
@Composable
fun GoldDivider(modifier: Modifier) {
    Canvas(modifier) {
        val cy = size.height / 2
        val cx = size.width / 2
        val d = 4f * density
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent)),
            Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f * density,
        )
        val diamond = Path().apply {
            moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close()
        }
        drawPath(diamond, Gold)
    }
}
