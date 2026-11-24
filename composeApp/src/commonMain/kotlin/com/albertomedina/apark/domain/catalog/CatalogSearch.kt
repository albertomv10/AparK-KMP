package com.albertomedina.apark.domain.catalog

/**
 * El texto con el que se compara al buscar: sin mayúsculas, sin tildes y con los guiones como
 * espacios, para que «skoda» encuentre «Škoda» y «mercedes benz», «Mercedes-Benz».
 *
 * `commonMain` no tiene `java.text.Normalizer`, que es lo que haría esto en la JVM, así que va con una
 * tabla propia de las letras latinas. Basta para nombres de marcas y modelos; no pretende ser Unicode
 * completo.
 */
fun foldForSearch(text: String): String {
    val lower = text.trim().lowercase()
    val out = StringBuilder(lower.length)
    for (c in lower) {
        when (c) {
            '-', '_' -> out.append(' ')
            else -> out.append(FOLDS[c] ?: c.toString())
        }
    }
    return out.toString().replace(SPACES, " ")
}

/**
 * Las [names] que coinciden con [query], como mucho [limit]: primero las que empiezan por lo escrito,
 * luego las que tienen una palabra que empieza por ello, y al final las que lo contienen en medio.
 * Dentro de cada grupo se respeta el orden de [names]. Sin nada escrito, no se sugiere nada.
 */
fun searchNames(query: String, names: List<String>, limit: Int = 6): List<String> {
    val q = foldForSearch(query)
    if (q.isEmpty()) return emptyList()
    val ranked = names.mapNotNull { name ->
        val folded = foldForSearch(name)
        val rank = when {
            folded.startsWith(q) -> 0
            folded.split(' ').any { it.startsWith(q) } -> 1
            folded.contains(q) -> 2
            else -> return@mapNotNull null
        }
        rank to name
    }
    return ranked.sortedBy { it.first }.map { it.second }.take(limit)
}

private val SPACES = Regex("\\s+")

private val FOLDS: Map<Char, String> = buildMap {
    fun put(chars: String, plain: String) = chars.forEach { put(it, plain) }
    put("àáâãäåāăą", "a")
    put("çćĉċč", "c")
    put("ďđ", "d")
    put("èéêëēĕėęě", "e")
    put("ĝğġģ", "g")
    put("ĥħ", "h")
    put("ìíîïĩīĭįı", "i")
    put("ĵ", "j")
    put("ķ", "k")
    put("ĺļľŀł", "l")
    put("ñńņňŉ", "n")
    put("òóôõöøōŏő", "o")
    put("ŕŗř", "r")
    put("śŝşšș", "s")
    put("ţťŧț", "t")
    put("ùúûüũūŭůűų", "u")
    put("ŵ", "w")
    put("ýÿŷ", "y")
    put("źżž", "z")
    put("ß", "ss")
    put("æ", "ae")
    put("œ", "oe")
}
