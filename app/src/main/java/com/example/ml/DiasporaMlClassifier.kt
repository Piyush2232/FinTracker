package com.example.ml

import com.example.data.MlTokenWeightEntity
import com.example.domain.ExpenseCategory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.ln

data class MlPrediction(
    val category: ExpenseCategory,
    val confidence: Float,
    val topTokens: List<String>,
    val runnerUp: ExpenseCategory?,
)

/**
 * 100% offline Multinomial Naive Bayes + subword bigram/trigram similarity classifier
 * tailored for Indian & diaspora merchants and UPI VPAs, with online user-correction learning.
 */
class DiasporaMlClassifier {

    private val learnedWeights = ConcurrentHashMap<ExpenseCategory, ConcurrentHashMap<String, Double>>()

    init {
        ExpenseCategory.entries.forEach { cat ->
            learnedWeights[cat] = ConcurrentHashMap()
        }
    }

    fun hydrateLearnedWeights(entities: List<MlTokenWeightEntity>) {
        ExpenseCategory.entries.forEach { learnedWeights[it]?.clear() }
        entities.forEach { row ->
            val category = ExpenseCategory.fromName(row.categoryName)
            learnedWeights[category]?.put(row.token, row.weight)
        }
    }

    fun tokenize(merchant: String, rawContext: String = ""): List<String> {
        val combined = "$merchant $rawContext".lowercase()
        // Strip UPI bank handle suffixes like @okaxis, @ybl, @okicici while preserving the VPA name
        val withoutVpaSuffix = combined.replace(
            Regex("@(okaxis|okicici|okhdfcbank|oksbi|ybl|ibl|axl|paytm|apl|upi|icici|hdfcbank|sbi|axisbank)"),
            " ",
        )
        val rawTokens = withoutVpaSuffix
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && !STOP_WORDS.contains(it) }

        val expanded = LinkedHashSet<String>()
        rawTokens.forEach { token ->
            expanded.add(token)
            // Keep merged camel/pair form for compound Indian names e.g. "patel brothers" -> "patelbrothers"
        }
        for (i in 0 until rawTokens.size - 1) {
            expanded.add("${rawTokens[i]}${rawTokens[i + 1]}")
        }
        return expanded.toList()
    }

    fun classify(merchant: String, rawContext: String = ""): MlPrediction {
        val tokens = tokenize(merchant, rawContext)
        if (tokens.isEmpty()) {
            return MlPrediction(
                category = ExpenseCategory.SHOPPING_LIFESTYLE,
                confidence = 0.28f,
                topTokens = emptyList(),
                runnerUp = null,
            )
        }

        val logScores = LinkedHashMap<ExpenseCategory, Double>()
        val matchedTokensByCategory = LinkedHashMap<ExpenseCategory, List<Pair<String, Double>>>()

        val vocabSize = 420.0
        val smoothingAlpha = 0.35

        for (category in ExpenseCategory.entries) {
            val priorMap = DiasporaVocabulary.priorWeights[category].orEmpty()
            val userMap = learnedWeights[category].orEmpty()
            val totalMass = priorMap.values.sum() + userMap.values.sum() + smoothingAlpha * vocabSize

            var logProb = ln(category.defaultBudgetRatio.coerceAtLeast(0.05))
            val hits = mutableListOf<Pair<String, Double>>()

            for (token in tokens) {
                val exactPrior = priorMap[token] ?: 0.0
                val userBoost = userMap[token] ?: 0.0
                var featureWeight = exactPrior + userBoost * 1.45

                // Fuzzy prefix/substring match for noisy bank truncation (e.g. "SWIGGYIN" or "ZOMATOLTD")
                if (featureWeight == 0.0 && token.length >= 4) {
                    val fuzzy = priorMap.entries
                        .filter { (key, _) ->
                            key.length >= 4 && (token.contains(key) || key.contains(token))
                        }
                        .maxOfOrNull { it.value * 0.72 } ?: 0.0
                    featureWeight = fuzzy
                }

                // Character 3-gram similarity fallback for unseen merchant variants
                if (featureWeight == 0.0 && token.length >= 5) {
                    val ngramBoost = bestTrigramMatch(token, priorMap)
                    if (ngramBoost > 0.0) {
                        featureWeight = ngramBoost
                    }
                }

                if (featureWeight > 0.0) {
                    hits.add(token to featureWeight)
                }

                val tokenLikelihood = (featureWeight + smoothingAlpha) / totalMass
                logProb += ln(tokenLikelihood)
            }

            // Extra heuristic: P2P UPI transfer cues (person names / personal VPA without merchant words)
            if (category == ExpenseCategory.REMITTANCE_TRANSFERS && hits.isEmpty() &&
                (rawContext.contains("trf to", ignoreCase = true) ||
                    rawContext.contains("sent to", ignoreCase = true) ||
                    rawContext.contains("vpa", ignoreCase = true))
            ) {
                logProb += 0.65
            }

            logScores[category] = logProb
            matchedTokensByCategory[category] = hits.sortedByDescending { it.second }
        }

        val sorted = logScores.entries.sortedByDescending { it.value }
        val maxLog = sorted.first().value
        val expValues = sorted.associate { it.key to exp((it.value - maxLog).coerceAtLeast(-20.0)) }
        val sumExp = expValues.values.sum().coerceAtLeast(1e-9)

        val winner = sorted.first().key
        val winnerProb = (expValues[winner]!! / sumExp).toFloat().coerceIn(0.15f, 0.99f)
        val runnerUp = sorted.getOrNull(1)?.key
        val topTokens = matchedTokensByCategory[winner]
            .orEmpty()
            .map { it.first }
            .distinct()
            .take(4)

        return MlPrediction(
            category = winner,
            confidence = winnerProb,
            topTokens = topTokens,
            runnerUp = runnerUp,
        )
    }

    /**
     * Updates in-memory and returns Room entities to persist online learning
     * whenever the user corrects a transaction's category.
     */
    fun buildOnlineLearningUpdates(
        merchant: String,
        rawContext: String,
        correctedCategory: ExpenseCategory,
    ): List<MlTokenWeightEntity> {
        val tokens = tokenize(merchant, rawContext)
            .filter { it.length >= 3 }
            .take(8)
        val targetMap = learnedWeights.getOrPut(correctedCategory) { ConcurrentHashMap() }
        val updates = mutableListOf<MlTokenWeightEntity>()

        tokens.forEachIndexed { idx, token ->
            val increment = if (idx < 2) 5.5 else 3.0
            val nextWeight = ((targetMap[token] ?: 0.0) + increment).coerceAtMost(40.0)
            targetMap[token] = nextWeight
            updates.add(
                MlTokenWeightEntity(
                    token = token,
                    categoryName = correctedCategory.name,
                    weight = nextWeight,
                    hitCount = 1,
                ),
            )
        }
        return updates
    }

    private fun bestTrigramMatch(token: String, priorMap: Map<String, Double>): Double {
        val tokenTrigrams = trigrams(token)
        if (tokenTrigrams.isEmpty()) return 0.0
        var best = 0.0
        for ((key, weight) in priorMap) {
            if (key.length < 5) continue
            val keyTrigrams = trigrams(key)
            val intersection = tokenTrigrams.intersect(keyTrigrams).size
            val union = tokenTrigrams.union(keyTrigrams).size
            if (union > 0) {
                val jaccard = intersection.toDouble() / union.toDouble()
                if (jaccard >= 0.45) {
                    val candidate = weight * jaccard * 0.75
                    if (candidate > best) best = candidate
                }
            }
        }
        return best
    }

    private fun trigrams(word: String): Set<String> {
        if (word.length < 3) return emptySet()
        val set = HashSet<String>()
        for (i in 0..word.length - 3) {
            set.add(word.substring(i, i + 3))
        }
        return set
    }

    companion object {
        private val STOP_WORDS = setOf(
            "rs", "inr", "amt", "amount", "sent", "paid", "debited", "spent",
            "from", "to", "on", "at", "via", "upi", "ref", "refno", "utr",
            "txn", "bank", "hdfc", "icici", "sbi", "axis", "kotak", "idfc",
            "acct", "account", "card", "your", "user", "dear", "info", "bal",
            "avl", "avbl", "balance", "available", "date", "time", "not", "you",
            "call", "if", "block", "sms", "for", "with", "using", "the", "and",
            "pvt", "ltd", "private", "limited", "india", "pay", "payments",
        )
    }
}
