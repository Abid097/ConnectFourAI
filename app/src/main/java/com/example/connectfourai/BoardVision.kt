package com.example.connectfourai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/** Lightweight vision for the dark TikPvP Connect Four board shown in the supplied screenshots. */
object BoardVision {
    data class Detection(val board: Array<IntArray>, val left: Int, val top: Int, val width: Int, val height: Int, val confidence: Float)

    fun detect(bitmap: Bitmap, ourColor: Int): Detection? {
        val w=bitmap.width; val h=bitmap.height
        // Find the pink/magenta outline pixels of the board; ignore the bottom emoji bar.
        var minX=w; var maxX=0; var minY=h; var maxY=0; var count=0
        val yStart=(h*0.15).toInt(); val yEnd=(h*0.78).toInt()
        val step=max(1, min(w,h)/500)
        for (y in yStart until yEnd step step) for (x in 0 until w step step) {
            val p=bitmap.getPixel(x,y); val r=Color.red(p); val g=Color.green(p); val b=Color.blue(p)
            if (r in 55..180 && b in 35..125 && r > g*1.45 && b > g*1.15 && absInt(r-b)<100) {
                count++; minX=min(minX,x); maxX=max(maxX,x); minY=min(minY,y); maxY=max(maxY,y)
            }
        }
        if (count < 100 || maxX-minX < w*0.45 || maxY-minY < h*0.20) return null
        // Tighten around likely board outline (edge colors vary with brightness).
        val bw=maxX-minX; val bh=maxY-minY
        if (bw.toFloat()/bh !in 1.0f..1.45f) return null
        val board = Array(6) { IntArray(7) }
        var colored=0
        for (r in 0..5) for (c in 0..6) {
            val cx=(minX+(c+0.5)*bw/7.0).toInt().coerceIn(0,w-1)
            val cy=(minY+(r+0.5)*bh/6.0).toInt().coerceIn(0,h-1)
            // Sample a small central patch; use the strongest orange/pink majority.
            var orange=0; var pink=0; var dark=0
            val rad=max(2, min(bw/7,bh/6)/9)
            for (dy in -rad..rad step max(1,rad/3)) for (dx in -rad..rad step max(1,rad/3)) {
                val p=bitmap.getPixel((cx+dx).coerceIn(0,w-1),(cy+dy).coerceIn(0,h-1))
                val rr=Color.red(p); val gg=Color.green(p); val bb=Color.blue(p)
                if (rr>190 && gg>65 && gg<190 && bb<75) orange++
                else if (rr>200 && gg<115 && bb>90) pink++
                else if (rr<30 && gg<20 && bb<35) dark++
            }
            if (orange>pink && orange>=2) { board[r][c]=if(ourColor==1) 1 else 2; colored++ }
            else if (pink>orange && pink>=2) { board[r][c]=if(ourColor==2) 1 else 2; colored++ }
        }
        // Guard against false detections on screens without the game board.
        return Detection(board,minX,minY,bw,bh,(count/300f).coerceAtMost(1f)).takeIf { colored >= 0 }
    }
    private fun absInt(v: Int)=kotlin.math.abs(v)
}
