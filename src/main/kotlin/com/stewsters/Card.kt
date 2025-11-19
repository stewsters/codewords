package com.stewsters


// each slot has
//   a word (known to all)
//   a color (known to spymaster) [red, blue, black, white]
//   if it has been selected and revealed

data class Card(
    val word: String,
    val color: CardColor,
    var selected: Boolean = false,
) {

    fun toAnsiString(): String {
        val color = when (color) {
            CardColor.RED -> ANSI_RED
            CardColor.BLUE -> ANSI_BLUE
            CardColor.BLACK -> ANSI_BLACK
            CardColor.WHITE -> ANSI_WHITE
        }
        return if (selected) {
            ANSI_STRIKETHROUGH
        } else {
            ""
        } + color + word + ANSI_RESET
    }

}


const val ANSI_RESET = "\u001B[0m"
const val ANSI_STRIKETHROUGH = "\u001B[9m"

const val ANSI_BLACK = "\u001B[30m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_BLUE = "\u001B[34m"
const val ANSI_PURPLE = "\u001B[35m"
const val ANSI_CYAN = "\u001B[36m"
const val ANSI_WHITE = "\u001B[37m"