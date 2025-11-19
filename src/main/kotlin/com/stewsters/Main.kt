package com.stewsters

import kaiju.math.matrix2dOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import java.io.File

suspend fun main() {

    println("loading common words")
    val commonWords = File("data/mostCommonWordList.txt").readLines().map { it.lowercase() }
    println("loaded ${commonWords.size} words")

    println("Initializing Word2Vec")
    val word2Vec: Word2Vec = WordVectorSerializer.readWord2VecModel("data/GoogleNews-vectors-negative300.bin.gz")
    println("Loaded Word2Vec")


    // Load in board
//    val cards = loadCardsFromFile()
    val cards = generateCards()

    println("Red: " + cards.count { card -> card.color == CardColor.RED })
    println("Blue: " + cards.count { card -> card.color == CardColor.BLUE })
    println("Black: " + cards.count { card -> card.color == CardColor.BLACK })
    println("White: " + cards.count { card -> card.color == CardColor.WHITE })

    val board = Board(matrix2dOf(boardSize.x, boardSize.y, dataList = cards))

    println(board)

    // Loop over each turn
    while (!board.isComplete()) {

        // Fancy hint logic here
        wordMatchScoreSearch(board, word2Vec, CardColor.RED, commonWords)

        val line = readLine()!!
        if (line.startsWith("quit")) {
            break
        } else if (line.startsWith("select")) {
            val action = line.split(" ")
            val word = action[1].trim()

            val cardSelected = board.cards.data.find { it.word == word }

            if (cardSelected != null) {
                cardSelected.selected = true
                println("Selecting: $cardSelected")
            } else {
                println("Could not find card $word")
            }
        }
        println(board)
    }

}


suspend fun wordMatchScoreSearch(board: Board, word2Vec: Word2Vec, yourColor: CardColor, commonWords: List<String>) {
    val (positiveWords, negativeWords) = getPositiveAndNegativeWords(board, yourColor)

    // Get a list of valid words
    // filter out words that we cant use with the current board

    val scoredWords = commonWords
        .filter { it.length >= 3 && !positiveWords.contains(it) && !negativeWords.contains(it) }
//        .pmap { word2Vec.wordsNearest(it, 100).toList() }
        .pmap { wordWeAreScoring ->
            // closest negative word
            val negativeScore = negativeWords.maxOfOrNull { word2Vec.similarity(wordWeAreScoring, it) } ?: 0.0

            //
            val score = positiveWords.sumOf {
                val simularity = word2Vec.similarity(wordWeAreScoring, it)

                if (simularity > negativeScore) simularity else 0.0
            }
            return@pmap Pair(wordWeAreScoring, score)
        }
        .sortedByDescending { it.second }

    // select the word with the highest score
    println("Scored words:" + scoredWords.take(20).toList())
}

fun getPositiveAndNegativeWords(board: Board, yourColor: CardColor): Pair<List<String>, List<String>> {
    val inPlayCards = board.cards.data
        .filter { (word, color, selected) -> !selected }

    val positiveCards = inPlayCards
        .filter { (word, color, selected) ->
            color == yourColor
        }
    val negativeCards = inPlayCards - positiveCards

    val positiveWords = positiveCards.map { it.word }
    val negativeWords = negativeCards.map { it.word }

    return Pair(positiveWords, negativeWords)
}


fun vectorSpaceSearch(board: Board, word2Vec: Word2Vec, yourColor: CardColor) {

    val (positiveWords, negativeWords) = getPositiveAndNegativeWords(board, yourColor)

    // cluster? - we dont necessarily need to have someone guess all 9 words in one go.
    // Calculate pairwise similarities between all positive words
    println("\n=== Word Similarities (${yourColor.name}) ===")
    val similarities = mutableListOf<Triple<String, String, Double>>()

    for (i in 0 until positiveWords.size) {
        for (j in i + 1 until positiveWords.size) {
            val word1 = positiveWords[i]
            val word2 = positiveWords[j]
            val similarity = word2Vec.similarity(word1, word2)
            similarities.add(Triple(word1, word2, similarity))
        }
    }

    // Sort by similarity (highest first)
    val sortedSimilarities = similarities.sortedByDescending { it.third }

    println("Most similar word pairs:")
    sortedSimilarities.take(5).forEach { (word1, word2, similarity) ->
        println("  $word1 - $word2: ${String.format("%.3f", similarity)}")
    }

    // Group words by similarity threshold
    val similarityThreshold = 0.1
    val groups = mutableListOf<MutableSet<String>>()

    sortedSimilarities
        .filter { it.third >= similarityThreshold }
        .forEach { (word1, word2, _) ->
            val existingGroup = groups.find { it.contains(word1) || it.contains(word2) }
            if (existingGroup != null) {
                existingGroup.add(word1)
                existingGroup.add(word2)
            } else {
                groups.add(mutableSetOf(word1, word2))
            }
        }

    println("\nWord groups (similarity >= $similarityThreshold):")
    groups.forEachIndexed { index, group ->
        println("  Group ${index + 1}: ${group.joinToString(", ")}")

        // Find potential clue words for this group
        val clueWords = word2Vec.wordsNearest(group.toList().take(2), negativeWords.take(2), 50).filter {
            !(it in positiveWords) && !(it in negativeWords)
                    && it.chars().allMatch { Character.isAlphabetic(it) }
        }
        println("    Suggested clues: ${clueWords.joinToString(", ")}")
    }
}

// Step 1 - find nearest words, find overlap.
// I think this is failing because there are too many words.
// The near ones are too near, and don't overlap.
private fun similarMatchingWords(board: Board, word2Vec: Word2Vec, yourColor: CardColor) {

    val positiveWords = board.cards.data
        .filter { (word, color, selected) ->
            color == yourColor && !selected
        }.map { (word, color, selected) ->
            val nearWords = word2Vec.wordsNearest(word, 100)
                .map { it.lowercase() }
                .filter { !it.contains(word) && !it.contains('_') }     // TODO cannot use words that appear in or as part of the wordset
                .distinct()
                .toList()
            Pair(word, nearWords)
        }

    positiveWords.forEach { words ->
        println("Word ${words.first} - ${words.second}")
    }


    // TODO: count which words are used the most
    val wordCounts = positiveWords.flatMap { it.second }.groupingBy { it }.eachCount()
    println(wordCounts)
}


suspend fun <A, B> Collection<A>.pmap(transform: suspend (A) -> B): List<B> =
    coroutineScope {
        map { async(Dispatchers.Default) { transform(it) } }.awaitAll()
    }