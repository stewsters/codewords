package com.stewsters

import kaiju.math.Matrix2d
import kaiju.math.Vec2

// a 5 tall, 5 wide board of cards
val boardSize = Vec2(5, 5)

data class Board(
    val cards: Matrix2d<Card>
) {

    override fun toString(): String {

        val lines = cards.data.toList()
            .chunked(5)

        return lines.joinToString("\n") { line ->
            line.joinToString { card -> card.toAnsiString() }
        }

    }

    fun isComplete(): Boolean {
        return cards.data.any { it.color == CardColor.BLACK && it.selected } ||
                cards.data.none { it.color == CardColor.RED && !it.selected } ||
                cards.data.none { it.color == CardColor.BLUE && !it.selected }
    }

}