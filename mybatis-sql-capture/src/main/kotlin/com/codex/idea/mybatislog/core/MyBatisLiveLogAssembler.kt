package com.codex.idea.mybatislog.core

class MyBatisLiveLogAssembler(
    private val onStatementReady: (ParsedStatement) -> Unit,
) {
    private val preparingPattern = Regex("""Preparing:\s*(.*)$""")
    private val parametersPattern = Regex("""Parameters:\s*(.*)$""")
    private val genericLogLinePattern = Regex(
        """^\s*\d{4}-\d{2}-\d{2}.*\b(?:TRACE|DEBUG|INFO|WARN|ERROR)\b.*$""",
        RegexOption.IGNORE_CASE,
    )

    private val sqlBuffer = mutableListOf<String>()
    private var currentParameters: String? = null

    fun acceptLine(rawLine: String) {
        val line = sanitize(rawLine)
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

            sqlBuffer.isNotEmpty() && shouldAppendToSql(line) -> {
                sqlBuffer += line.trim()
            }

            sqlBuffer.isNotEmpty() && (line.contains("<==") || genericLogLinePattern.matches(line.trim())) -> {
                flush()
            }
        }
    }

    fun flush() {
        if (sqlBuffer.isEmpty()) {
            currentParameters = null
            return
        }

        val preparedSql = sqlBuffer.joinToString(separator = " ")
        onStatementReady(MyBatisLogParser.restoreStatement(preparedSql, currentParameters))
        sqlBuffer.clear()
        currentParameters = null
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

    private fun sanitize(line: String): String {
        return line
            .replace(Regex("""\u001B\[[;\d]*m"""), "")
            .trimEnd('\r')
    }
}
