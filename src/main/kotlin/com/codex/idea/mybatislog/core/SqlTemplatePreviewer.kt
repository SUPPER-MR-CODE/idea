package com.codex.idea.mybatislog.core

data class SqlTemplatePreviewResult(
    val executableSql: String,
    val formattedParameters: List<String>,
    val warnings: List<String> = emptyList(),
) {
    fun renderOutput(): String {
        return buildString {
            warnings.forEach { appendLine("-- $it") }
            if (warnings.isNotEmpty()) {
                appendLine()
            }
            append(executableSql.removeSuffix(";"))
            append(';')
        }.trim()
    }
}

object SqlTemplatePreviewer {
    private val placeholderPattern = Regex("""#\{[^}]+}|[?]""")
    private val unsafePlaceholderPattern = Regex("""\$\{[^}]+}""")

    fun preview(sqlTemplate: String, parametersText: String): SqlTemplatePreviewResult {
        val normalizedTemplate = sqlTemplate
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(separator = " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val formattedParameters = SqlParameterFormatter.parseParameters(parametersText)
        val placeholders = placeholderPattern.findAll(normalizedTemplate).count()
        val warnings = mutableListOf<String>()

        if (unsafePlaceholderPattern.containsMatchIn(normalizedTemplate)) {
            warnings += "`${'$'}{}` placeholders were kept as-is."
        }
        if (placeholders == 0) {
            warnings += "No `?` or `#{}` placeholders were found."
        } else when {
            formattedParameters.isEmpty() -> warnings += "Parameters are missing; placeholders were left unchanged."
            formattedParameters.size < placeholders -> warnings += "Only ${formattedParameters.size} of $placeholders placeholders were filled."
            formattedParameters.size > placeholders -> warnings += "Found ${formattedParameters.size} parameters for $placeholders placeholders."
        }

        var parameterIndex = 0
        val executableSql = placeholderPattern.replace(normalizedTemplate) { match ->
            formattedParameters.getOrNull(parameterIndex++) ?: match.value
        }

        return SqlTemplatePreviewResult(
            executableSql = SqlFormatter.format(executableSql),
            formattedParameters = formattedParameters,
            warnings = warnings,
        )
    }
}
