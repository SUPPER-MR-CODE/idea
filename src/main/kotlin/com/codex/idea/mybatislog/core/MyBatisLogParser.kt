package com.codex.idea.mybatislog.core

data class ParsedStatement(
    val preparedSql: String,
    val executableSql: String,
    val formattedParameters: List<String>,
    val warning: String? = null,
)

data class ParseResult(
    val statements: List<ParsedStatement>,
    val warnings: List<String> = emptyList(),
) {
    fun renderOutput(): String {
        if (statements.isEmpty()) {
            return warnings.joinToString(separator = System.lineSeparator()) { "-- $it" }
                .ifBlank { "-- No MyBatis statements found." }
        }

        return buildString {
            statements.forEachIndexed { index, statement ->
                if (statements.size > 1) {
                    appendLine("-- Statement ${index + 1}")
                }
                statement.warning?.let { appendLine("-- $it") }
                append(SqlFormatter.format(statement.executableSql).removeSuffix(";"))
                appendLine(";")
                if (index < statements.lastIndex) {
                    appendLine()
                }
            }

            if (warnings.isNotEmpty()) {
                if (isNotBlank()) {
                    appendLine()
                }
                warnings.forEach { appendLine("-- $it") }
            }
        }.trim()
    }
}

object MyBatisLogParser {
    private val preparingPattern = Regex("""Preparing:\s*(.*)$""")
    private val parametersPattern = Regex("""Parameters:\s*(.*)$""")
    private val genericLogLinePattern = Regex(
        """^\s*\d{4}-\d{2}-\d{2}.*\b(?:TRACE|DEBUG|INFO|WARN|ERROR)\b.*$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(rawLog: String): ParseResult {
        if (rawLog.isBlank()) {
            return ParseResult(emptyList(), listOf("Input is empty."))
        }

        val statements = mutableListOf<ParsedStatement>()
        val warnings = mutableListOf<String>()
        val sqlBuffer = mutableListOf<String>()
        var currentParameters: String? = null

        fun flush() {
            if (sqlBuffer.isEmpty()) {
                currentParameters = null
                return
            }

            val preparedSql = normalizeSql(sqlBuffer)
            statements += restoreStatement(preparedSql, currentParameters)
            sqlBuffer.clear()
            currentParameters = null
        }

        rawLog.lineSequence().forEach { line ->
            val preparingMatch = preparingPattern.find(line)
            val parametersMatch = parametersPattern.find(line)

            when {
                preparingMatch != null -> {
                    flush()
                    preparingMatch.groupValues[1].trim()
                        .takeIf { it.isNotEmpty() }
                        ?.let(sqlBuffer::add)
                }

                parametersMatch != null -> {
                    if (sqlBuffer.isNotEmpty()) {
                        currentParameters = parametersMatch.groupValues[1].trim()
                        flush()
                    }
                }

                sqlBuffer.isNotEmpty() && currentParameters == null && shouldAppendToSql(line) -> {
                    sqlBuffer += line.trim()
                }

                sqlBuffer.isNotEmpty() && line.contains("<==") -> flush()
            }
        }

        flush()

        if (statements.isEmpty()) {
            warnings += "No MyBatis `Preparing:` statements found."
        }

        return ParseResult(statements, warnings)
    }

    fun restoreStatement(preparedSql: String, parametersText: String?): ParsedStatement {
        val normalizedPreparedSql = normalizeSql(listOf(preparedSql))
        val formattedParameters = SqlParameterFormatter.parseParameters(parametersText.orEmpty())
        val placeholderCount = normalizedPreparedSql.count { it == '?' }
        val executableSql = substituteParameters(normalizedPreparedSql, formattedParameters)

        val warning = when {
            placeholderCount == 0 && parametersText.isNullOrBlank().not() ->
                "Found a Parameters line but no placeholders in SQL."

            placeholderCount > 0 && parametersText.isNullOrBlank() ->
                "Parameters line is missing; placeholders were left unchanged."

            formattedParameters.size < placeholderCount ->
                "Only ${formattedParameters.size} of $placeholderCount placeholders were filled."

            formattedParameters.size > placeholderCount ->
                "Found ${formattedParameters.size} parameters for $placeholderCount placeholders."

            else -> null
        }

        return ParsedStatement(
            preparedSql = normalizedPreparedSql,
            executableSql = executableSql,
            formattedParameters = formattedParameters,
            warning = warning,
        )
    }

    private fun normalizeSql(parts: List<String>): String {
        return parts.joinToString(separator = " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun shouldAppendToSql(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        if (preparingPattern.containsMatchIn(trimmed) || parametersPattern.containsMatchIn(trimmed)) {
            return false
        }
        if (trimmed.contains("<==")) {
            return false
        }
        return !genericLogLinePattern.matches(trimmed)
    }

    private fun substituteParameters(sql: String, parameters: List<String>): String {
        if (parameters.isEmpty()) {
            return sql
        }

        val builder = StringBuilder(sql.length + parameters.sumOf(String::length))
        var parameterIndex = 0

        sql.forEach { char ->
            if (char == '?' && parameterIndex < parameters.size) {
                builder.append(parameters[parameterIndex++])
            } else {
                builder.append(char)
            }
        }

        return builder.toString()
    }
}
