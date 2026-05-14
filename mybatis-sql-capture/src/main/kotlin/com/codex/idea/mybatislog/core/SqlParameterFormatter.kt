package com.codex.idea.mybatislog.core

object SqlParameterFormatter {
    private val parameterTokenPattern = Regex("""\s*(null|.*?\([^()]+\))(?:,\s*|$)""", RegexOption.IGNORE_CASE)
    private val numericPattern = Regex("""[-+]?\d+(?:\.\d+)?""")

    fun parseParameters(parametersText: String): List<String> {
        val normalized = parametersText
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(separator = " ")
            .removePrefix("Parameters:")
            .trim()

        if (normalized.isBlank()) {
            return emptyList()
        }

        val tokens = mutableListOf<String>()
        var index = 0

        while (index < normalized.length) {
            while (index < normalized.length && (normalized[index].isWhitespace() || normalized[index] == ',')) {
                index++
            }

            if (index >= normalized.length) {
                break
            }

            val match = parameterTokenPattern.find(normalized, index)
            if (match == null || match.range.first != index) {
                tokens += normalized.substring(index).split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                break
            }

            tokens += match.groupValues[1].trim()
            index = match.range.last + 1
        }

        return tokens.map(::formatParameter)
    }

    private fun formatParameter(token: String): String {
        if (token.equals("null", ignoreCase = true)) {
            return "null"
        }

        val typeStart = token.lastIndexOf('(')
        val typeEnd = token.lastIndexOf(')')
        if (typeStart < 0 || typeEnd <= typeStart) {
            return quoteFallback(token)
        }

        val rawValue = token.substring(0, typeStart).trim()
        val rawType = token.substring(typeStart + 1, typeEnd).trim().lowercase()

        if (rawValue.equals("null", ignoreCase = true)) {
            return "null"
        }

        return when {
            isNumericType(rawType) || numericPattern.matches(rawValue) -> rawValue
            isBooleanType(rawType) -> rawValue.lowercase()
            else -> "'${escapeSql(rawValue)}'"
        }
    }

    private fun quoteFallback(value: String): String {
        return if (numericPattern.matches(value)) {
            value
        } else {
            "'${escapeSql(value)}'"
        }
    }

    private fun isNumericType(type: String): Boolean {
        return type.contains("int") ||
            type.contains("long") ||
            type.contains("short") ||
            type.contains("double") ||
            type.contains("float") ||
            type.contains("decimal") ||
            type.contains("number") ||
            type.contains("bigdecimal") ||
            type.contains("biginteger")
    }

    private fun isBooleanType(type: String): Boolean {
        return type.contains("boolean")
    }

    private fun escapeSql(value: String): String {
        return value.replace("'", "''")
    }
}
