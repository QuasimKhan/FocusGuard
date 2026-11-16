package com.focusguard.app

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object DomainBlocklist {
    @Volatile
    private var staticDomains: Set<String>? = null

    private val blockedTlds = setOf("xxx", "porn", "adult", "sex", "cam", "cams")

    // Brand stems & common mirror misspellings (matched after stripping mirror digits)
    private val adultBrandStems = setOf(
        "xhamster", "xhamester", "pornhub", "xnxx", "xvideos", "redtube",
        "youporn", "tube8", "spankbang", "brazzers", "chaturbate", "stripchat",
        "bongacams", "livejasmin", "cam4", "onlyfans", "nhentai", "hanime",
        "eporner", "hqporner", "txxx", "upornia", "vporn", "drtuber", "tnaflix",
        "sunporno", "porntrex", "daftsex", "sxyprn", "ixxx", "pornhat", "okxxx",
        "desiporn", "indianporn", "antarvasna", "masaladesi", "fsiblog",
        "kamababa", "xossip", "desixnxx", "mmsbee", "desimms", "bangbros",
        "realitykings", "naughtyamerica", "xvideo", "xhamstar", "xhmaster"
    )

    private val adultSubstringRegex = Regex(
        "(porn|hentai|nsfw|nude|nudes|fetish|camgirl|bhabhi|xhamster|xhamester|xnxx|xvideos)"
    )

    // Bounded 'sex' regex so words like essex, sussex, unisex, codex, vertex, nextjs are safe
    private val boundedSexRegex = Regex(
        "(^|[-_0-9])sex($|[-_0-9])|^sex(cam|chat|hub|tube|vid|hd|desi|indian|tamil|hindi|telugu|malayalam|bengali|bhabhi|aunty|mms|clip|tape|toy|doll|shop|sluts|video|videos|story|stories)$"
    )

    private val digitRegex = Regex("[0-9]")

    fun ensureLoaded(context: Context): Int {
        staticDomains?.let { return it.size }
        synchronized(this) {
            staticDomains?.let { return it.size }
            val loaded = HashSet<String>(135_000)
            try {
                context.assets.open("blocked_domains.txt").use { stream ->
                    BufferedReader(InputStreamReader(stream)).useLines { lines ->
                        lines.forEach { line ->
                            val domain = line.trim().lowercase()
                            if (domain.isNotEmpty() && !domain.startsWith("#")) {
                                loaded.add(domain)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            staticDomains = loaded
            return loaded.size
        }
    }

    fun isBlocked(context: Context, queryDomain: String): Boolean {
        if (!FocusPrefs.isMasterActive(context)) return false
        val cleanDomain = queryDomain.lowercase().trimEnd('.')
        if (cleanDomain.isBlank()) return false

        val baseSet = staticDomains ?: run {
            ensureLoaded(context)
            staticDomains ?: emptySet()
        }
        val customSet = FocusPrefs.getCustomBlockedDomains(context)

        // 1. Check custom wildcard rules (e.g., *xhamester* or *.desi)
        for (rule in customSet) {
            if (rule.contains("*")) {
                val pattern = Regex("^" + Regex.escape(rule).replace("\\*", ".*") + "$")
                if (pattern.matches(cleanDomain)) return true
            }
        }

        // 2. Check exact match and every parent domain against static & custom sets
        var candidate = cleanDomain
        while (candidate.contains(".")) {
            if (baseSet.contains(candidate) || customSet.contains(candidate)) {
                return true
            }
            candidate = candidate.substringAfter(".")
        }

        // 3. Heuristic check for rotating mirror domains (e.g., xhamester46.desi)
        val labels = cleanDomain.split(".")
        if (labels.size >= 2) {
            if (blockedTlds.contains(labels.last())) return true

            // Inspect all domain labels except the TLD
            for (i in 0 until labels.size - 1) {
                val label = labels[i]
                if (label == "www" || label == "m") continue

                val alphaOnly = label.replace(digitRegex, "")
                if (adultBrandStems.contains(alphaOnly)) return true
                if (adultSubstringRegex.containsMatchIn(alphaOnly)) return true
                if (boundedSexRegex.containsMatchIn(label)) return true
            }
        }

        return false
    }
}
