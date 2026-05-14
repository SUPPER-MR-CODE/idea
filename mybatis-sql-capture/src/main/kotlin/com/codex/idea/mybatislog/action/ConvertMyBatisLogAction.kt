package com.codex.idea.mybatislog.action

import com.codex.idea.mybatislog.ui.MyBatisLogDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

class ConvertMyBatisLogAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedText = event.getData(CommonDataKeys.EDITOR)
            ?.selectionModel
            ?.selectedText
            ?.takeIf { it.isNotBlank() }

        MyBatisLogDialog(project, selectedText).show()
    }
}
