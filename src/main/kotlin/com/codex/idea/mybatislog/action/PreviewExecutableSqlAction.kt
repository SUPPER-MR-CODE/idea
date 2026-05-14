package com.codex.idea.mybatislog.action

import com.codex.idea.mybatislog.core.MapperSqlExtractor
import com.codex.idea.mybatislog.ui.SqlTemplatePreviewDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

class PreviewExecutableSqlAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val project = event.project
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.PSI_FILE)
        val extracted = if (project != null && editor != null && file != null) {
            MapperSqlExtractor.extract(file, editor)
        } else {
            null
        }
        event.presentation.isEnabledAndVisible = extracted != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val extracted = MapperSqlExtractor.extract(file, editor) ?: return

        SqlTemplatePreviewDialog(project, extracted.title, extracted.sqlTemplate).show()
    }
}
