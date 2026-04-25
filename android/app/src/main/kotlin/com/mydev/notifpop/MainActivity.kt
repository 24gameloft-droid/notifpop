package com.mydev.notifpop

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val CH = "com.mydev.notifpop/ch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        if (!isNLEnabled())
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        startService(Intent(this, OverlayService::class.java))
    }

    override fun configureFlutterEngine(fe: FlutterEngine) {
        super.configureFlutterEngine(fe)
        val prefs = getSharedPreferences("np_prefs", Context.MODE_PRIVATE)

        MethodChannel(fe.dartExecutor.binaryMessenger, CH).setMethodCallHandler { call, result ->
            when (call.method) {
                "load" -> {
                    val pm = packageManager
                    val list = mutableListOf<Pair<String,String>>()
                    val i = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                    for (ri in pm.queryIntentActivities(i, 0)) try {
                        val info = ri.activityInfo.applicationInfo
                        list.add(pm.getApplicationLabel(info).toString() to info.packageName)
                    } catch (e: Exception) {}
                    list.sortBy { it.first }
                    val apps = JSONArray()
                    for ((n, p) in list) apps.put(JSONObject().apply { put("n", n); put("p", p) })
                    result.success(JSONObject().apply {
                        put("apps", apps)
                        put("allowed", JSONArray(prefs.getStringSet("allowed", emptySet())?.toList() ?: emptyList<String>()))
                        put("r", prefs.getInt("r", 30)); put("g", prefs.getInt("g", 30))
                        put("b", prefs.getInt("b", 46)); put("a", prefs.getInt("a", 204))
                        put("hasOverlay", Settings.canDrawOverlays(this@MainActivity))
                        put("hasNL", isNLEnabled())
                    }.toString())
                }
                "save" -> {
                    val allowed = call.argument<List<String>>("allowed") ?: emptyList()
                    prefs.edit().putStringSet("allowed", allowed.toSet())
                        .putInt("r", call.argument<Int>("r") ?: 30)
                        .putInt("g", call.argument<Int>("g") ?: 30)
                        .putInt("b", call.argument<Int>("b") ?: 46)
                        .putInt("a", call.argument<Int>("a") ?: 204).apply()
                    result.success(true)
                }
                "requestOverlay" -> { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); result.success(true) }
                "requestNL" -> { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); result.success(true) }
                "testPopup" -> { OverlayService.show(this, "Notif Pop", "Test Notification", "This popup will appear for 5 seconds!", packageName); result.success(true) }
                else -> result.notImplemented()
            }
        }
    }

    private fun isNLEnabled() = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
}
