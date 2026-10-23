package com.example.connectfourai

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureService : Service() {
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastFrameAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, notification())
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
        if (data != null && resultCode == Activity.RESULT_OK && display == null) startCapture(resultCode, data)
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
        val width=metrics.widthPixels; val height=metrics.heightPixels; val density=metrics.densityDpi
        thread=HandlerThread("screen-capture").also { it.start() }; handler=Handler(thread!!.looper)
        reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2)
        reader!!.setOnImageAvailableListener({ ir ->
            val now=System.currentTimeMillis()
            if (now-lastFrameAt < 650) { ir.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
            val image=ir.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane=image.planes[0]; val buffer=plane.buffer; val pixelStride=plane.pixelStride; val rowStride=plane.rowStride
                val paddedWidth=rowStride/pixelStride
                val raw=Bitmap.createBitmap(paddedWidth,height,Bitmap.Config.ARGB_8888)
                raw.copyPixelsFromBuffer(buffer)
                val bitmap=Bitmap.createBitmap(raw,0,0,width,height); raw.recycle()
                lastFrameAt=now
                frameListener?.invoke(bitmap)
            } catch (_: Exception) { } finally { image.close() }
        },handler)
        val mgr=getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        projection=mgr.getMediaProjection(resultCode,data)
        display=projection?.createVirtualDisplay("ConnectFourAI",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,handler)
        projection?.registerCallback(object: android.media.projection.MediaProjection.Callback(){ override fun onStop(){ stopSelf() } },handler)
    }

    override fun onDestroy() {
        frameListener=null; display?.release(); reader?.close(); projection?.stop(); projection=null
        thread?.quitSafely(); super.onDestroy()
    }
    private fun notification(): Notification {
        val channel="connect_four_ai_capture"
        if (Build.VERSION.SDK_INT >= 26) (getSystemService(NotificationManager::class.java)).createNotificationChannel(NotificationChannel(channel,"Game screen capture",NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this,channel).setContentTitle("Connect Four AI is active").setContentText("Screen analysis is running. Stop it from the app.").setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true).build()
    }
    companion object {
        const val EXTRA_RESULT_CODE="resultCode"; const val EXTRA_DATA="projectionData"; const val ACTION_STOP="stopCapture"; const val NOTIFICATION_ID=401
        @Volatile var frameListener: ((Bitmap)->Unit)? = null
        private var projection: android.media.projection.MediaProjection? = null
    }
}
