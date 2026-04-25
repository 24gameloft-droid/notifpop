package com.mydev.notifpop

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private var wm: WindowManager? = null
    private var view: LinearLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    companion object {
        var instance: OverlayService? = null
        fun show(ctx: Context, app: String, title: String, text: String, pkg: String) {
            ctx.startService(Intent(ctx, OverlayService::class.java).apply {
                action = "SHOW"
                putExtra("app", app); putExtra("title", title)
                putExtra("text", text); putExtra("pkg", pkg)
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val ch = NotificationChannel("pop", "Popup", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        startForeground(1, Notification.Builder(this, "pop").setContentTitle("Notif Pop Active").setSmallIcon(android.R.drawable.ic_dialog_info).build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "SHOW") {
            showPopup(
                intent.getStringExtra("app") ?: "",
                intent.getStringExtra("title") ?: "",
                intent.getStringExtra("text") ?: "",
                intent.getStringExtra("pkg") ?: ""
            )
        }
        return START_STICKY
    }

    private fun showPopup(app: String, title: String, text: String, pkg: String) {
        handler.post {
            removeView()
            val prefs = getSharedPreferences("np_prefs", Context.MODE_PRIVATE)
            val r = prefs.getInt("r", 30); val g = prefs.getInt("g", 30)
            val b = prefs.getInt("b", 46); val a = prefs.getInt("a", 204)
            val color = (a shl 24) or (r shl 16) or (g shl 8) or b

            val v = LayoutInflater.from(this).inflate(R.layout.popup, null) as LinearLayout
            val bg = GradientDrawable().apply { setColor(color); cornerRadius = 48f; setStroke(2, 0x40FFFFFF) }
            v.background = bg
            v.findViewById<TextView>(R.id.popup_app).text = app
            v.findViewById<TextView>(R.id.popup_title).text = title
            v.findViewById<TextView>(R.id.popup_text).text = text
            try { v.findViewById<ImageView>(R.id.popup_icon).setImageBitmap(toBmp(packageManager.getApplicationIcon(pkg))) } catch (e: Exception) {}

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP; x = 16; y = 80 }

            wm?.addView(v, params)
            view = v
            hideRunnable?.let { handler.removeCallbacks(it) }
            hideRunnable = Runnable { removeView() }.also { handler.postDelayed(it, 5000) }
        }
    }

    private fun removeView() {
        view?.let { try { wm?.removeView(it) } catch (e: Exception) {}; view = null }
    }

    private fun toBmp(d: Drawable): Bitmap {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
        val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h); d.draw(Canvas(bmp)); return bmp
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); removeView(); instance = null }
}
