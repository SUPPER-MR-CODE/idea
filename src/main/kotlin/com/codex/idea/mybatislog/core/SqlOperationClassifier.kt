package com.codex.idea.mybatislog.core

object SqlOperationClassifier {
    fun classify(sql: String): SqlOperationKind {
        val firstKeyword = sql.lineSequence()
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() }
            ?.substringBefore(' ')
            ?.uppercase()
            ?: return SqlOperationKind.UNKNOWN

        return when (firstKeyword) {
            "SELECT", "WITH" -> SqlOperationKind.SELECT
            "INSERT", "REPLACE", "MERGE" -> SqlOperationKind.INSERT
            "UPDATE" -> SqlOperationKind.UPDATE
            "DELETE", "TRUNCATE" -> SqlOperationKind.DELETE
            else -> SqlOperationKind.UNKNOWN
        }
    }
}
