package posidon.launcher.view.drawer

import android.content.ComponentName
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.UserHandle
import android.util.AttributeSet
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.graphics.luminance
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import io.posidon.android.conveniencelib.Device
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.sp
import io.posidon.android.conveniencelib.units.toFloatPixels
import io.posidon.android.conveniencelib.units.toPixels
import posidon.launcher.R
import posidon.launcher.items.App
import posidon.launcher.items.Folder
import posidon.launcher.items.LauncherItem
import posidon.launcher.items.PinnedShortcut
import posidon.launcher.search.SearchActivity
import posidon.launcher.storage.Settings
import posidon.launcher.tools.Dock
import posidon.launcher.tools.Tools
import posidon.launcher.tools.theme.Customizer
import posidon.launcher.tools.theme.Icons
import java.util.*
import kotlin.math.abs
import kotlin.math.min

class DockView : LinearLayout {

    var onItemClick: (Context, View, dockI: Int, item: LauncherItem) -> Unit = { _, _, _, _ -> }
    var onItemLongClick: (Context, View, dockI: Int, item: LauncherItem) -> Boolean = { _, _, _, _ -> false }

    fun updateTheme(drawer: View) {
        layoutParams = (layoutParams as MarginLayoutParams).apply {
            val m = Settings["dock:margin_x", 16].dp.toPixels(context)
            leftMargin = m
            rightMargin = m
        }
        val bgColor = Settings["dock:background_color", 0xff242424.toInt()]
        when (Settings["dock:background_type", 0]) {
            1 -> {
                containerContainer.background = null
                drawer.background = LayerDrawable(arrayOf(
                    GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(bgColor and 0x00ffffff, bgColor)),
                    GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(bgColor,
                        Settings["drawer:background_color", 0xa4171717.toInt()]))
                ))
            }
            2 -> {
                val r = Settings["dock:radius", 0].dp.toFloatPixels(context)
                drawer.background = ShapeDrawable().apply {
                    shape = RoundRectShape(floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f), null, null)
                    paint.color = 0
                }
                containerContainer.background = ShapeDrawable().apply {
                    shape = RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)
                    paint.color = bgColor
                }
            }
            else -> {
                containerContainer.background = null
                drawer.background = ShapeDrawable().apply {
                    val tr = Settings["dock:radius", 0].dp.toFloatPixels(context)
                    shape = RoundRectShape(floatArrayOf(tr, tr, tr, tr, 0f, 0f, 0f, 0f), null, null)
                    paint.color = bgColor
                }
            }
        }
        if (!Settings["docksearchbarenabled", false]) {
            searchBar.visibility = GONE
            battery.visibility = GONE
            return
        }
        searchBar.visibility = VISIBLE
        battery.visibility = VISIBLE
        if (Settings["dock:search:below_apps", true]) { searchBar.bringToFront() }
        else { containerContainer.bringToFront() }
        run {
            val color = Settings["docksearchtxtcolor", -0x1000000]
            searchTxt.setTextColor(color)
            searchIcon.imageTintList = ColorStateList.valueOf(color)
            battery.progressTintList = ColorStateList.valueOf(color)
            battery.indeterminateTintMode = PorterDuff.Mode.MULTIPLY
            battery.progressBackgroundTintList = ColorStateList.valueOf(color)
            battery.progressBackgroundTintMode = PorterDuff.Mode.MULTIPLY
            (battery.progressDrawable as LayerDrawable).getDrawable(3).setTint(if (color.luminance > .6f) -0x23000000 else -0x11000001)
        }
        searchBar.background = ShapeDrawable().apply {
            val r = Settings["dock:search:radius", 30].dp.toFloatPixels(context)
            shape = RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null)
            paint.color = Settings["docksearchcolor", -0x22000001]
        }
    }

    constructor(c: Context) : super(c)
    constructor(c: Context, a: AttributeSet?) : super(c, a)
    constructor(c: Context, a: AttributeSet?, sa: Int) : super(c, a, sa)

    var dockHeight = 0

    val searchIcon = ImageView(context).apply {
        run {
            val p = 8.dp.toPixels(context)
            setPadding(p, p, p, p)
        }
        setImageResource(R.drawable.ic_search)
        imageTintList = ColorStateList.valueOf(0xffffffff.toInt())
    }

    val searchTxt = TextView(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        textSize = 16f
    }

    val battery = ProgressBar(context, null, R.style.Widget_AppCompat_ProgressBar_Horizontal).apply {
        run {
            val p = 10.dp.toPixels(context)
            setPadding(p, p, p, p)
        }
        max = 100
        progressDrawable = context.getDrawable(R.drawable.battery_bar)
    }

    val searchBar = LinearLayout(context).apply {

        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        setOnClickListener {
            SearchActivity.open(context)
        }

        addView(searchIcon, LayoutParams(56.dp.toPixels(context), 48.dp.toPixels(context)))
        addView(searchTxt, LayoutParams(0, 48.dp.toPixels(context), 1f))
        addView(battery, LayoutParams(56.dp.toPixels(context), 48.dp.toPixels(context)).apply {
            marginEnd = 8.dp.toPixels(context)
        })
    }

    val container = GridLayout(context)

    val containerContainer = FrameLayout(context).apply {
        addView(container, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            this.gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 8.dp.toPixels(context)
            bottomMargin = 8.dp.toPixels(context)
        })
    }

    init {
        gravity = Gravity.CENTER_HORIZONTAL
        orientation = VERTICAL
        addView(searchBar, LayoutParams(MATCH_PARENT, 48.dp.toPixels(context)).apply {
            topMargin = 10.dp.toPixels(context)
            bottomMargin = 10.dp.toPixels(context)
        })
        addView(containerContainer, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    fun loadAppsAndUpdateHome(drawer: DrawerView, feed: View, desktopContent: View) {
        loadApps()
        updateDimensions(drawer, feed, desktopContent)
    }

    fun loadApps() {
        val columnCount = Settings["dock:columns", 5]
        val marginX = Settings["dock:margin_x", 16].dp.toPixels(context)
        val appSize = min(Settings["dock:icons:size", 74].dp.toPixels(context), (Device.screenWidth(context) - marginX * 2) / columnCount)
        val rowCount = Settings["dock:rows", 2]
        val showLabels = Settings["dock:labels:enabled", false]
        val notifBadgesEnabled = Settings["notif:badges", true]
        val notifBadgesShowNum = Settings["notif:badges:show_num", true]
        container.run {
            removeAllViews()
            this.columnCount = columnCount
            this.rowCount = rowCount
        }
        loop@ for (i in 0 until columnCount * rowCount) {
            val view = LayoutInflater.from(context).inflate(R.layout.drawer_item, container, false)
            view.findViewById<View>(R.id.iconFrame).run {
                layoutParams.height = appSize
                layoutParams.width = appSize
            }
            val label = view.findViewById<TextView>(R.id.icontxt)
            val badge = view.findViewById<TextView>(R.id.notificationBadge)
            val item = Dock[i]
            if (item == null) {
                label.isVisible = false
                view.isInvisible = true
                badge.isVisible = false
            } else {
                if (showLabels) {
                    label.text = item.label
                    Customizer.styleLabel("dock:labels", label, -0x11111112, 12)
                } else {
                    label.isVisible = false
                }
                when (item) {
                    is PinnedShortcut -> {
                        if (!item.isInstalled(context.packageManager)) {
                            Dock[i] = null
                            continue@loop
                        }
                    }
                    is App -> {
                        if (!item.isInstalled(context.packageManager)) {
                            Dock[i] = null
                            continue@loop
                        }
                    }
                    is Folder -> item.updateIcon()
                }
                view.findViewById<ImageView>(R.id.iconimg).also { img ->
                    img.setImageDrawable(item.icon)
                }
                if (notifBadgesEnabled) {
                    val c = item.notificationCount
                    if (c != 0) {
                        badge.visibility = View.VISIBLE
                        badge.text = if (notifBadgesShowNum) c.toString() else ""
                        Icons.generateNotificationBadgeBGnFG(item.icon) { bg, fg ->
                            badge.background = bg
                            badge.setTextColor(fg)
                        }
                    } else badge.isVisible = false
                } else badge.isVisible = false
                view.setOnClickListener {
                    onItemClick(context, view, i, item)
                }
                view.setOnLongClickListener { view ->
                    onItemLongClick(context, view, i, item)
                }
            }
            container.addView(view)
        }
    }

    fun updateDimensions(drawer: DrawerView, feed: View, desktopContent: View) {
        val columnCount = Settings["dock:columns", 5]
        val marginX = Settings["dock:margin_x", 16].dp.toPixels(context)
        val appSize = min(Settings["dock:icons:size", 74].dp.toPixels(context), (Device.screenWidth(context) - marginX * 2) / columnCount)
        val rowCount = Settings["dock:rows", 2]
        val containerHeight = (appSize + if (Settings["dock:labels:enabled", false]) (Settings["dock:labels:text_size", 12].sp.toFloatPixels(context) + 4.dp.toFloatPixels(context)).toInt() else 0) * rowCount
        dockHeight = if (Settings["docksearchbarenabled", false] && !Device.isTablet(resources)) {
            containerHeight + 84.dp.toPixels(context)
        } else {
            containerHeight + 14.dp.toPixels(context)
        }
        val addition = drawer.scrollBar.updateTheme(drawer, feed, this)
        dockHeight += addition
        container.layoutParams.height = containerHeight
        setPadding(0, 0, 0, addition)
        drawer.peekHeight = dockHeight + Tools.navbarHeight + Settings["dockbottompadding", 10].dp.toPixels(context)
        drawer.drawerContent.layoutParams.height = Device.screenHeight(context) + Tools.navbarHeight
        if (Settings["feed:show_behind_dock", false]) {
            desktopContent.setPadding(0, 12.dp.toPixels(context), 0, dockHeight + Tools.navbarHeight + Settings["dockbottompadding", 10].dp.toPixels(context))
        } else {
            (feed.layoutParams as MarginLayoutParams).run {
                bottomMargin = dockHeight + Tools.navbarHeight + (Settings["dockbottompadding", 10] - 18).dp.toPixels(context)
            }
            desktopContent.setPadding(0, 12.dp.toPixels(context), 0, 24.dp.toPixels(context))
        }
        run {
            val bg = drawer.background
            if (Settings["dock:background_type", 0] == 1 && bg is LayerDrawable) {
                bg.setLayerInset(0, 0, 0, 0, (Device.screenHeight(context) + Tools.navbarHeight) - drawer.peekHeight)
                bg.setLayerInset(1, 0, drawer.peekHeight, 0, 0)
            }
        }
    }

    inline fun onItemDrop(event: DragEvent): Boolean {
        if (event.y > Device.screenHeight(context) - dockHeight) {
            val item = LauncherItem(event.clipData.description.label.toString())!!
            val location = IntArray(2)
            var i = 0
            while (i < container.childCount) {
                container.getChildAt(i).getLocationOnScreen(location)
                val threshHold = min(container.getChildAt(i).height / 2.toFloat(), 100.dp.toFloatPixels(context))
                if (abs(location[0] - (event.x - container.getChildAt(i).height / 2f)) < threshHold && abs(location[1] - (event.y - container.getChildAt(i).height / 2f)) < threshHold) {
                    Dock.add(item, i)
                    loadApps()
                    return true
                }
                i++
            }
        }
        return false
    }

    fun getPositionOnScreen(i: Int, rectF: RectF) {
        val v = container
        val columns = container.columnCount
        val iconWidth = v.measuredWidth.toFloat() / columns
        val location = intArrayOf(0, 0)
        v.getLocationOnScreen(location)
        val left = location[0].toFloat() + iconWidth * (i % columns)
        val bottom = Device.screenHeight(context) - location[0].toFloat() + iconWidth * (i / columns)
        val right = left + iconWidth
        val top = bottom - v.measuredHeight.toFloat()
        rectF.set(left, top, right, bottom)
    }

    /**
     * Sets [outLocation] to the location of the icon
     * @return index in dock
     * If icon isn't in the dock, outLocation will be zeroed out and -1 will be returned
     */
    fun getLocationForApp(c: ComponentName, user: UserHandle, outLocation: RectF): Int {
        val packageName = c.packageName
        val list = LinkedList<Pair<String, Int>>()
        for ((item, i) in Dock.indexed()) {
            when (item) {
                is App -> if (
                        item.packageName == packageName &&
                        item.userHandle == user
                ) list.add(item.name to i)
                is Folder -> for (folderItem in item.items) {
                    if (folderItem is App &&
                            folderItem.packageName == packageName &&
                            folderItem.userHandle == user) {
                        list.add(folderItem.name to i)
                        break
                    }
                }
            }
        }
        val className = c.className
        for ((name, i) in list) if (name == className) {
            getPositionOnScreen(i, outLocation)
            return i
        }
        var retI = -1
        list.firstOrNull()?.let {
            retI = it.second
            getPositionOnScreen(it.second, outLocation)
        }
        return retI
    }
}