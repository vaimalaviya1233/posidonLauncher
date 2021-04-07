package posidon.launcher.view.groupView

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewParent
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.main.view.*
import posidon.android.conveniencelib.dp
import posidon.android.conveniencelib.sp
import posidon.launcher.R
import posidon.launcher.items.App
import posidon.launcher.items.LauncherItem
import posidon.launcher.items.users.ItemLongPress
import posidon.launcher.storage.Settings
import posidon.launcher.tools.Tools
import posidon.launcher.tools.theme.Customizer
import posidon.launcher.tools.theme.Icons
import posidon.launcher.view.drawer.DrawerView
import kotlin.math.min

@SuppressLint("ViewConstructor")
class AppSectionView(drawer: DrawerView) : ItemGroupView(drawer.context) {

    private val appSize = Tools.appContext!!.dp(when (Settings["icsize", 1]) {
            0 -> 64
            2 -> 84
            else -> 74
        }).toInt()

    init {
        when (Settings["drawer:sec_name_pos", 0]) {
            0 -> {
                orientation = VERTICAL
            }
            1 -> {
                orientation = HORIZONTAL
                textView.layoutParams.width = context.sp(28).toInt()
                textView.run {
                    setPadding(0, 0, 0, 0)
                    gravity = Gravity.END
                }
            }
        }

        gridLayout.run {
            columnCount = Settings["drawer:columns", 4]
            if (Settings["drawer:columns", 4] > 2) {
                setPaddingRelative(Tools.appContext!!.dp(12).toInt(), 0, 0, 0)
            }
        }
        textView.run {
            setTextColor(Settings["drawer:labels:color", -0x11111112])
        }
    }

    override fun getItemView (item: LauncherItem, parent: ViewParent): View {
        item as App
        val columns = Settings["drawer:columns", 4]
        val parentWidth = (parent as View).measuredWidth
        return (if (columns > 2) {
            LayoutInflater.from(context).inflate(R.layout.drawer_item, gridLayout, false).apply {
                layoutParams.width = min(if (Settings["drawer:scrollbar:enabled", false]) {
                    parentWidth - dp(24).toInt() - /* Scrollbar width -> */ dp(24).toInt()
                } else { parentWidth - dp(24).toInt() } / columns, appSize)
            }
        } else LayoutInflater.from(context).inflate(R.layout.list_item, gridLayout, false).apply {
            if (columns == 2) {
                layoutParams.width = if (Settings["drawer:scrollbar:enabled", false]) {
                    parentWidth - dp(24).toInt() - /* Scrollbar width -> */ dp(24).toInt()
                } else { parentWidth - dp(24).toInt() } / 2
            }
        }).apply {
            findViewById<ImageView>(R.id.iconimg).setImageDrawable(item.icon)
            findViewById<View>(R.id.iconFrame).run {
                layoutParams.height = appSize
                layoutParams.width = appSize
            }
            findViewById<TextView>(R.id.icontxt).run {
                if (Settings["labelsenabled", false]) {
                    text = item.label
                    visibility = View.VISIBLE
                    Customizer.styleLabel("drawer:labels", this, -0x11111112, 12f)
                } else visibility = View.GONE
            }
            findViewById<TextView>(R.id.notificationBadge).run {
                if (Settings["notif:badges", true] && item.notificationCount != 0) {
                    visibility = View.VISIBLE
                    text = if (Settings["notif:badges:show_num", true]) item.notificationCount.toString() else ""
                    Icons.generateNotificationBadgeBGnFG(item.icon!!) { bg, fg ->
                        background = bg
                        setTextColor(fg)
                    }
                } else { visibility = View.GONE }
            }
            setOnClickListener { item.open(context, it) }
            setOnLongClickListener {
                ItemLongPress.onItemLongPress(context, it, item, null, {
                    item.setHidden()
                    drawer.loadApps()
                }, isRemoveFnActuallyHide = true)
                true
            }
            (layoutParams as GridLayout.LayoutParams).bottomMargin = dp(Settings["verticalspacing", 12]).toInt()
        }
    }
}