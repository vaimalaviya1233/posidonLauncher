package posidon.launcher.tools.theme

import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.graphics.*
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import io.posidon.android.conveniencelib.AnimUtils
import io.posidon.android.conveniencelib.drawable.MaskedDrawable
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.sp
import io.posidon.android.conveniencelib.units.toFloatPixels
import posidon.launcher.Global
import posidon.launcher.Home
import posidon.launcher.drawable.ContactDrawable
import posidon.launcher.drawable.FastColorDrawable
import posidon.launcher.drawable.NonDrawable
import posidon.launcher.storage.Settings
import posidon.launcher.tools.Tools
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object Icons {

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun generateAdaptiveIcon(drawable: Drawable, iconShape: IconShape = IconShape(Settings["icshape", 4])): Drawable {
        if (drawable is AdaptiveIconDrawable) {
            val tmp = LayerDrawable(arrayOf(drawable.background ?: NonDrawable(), drawable.foreground ?: NonDrawable()))
            val w = tmp.intrinsicWidth
            val h = tmp.intrinsicHeight
            tmp.setLayerInset(0, -w / 6, -h / 6, -w / 6, -h / 6)
            tmp.setLayerInset(1, -w / 6, -h / 6, -w / 6, -h / 6)
            val width = tmp.intrinsicWidth
            val height = tmp.intrinsicHeight
            tmp.setBounds(0, 0, width, height)
            return if (iconShape.isSquare) tmp
                else BitmapDrawable(Tools.appContext!!.resources, MaskedDrawable(tmp, iconShape.getPath(width, height)).toBitmap())
        }
        return drawable
    }

    fun applyInsets(icon: Drawable): Drawable {
        return LayerDrawable(arrayOf(icon)).apply {
            val diameter = max(intrinsicWidth, intrinsicHeight)
            val p = 8 * diameter / Settings["drawer:icons:size", 64]
            setLayerInset(0, p, p, p, p)
        }.let {
            if (icon is BitmapDrawable) {
                BitmapDrawable(Tools.appContext!!.resources, it.toBitmap())
            } else it
        }
    }

    fun badge(icon: Drawable, badge: Drawable, icSizeDP: Int): Drawable {
        val drawable = LayerDrawable(arrayOf(icon, badge))
        val diameter = max(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val p = 8 * diameter / icSizeDP
        drawable.setLayerInset(0, p, p, p, p)
        val o = diameter - (20.sp.toFloatPixels(Tools.appContext!!) * diameter / icSizeDP.dp.toFloatPixels(Tools.appContext!!)).toInt()
        drawable.setLayerInset(1, o, o, 0, 0)
        return if (icon is BitmapDrawable) {
            BitmapDrawable(Tools.appContext!!.resources, drawable.toBitmap())
        } else drawable
    }

    private val pics = HashMap<Int, ContactDrawable>()
    fun generateContactPicture(name: String, tmpLab: DoubleArray, paint: Paint): Drawable? {
        if (name.isEmpty()) return null
        val realName = name.trim { !it.isLetterOrDigit() }.uppercase()
        if (realName.isEmpty()) return null
        val key = (realName[0].code shl 16) + realName[realName.length / 2].code
        return pics.getOrPut(key) {
            val random = Random(key)
            val base = Color.HSVToColor(floatArrayOf(random.nextFloat() * 360f, 1f, 1f))
            ColorUtils.colorToLAB(base, tmpLab)
            ContactDrawable(
                ColorUtils.LABToColor(
                    50.0,
                    tmpLab[1] / 2.0,
                    tmpLab[2] / 2.0
                ),
                realName[0],
                paint
            )
        }
    }

    fun generateNotificationBadgeBGnFG(icon: Drawable? = null, onGenerated: (bg: Drawable, fg: Int) -> Unit) {
        val bgType = Settings["notif:badges:bg_type", 0]
        val customBG = Settings["notif:badges:bg_color", 0xffff5555.toInt()]
        if (icon != null && bgType == 0) {
            Palette.from(icon.toBitmap()).generate {
                val bg = it?.getDominantColor(customBG) ?: customBG
                onGenerated(ColorTools.iconBadge(bg), if (bg.luminance > .6f) 0xff111213.toInt() else 0xffffffff.toInt())
            }
        } else if (bgType == 1) {
            val bg = Global.accentColor
            onGenerated(ColorTools.iconBadge(bg), if (bg.luminance > .6f) 0xff111213.toInt() else 0xffffffff.toInt())
        } else {
            onGenerated(ColorTools.iconBadge(customBG), if (customBG.luminance > .6f) 0xff111213.toInt() else 0xffffffff.toInt())
        }
    }

    inline fun animateIfShould(context: Context, drawable: Drawable) {
        if (!(context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode && Settings["animatedicons", true]) {
            try {
                AnimUtils.tryAnimate(Home.instance, drawable)
            } catch (e: Exception) {}
        }
    }

    inline class IconShape(val int: Int) {
        inline val isSquare get() = int == 3
        inline val isSystem get() = int == 0

        fun getPath(width: Int, height: Int): Path {
            val minSize = min(width, height)
            if (isSystem) {
                val path = AdaptiveIconDrawable(null, null).iconMask
                val rect = RectF()
                path.computeBounds(rect, true)
                val matrix = Matrix()
                matrix.setScale(minSize / rect.right, minSize / rect.bottom)
                path.transform(matrix)
                path.fillType = Path.FillType.INVERSE_EVEN_ODD
                return path
            } else {
                val path = Path()
                when (int) {
                    1 -> path.addCircle(width / 2f, height / 2f, minSize / 2f - 2, Path.Direction.CCW)
                    2 -> path.addRoundRect(2f, 2f, width - 2f, height - 2f, minSize / 4f, minSize / 4f, Path.Direction.CCW)
                    4 -> { //Formula: (|x|)^3 + (|y|)^3 = radius^3
                        val xx = 2
                        val yy = 2
                        val radius = minSize / 2 - 2
                        val radiusToPow = radius * radius * radius.toDouble()
                        path.moveTo(-radius.toFloat(), 0f)
                        for (x in -radius..radius) {
                            path.lineTo(x.toFloat(), Math.cbrt(radiusToPow - abs(x * x * x)).toFloat())
                        }
                        for (x in radius downTo -radius) {
                            path.lineTo(x.toFloat(), (-Math.cbrt(radiusToPow - abs(x * x * x))).toFloat())
                        }
                        path.close()
                        val matrix = Matrix()
                        matrix.postTranslate(xx + radius.toFloat(), yy + radius.toFloat())
                        path.transform(matrix)
                    }
                }
                path.fillType = Path.FillType.INVERSE_EVEN_ODD
                return path
            }
        }
    }
}