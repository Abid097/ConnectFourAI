package com.example.connectfourai

import kotlin.math.abs

/** Board values: 0 empty, 1 our discs, 2 opponent discs. */
object ConnectFourAI {
    private const val ROWS = 6
    private const val COLS = 7
    private const val WIN = 1_000_000

    fun chooseMove(board: Array<IntArray>, ourPiece: Int = 1, maxDepth: Int = 7): Int {
        val legal = (0 until COLS).filter { board[0][it] == 0 }
        if (legal.isEmpty()) return -1
        for (c in ordered(legal)) if (winsAfter(board, c, ourPiece)) return c
        val opp = if (ourPiece == 1) 2 else 1
        for (c in ordered(legal)) if (winsAfter(board, c, opp)) return c
        var best = legal.first(); var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE / 2
        val beta = Int.MAX_VALUE / 2
        for (c in ordered(legal)) {
            val r = drop(board, c, ourPiece) ?: continue
            val score = if (hasWin(board, ourPiece)) WIN else minimax(board, maxDepth - 1, alpha, beta, opp, ourPiece)
            board[r][c] = 0
            if (score > bestScore) { bestScore = score; best = c }
            if (score > alpha) alpha = score
        }
        return best
    }

    private fun minimax(b: Array<IntArray>, depth: Int, alpha0: Int, beta0: Int, player: Int, me: Int): Int {
        if (depth <= 0 || full(b)) return evaluate(b, me)
        var alpha = alpha0; var beta = beta0
        val maximizing = player == me
        var best = if (maximizing) Int.MIN_VALUE / 2 else Int.MAX_VALUE / 2
        for (c in ordered((0 until COLS).filter { b[0][it] == 0 })) {
            val r = drop(b, c, player) ?: continue
            val score = when {
                hasWin(b, player) -> if (player == me) WIN + depth else -WIN - depth
                else -> minimax(b, depth - 1, alpha, beta, if (player == 1) 2 else 1, me)
            }
            b[r][c] = 0
            if (maximizing) { if (score > best) best = score; if (best > alpha) alpha = best }
            else { if (score < best) best = score; if (best < beta) beta = best }
            if (alpha >= beta) break
        }
        return if (best == Int.MIN_VALUE / 2 || best == Int.MAX_VALUE / 2) evaluate(b, me) else best
    }

    private fun evaluate(b: Array<IntArray>, me: Int): Int {
        var score = 0
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            if (b[r][c] == me) score += (3 - abs(3 - c)) * 3
        }
        val dirs = arrayOf(Pair(0,1), Pair(1,0), Pair(1,1), Pair(1,-1))
        for (r in 0 until ROWS) for (c in 0 until COLS) for ((dr,dc) in dirs) {
            val er = r + 3*dr; val ec = c + 3*dc
            if (er !in 0 until ROWS || ec !in 0 until COLS) continue
            var mine = 0; var opp = 0; var empty = 0
            for (i in 0..3) when (b[r+i*dr][c+i*dc]) {
                me -> mine++
                0 -> empty++
                else -> opp++
            }
            if (opp == 0) score += when (mine) { 3 -> 120; 2 -> 12; 1 -> 2; else -> 0 }
            if (mine == 0) score -= when (opp) { 3 -> 150; 2 -> 14; 1 -> 2; else -> 0 }
        }
        return score
    }

    private fun winsAfter(b: Array<IntArray>, c: Int, p: Int): Boolean {
        val r = drop(b, c, p) ?: return false
        val result = hasWin(b, p); b[r][c] = 0; return result
    }
    private fun drop(b: Array<IntArray>, c: Int, p: Int): Int? {
        for (r in ROWS - 1 downTo 0) if (b[r][c] == 0) { b[r][c] = p; return r }
        return null
    }
    private fun hasWin(b: Array<IntArray>, p: Int): Boolean {
        val ds = arrayOf(Pair(0,1), Pair(1,0), Pair(1,1), Pair(1,-1))
        for (r in 0 until ROWS) for (c in 0 until COLS) if (b[r][c] == p) for ((dr,dc) in ds) {
            var n = 1
            for (i in 1..3) { val rr=r+i*dr; val cc=c+i*dc; if (rr in 0 until ROWS && cc in 0 until COLS && b[rr][cc]==p) n++ else break }
            if (n >= 4) return true
        }
        return false
    }
    private fun full(b: Array<IntArray>) = b[0].all { it != 0 }
    private fun ordered(cols: List<Int>) = cols.sortedBy { abs(3-it) }
}
