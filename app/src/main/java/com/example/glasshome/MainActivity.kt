package com.example.glasshome

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class MainActivity : Activity() {
    private lateinit var row: LinearLayout
    private lateinit var greet: TextView
    private lateinit var dock: View
    private var bob: ValueAnimator? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun white(a: Int) = Color.argb(a, 255, 255, 255)
    private fun glass(a: Int, stroke: Int, r: Int, w: Int = 1) = GradientDrawable().apply {
        cornerRadius = dp(r).toFloat(); setColor(white(a)); setStroke(dp(w), white(stroke))
    }
    private fun text(sp: Float, a: Int, face: String) = TextView(this).apply {
        textSize = sp; setTextColor(white(a)); typeface = Typeface.create(face, Typeface.NORMAL)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = FrameLayout(this)
        root.addView(AuroraView(this))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(64), dp(44), dp(64), dp(36))
        }
        greet = text(14f, 170, "sans-serif-medium").apply { letterSpacing = 0.25f; isAllCaps = true }
        col.addView(greet)
        col.addView(TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "h:mm"; textSize = 80f
            setTextColor(white(255)); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        })
        col.addView(TextClock(this).apply {
            format24Hour = "EEEE, d MMMM"; format12Hour = "EEEE, d MMMM"; textSize = 18f
            setTextColor(white(180)); typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        })
        col.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }
        dock = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; clipChildren = false; clipToPadding = false
            setPadding(dp(24), dp(26), dp(24), dp(22)); background = glass(34, 80, 30); addView(row)
        }
        col.addView(dock, LinearLayout.LayoutParams(-1, -2))
        root.addView(col)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        greet.text = when { h < 5 -> "Good night"; h < 12 -> "Good morning"; h < 18 -> "Good afternoon"; else -> "Good evening" }
        row.removeAllViews()
        val pm = packageManager
        fun q(cat: String) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
        val apps = (q(Intent.CATEGORY_LEANBACK_LAUNCHER) + q(Intent.CATEGORY_LAUNCHER))
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        for (ri in apps) {
            val ai = ri.activityInfo
            val banner = ai.loadBanner(pm)
            row.addView(tile(ri.loadLabel(pm).toString(), banner ?: ri.loadIcon(pm), banner != null) {
                try {
                    startActivity(Intent(Intent.ACTION_MAIN).setClassName(ai.packageName, ai.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {}
            })
        }
        row.getChildAt(0)?.requestFocus()
        bob = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
            duration = 7000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { dock.translationY = sin(it.animatedValue as Float) * dp(4) }
            start()
        }
    }

    override fun onPause() { bob?.cancel(); super.onPause() }
    @Suppress("OVERRIDE_DEPRECATION") override fun onBackPressed() {}

    private fun tile(name: String, pic: Drawable, banner: Boolean, go: () -> Unit): View {
        val card = FrameLayout(this).apply {
            background = glass(40, 70, 14)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, dp(14).toFloat()) }
            }
            addView(ImageView(context).apply {
                setImageDrawable(pic)
                if (banner) scaleType = ImageView.ScaleType.CENTER_CROP
                else { scaleType = ImageView.ScaleType.FIT_CENTER; setPadding(dp(24), dp(10), dp(24), dp(10)) }
            }, FrameLayout.LayoutParams(-1, -1))
        }
        val label = text(12f, 190, "sans-serif-medium").apply {
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER; text = name; setPadding(0, dp(8), 0, 0)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; isFocusable = true; isClickable = true
            addView(card, dp(172), dp(97)); addView(label, dp(172), -2)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(10), 0, dp(10), 0) }
            setOnClickListener { go() }
            setOnFocusChangeListener { v, f ->
                card.background = if (f) glass(70, 235, 14, 2) else glass(40, 70, 14)
                label.alpha = if (f) 1f else 0.7f
                v.animate().scaleX(if (f) 1.1f else 1f).scaleY(if (f) 1.1f else 1f)
                    .translationY(if (f) -dp(6).toFloat() else 0f).setDuration(180).start()
            }
        }
    }
}

class AuroraView(c: Context) : View(c) {
    private val cols = arrayOf(Color.rgb(255, 138, 91), Color.rgb(91, 140, 255), Color.rgb(176, 101, 255))
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        cv.drawColor(Color.rgb(14, 18, 32))
        val t = System.nanoTime() / 1e9f
        val r = max(width, height) * 0.5f
        for (i in cols.indices) {
            val cx = width * (0.5f + 0.38f * sin(t * 0.12f + i * 2.1f))
            val cy = height * (0.5f + 0.32f * cos(t * 0.09f + i * 1.7f))
            val c = cols[i]
            p.shader = RadialGradient(cx, cy, r, Color.argb(120, Color.red(c), Color.green(c), Color.blue(c)),
                Color.argb(0, Color.red(c), Color.green(c), Color.blue(c)), Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        }
        postInvalidateDelayed(40)
    }
}
