package com.codex.idea.mybatislog.ui

import com.intellij.ide.DataManager
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

object SqlScratchHelper {
    private val runActionIds = listOf("Console.Execute", "Console.Execute.Multiline")

    fun open(project: Project, sql: String): VirtualFile? {
        val language = Language.findLanguageByID("SQL") ?: Language.findLanguageByID("GenericSQL")
        val scratchFile = ScratchRootType.getInstance().createScratchFile(
            project,
            "mybatis-preview.sql",
            language,
            sql,
            ScratchFileService.Option.create_new_always,
        ) ?: return null

        FileEditorManager.getInstance(project).openFile(scratchFile, true)
        return scratchFile
    }

    fun runInCurrentEditor(project: Project): Boolean {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return false
        return runInEditor(editor)
    }

    private fun runInEditor(editor: Editor): Boolean {
        val dataContext = DataManager.getInstance().getDataContext(editor.component)
        val actionManager = ActionManager.getInstance()

        return runActionIds.any { actionId ->
            val action = actionManager.getAction(actionId) ?: return@any false
            val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
                action,
                null,
                ActionPlaces.UNKNOWN,
                dataContext,
            )
            if (!ActionUtil.performDumbAwareUpdate(action, event, false) || !event.presentation.isEnabled) {
                return@any false
            }
            ActionUtil.invokeAction(action, dataContext, ActionPlaces.UNKNOWN, null, null)
            true
        }
    }
}
