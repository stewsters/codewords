package com.stewsters

import java.io.File
import kotlin.text.lowercase

fun loadCardsFromFile(): List<Card> = File("data/board.txt").readLines().flatMap { row ->
    row.split(',')
}.map {
    val cardDesc = it.lowercase().trim()
    val word = cardDesc.substring(1)

    val color = when (cardDesc.first()) {
        'r' -> CardColor.RED
        'b' -> CardColor.BLUE
        'x' -> CardColor.BLACK
        'w' -> CardColor.WHITE
        else -> {
            throw Exception("what does ${it} mean?")
        }
    }
    Card(word, color)
}

fun generateCards(): List<Card> {
    val wordList = File("data/wordsUsedInCodenames.txt").readLines().map { it.lowercase().trim() }.shuffled()
    val list = mutableListOf<Card>()
    repeat(9) { i ->
        list.add(Card(wordList[i], CardColor.RED))
    }
    repeat(8) { i ->
        list.add(Card(wordList[i + 9], CardColor.BLUE))
    }
    repeat(7) { i ->
        list.add(Card(wordList[i + 9 + 8], CardColor.WHITE))
    }
    list.add(Card(wordList[9 + 8 + 7], CardColor.BLACK))

    return list.shuffled()
}