package com.anotherlife.app.engine

/** A small explainable rule set. Ambiguous dialogue remains NEUTRAL rather than being scored by guesswork. */
object ConversationEmotionAnalyzer {
    data class Signal(val category: String, val evidence: String, val trust: Int, val closeness: Int, val tension: Int)

    fun analyze(text: String): Signal {
        val normalized = text.lowercase().replace(Regex("\s+"), " ").take(1200)
        val rules = listOf(
            Triple("RECONCILIATION", listOf("화해하자", "용서해 줘", "용서해줘", "미안했어", "사과할게"), intArrayOf(1, 1, -2)),
            Triple("CONFLICT", listOf("다신 연락하지 마", "거짓말쟁이", "널 믿을 수 없어", "너 때문에 화가 나", "그만 괴롭혀"), intArrayOf(-2, -1, 3)),
            Triple("SUPPORT", listOf("네 편이야", "도와줄게", "힘들면 말해", "함께할게", "걱정하지 마"), intArrayOf(1, 1, -1)),
            Triple("GRATITUDE", listOf("고마워", "감사해", "덕분이야"), intArrayOf(1, 1, 0))
        )
        // Longer and more consequential signals take precedence; do not infer sentiment from topic alone.
        for ((category, phrases, delta) in rules) {
            val match = phrases.firstOrNull { normalized.contains(it) } ?: continue
            return Signal(category, match, delta[0], delta[1], delta[2])
        }
        return Signal("NEUTRAL", "", 0, 0, 0)
    }
}
