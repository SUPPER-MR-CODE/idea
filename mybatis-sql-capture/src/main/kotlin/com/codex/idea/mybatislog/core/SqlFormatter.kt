package com.codex.idea.mybatislog.core

object SqlFormatter {
    private val breakBeforeKeywords = listOf(
        "FROM",
        "WHERE",
        "VALUES",
        "SET",
        "LEFT JOIN",
        "RIGHT JOIN",
        "INNER JOIN",
        "FULL JOIN",
        "JOIN",
        "ORDER BY",
        "GROUP BY",
        "HAVING",
        "LIMIT",
        "AND",
        "OR",
    )

    fun format(sql: String): String {
        var formatted = sql
            .replace(Regex("""\s+"""), " ")
            .trim()

        breakBeforeKeywords.forEach { keyword ->
            val regex = Regex("""(?i)\s+${keyword.split(' ').joinToString("""\s+""") { Regex.escape(it) }}\b""")
            formatted = formatted.replace(regex) { match ->
                "\n${match.value.trimStart()}"
            }
        }

        return formatted.trim()
    }
}
