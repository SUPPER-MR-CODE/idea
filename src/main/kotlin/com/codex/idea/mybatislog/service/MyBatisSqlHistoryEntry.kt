package com.codex.idea.mybatislog.service

import com.codex.idea.mybatislog.core.SqlOperationKind
import java.time.LocalDateTime

data class MyBatisSqlHistoryEntry(
    val id: Long,
    val source: String,
    val operation: SqlOperationKind,
    val sql: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val warning: String? = null,
)
