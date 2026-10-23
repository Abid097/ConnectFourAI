package com.example.connectfourai

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Bitmap

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var autoButton: Button
    private lateinit var colorSpinner: Spinner
    private lateinit var startsSpinner: Spinner
    private var captureGranted = false
    @Volatile private var autoPlay = false
    private var ourColor = 1 // orange by default
    private var startsFirst = true
    private var lastPlayedSignature: String? = null
    private var lastSeenSignature: String? = null
    private var lastBoard: BoardVision.Detection? = null
    private val requestCapture = 1107

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        ScreenCaptureService.frameListener = { bitmap -> analyzeFrame(bitmap) }
    }

    private fun buildUi() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,28,24,24); setBackgroundColor(0xFF09080D.toInt()) }
        fun label(s:String, size:Float=16f)=TextView(this).apply { text=s; textSize=size; setTextColor(0xFFF4F2F8.toInt()); setPadding(0,10,0,10) }
        root.addView(label("CONNECT FOUR AI",25f))
        root.addView(label("TikPvP auto-player · local screen analysis",14f).apply { setTextColor(0xFFB8B5C2.toInt()) })
        root.addView(label("Your disc color"))
        colorSpinner=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Orange","Pink")); setSelection(0) }
        root.addView(colorSpinner)
        root.addView(label("Who plays first?"))
        startsSpinner=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("I play first","Opponent plays first")); setSelection(0) }
        root.addView(startsSpinner)
        val capture=Button(this).apply { text="1 · Allow screen capture"; setOnClickListener { askCapture() } }
        root.addView(capture, LinearLayout.LayoutParams(-1,-2).apply { topMargin=12 })
        val access=Button(this).apply { text="2 · Enable tap accessibility"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
        root.addView(access,LinearLayout.LayoutParams(-1,-2).apply { topMargin=8 })
        autoButton=Button(this).apply { text="3 · START AUTO-PLAY"; setOnClickListener { if(autoPlay) stopAuto() else startAuto() } }
        root.addView(autoButton,LinearLayout.LayoutParams(-1,-2).apply { topMargin=8 })
        val stop=Button(this).apply { text="EMERGENCY STOP"; setBackgroundColor(0xFF9D244A.toInt()); setTextColor(0xFFFFFFFF.toInt()); setOnClickListener { stopAuto(); stopService(Intent(this@MainActivity,ScreenCaptureService::class.java)) } }
        root.addView(stop,LinearLayout.LayoutParams(-1,-2).apply { topMargin=8 })
        status=label("Status: idle",15f)
        root.addView(status)
        val note=label("Keep TikPvP open and visible. The first screen-capture prompt is required. Auto-play stops if the board cannot be recognized or tapping permission is unavailable. Test in a non-staked game first.",13f)
        note.setTextColor(0xFFFFC36B.toInt()); root.addView(note)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun askCapture() {
        val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(),requestCapture)
    }

    @Deprecated("Android activity result API kept for compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==requestCapture && resultCode==RESULT_OK && data!=null) {
            captureGranted=true
            val intent=Intent(this,ScreenCaptureService::class.java).putExtra(ScreenCaptureService.EXTRA_RESULT_CODE,resultCode).putExtra(ScreenCaptureService.EXTRA_DATA,data)
            if(Build.VERSION.SDK_INT>=26) startForegroundService(intent) else startService(intent)
            status.text="Status: screen capture running. Enable accessibility, then start auto-play."
        } else status.text="Status: screen capture permission was not granted."
    }

    private fun startAuto() {
        ourColor=if(colorSpinner.selectedItemPosition==0) 1 else 2
        startsFirst=startsSpinner.selectedItemPosition==0
        if(!captureGranted) { status.text="Status: allow screen capture first."; return }
        if(TapAccessibilityService.instance==null) {
            status.text="Status: enable Connect Four AI in Android Accessibility settings, then return here."
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return
        }
        autoPlay=true; lastPlayedSignature=null; lastSeenSignature=null
        autoButton.text="AUTO-PLAY IS ON · TAP TO PAUSE"
        status.text="Status: running. Keep the game in the foreground."
    }

    private fun stopAuto() { autoPlay=false; autoButton.text="3 · START AUTO-PLAY"; status.text="Status: stopped. Screen capture may still be active." }

    private fun analyzeFrame(bitmap: Bitmap) {
        try {
            if(!autoPlay) return
            val detection=BoardVision.detect(bitmap,ourColor) ?: run { runOnUiThread { status.text="Status: searching for the board…" }; return }
            lastBoard=detection
            val b=detection.board
            val sig=b.joinToString("") { row -> row.joinToString("") }
            if(sig==lastSeenSignature) return
            lastSeenSignature=sig
            val mine=b.sumOf { row -> row.count { it==1 } }
            val theirs=b.sumOf { row -> row.count { it==2 } }
            val ourTurn=if(startsFirst) mine==theirs else mine==theirs-1
            runOnUiThread { status.text="Status: board found · you=$mine opponent=$theirs · ${if(ourTurn) "your turn" else "waiting for opponent"}" }
            if(!ourTurn || sig==lastPlayedSignature) return
            // Choose a tactical move, then verify that the board changes before any further action.
            val col=ConnectFourAI.chooseMove(b,1,7)
            if(col !in 0..6) { runOnUiThread { status.text="Status: board full; stopping." }; stopAuto(); return }
            val x=(detection.left+(col+0.5)*detection.width/7.0).toFloat()
            val y=(detection.top+detection.height*0.08).toFloat()
            lastPlayedSignature=sig
            val tapped=TapAccessibilityService.instance?.tap(x,y)
            if(tapped!=true) { runOnUiThread { status.text="Status: tap failed; auto-play stopped." }; stopAuto() }
            else runOnUiThread { status.text="Status: tapped column ${col+1}; verifying move…" }
        } catch (_: Exception) {
            runOnUiThread { status.text="Status: analysis error; stopped for safety."; stopAuto() }
        } finally { if(!bitmap.isRecycled) bitmap.recycle() }
    }

    override fun onDestroy() { autoPlay=false; if(ScreenCaptureService.frameListener!=null) ScreenCaptureService.frameListener=null; super.onDestroy() }
}
