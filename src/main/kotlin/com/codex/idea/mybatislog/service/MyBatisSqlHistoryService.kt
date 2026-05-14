package com.codex.idea.mybatislog.service

import com.codex.idea.mybatislog.core.SqlFormatter
import com.codex.idea.mybatislog.core.SqlOperationClassifier
import com.codex.idea.mybatislog.core.SqlOperationKind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

@Service(Service.Level.PROJECT)
class MyBatisSqlHistoryService(private val project: Project) {
    private val listeners = CopyOnWriteArrayList<(List<MyBatisSqlHistoryEntry>) -> Unit>()
    private val entries = mutableListOf<MyBatisSqlHistoryEntry>()
    private val idGenerator = AtomicLong()
    private val lock = Any()
    private val historyLimit = 500

    fun addEntry(source: String, sql: String, warning: String?) {
        val formattedSql = SqlFormatter.format(sql).removeSuffix(";") + ";"
        val operation = SqlOperationClassifier.classify(formattedSql)
        val wasEmpty: Boolean
        synchronized(lock) {
            wasEmpty = entries.isEmpty()
            entries.add(
                0,
                MyBatisSqlHistoryEntry(
                    id = idGenerator.incrementAndGet(),
                    source = source,
                    operation = operation,
                    sql = formattedSql,
                    warning = warning,
                ),
            )
            if (entries.size > historyLimit) {
                entries.subList(historyLimit, entries.size).clear()
            }
        }

        notifyListeners()
        if (wasEmpty) {
            ApplicationManager.getApplication().invokeLater {
                ToolWindowManager.getInstance(project).getToolWindow("MyBatis SQL")?.show(null)
            }
        }
    }

    fun addEntry(source: String, operation: SqlOperationKind, sql: String, warning: String?) {
        val formattedSql = SqlFormatter.format(sql).removeSuffix(";") + ";"
        synchronized(lock) {
            entries.add(
                0,
                MyBatisSqlHistoryEntry(
                    id = idGenerator.incrementAndGet(),
                    source = source,
                    operation = operation,
                    sql = formattedSql,
                    warning = warning,
                ),
            )
            if (entries.size > historyLimit) {
                entries.subList(historyLimit, entries.size).clear()
            }
        }
        notifyListeners()
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
        }
        notifyListeners()
    }

    fun snapshot(): List<MyBatisSqlHistoryEntry> {
        return synchronized(lock) { entries.toList() }
    }

    fun addListener(listener: (List<MyBatisSqlHistoryEntry>) -> Unit) {
        listeners += listener
        listener(snapshot())
    }

    fun removeListener(listener: (List<MyBatisSqlHistoryEntry>) -> Unit) {
        listeners -= listener
    }

    private fun notifyListeners() {
        val snapshot = snapshot()
        ApplicationManager.getApplication().invokeLater {
            listeners.forEach { it(snapshot) }
        }
    }
}
