package launcherutils

import android.content.Intent
import posidon.launcher.tools.Tools

object Kustom {

    private const val KUSTOM_ACTION = "org.kustom.action.SEND_VAR"
    private const val KUSTOM_ACTION_EXT_NAME = "org.kustom.action.EXT_NAME"
    private const val KUSTOM_ACTION_VAR_NAME = "org.kustom.action.VAR_NAME"
    private const val KUSTOM_ACTION_VAR_VALUE = "org.kustom.action.VAR_VALUE"
    private const val KUSTOM_ACTION_VAR_NAME_ARRAY = "org.kustom.action.VAR_NAME_ARRAY"
    private const val KUSTOM_ACTION_VAR_VALUE_ARRAY = "org.kustom.action.VAR_VALUE_ARRAY"

    operator fun set(name: String, variable: String, value: String) {
        val intent = Intent(KUSTOM_ACTION)
        intent.putExtra(KUSTOM_ACTION_EXT_NAME, name)
        intent.putExtra(KUSTOM_ACTION_VAR_NAME, variable)
        intent.putExtra(KUSTOM_ACTION_VAR_VALUE, value)
        Tools.appContext!!.sendBroadcast(intent)
    }
}