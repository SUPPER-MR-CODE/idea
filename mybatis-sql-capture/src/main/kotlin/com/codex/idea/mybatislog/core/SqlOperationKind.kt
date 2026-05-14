package com.codex.idea.mybatislog.core

enum class SqlOperationKind(val displayName: String) {
    SELECT("SELECT"),
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    UNKNOWN("OTHER"),
}
