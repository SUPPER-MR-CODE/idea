package com.codex.idea.mybatislog.ui

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.datatransfer.StringSelection
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

class SqlTemplatePreviewDialog(
    private val project: Project,
    private val dialogTitle: String,
    initialTemplate: String,
) : DialogWrapper(project, true) {
    private val panel = SqlTemplatePreviewPanel(initialTemplate)

    init {
        title = dialogTitle
        setResizable(true)
        init()
        panel.preview()
    }

    override fun createCenterPanel(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = panel.preferredFocusComponent()

    override fun createActions(): Array<Action> {
        return arrayOf(
            object : AbstractAction("Preview") {
                override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                    panel.preview()
                }
            },
            object : AbstractAction("Copy") {
                override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                    if (panel.outputText().isBlank()) {
                        panel.setStatus("Nothing to copy.")
                        return
                    }
                    CopyPasteManager.getInstance().setContents(StringSelection(panel.outputText()))
                    panel.setStatus("SQL copied to clipboard.")
                }
            },
            object : AbstractAction("Open in SQL Scratch") {
                override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                    val result = panel.preview()
                    val file = SqlScratchHelper.open(project, result.executableSql)
                    panel.setStatus(
                        if (file == null) "Unable to open SQL scratch."
                        else "Opened SQL scratch: ${file.name}",
                    )
                }
            },
            object : AbstractAction("Run") {
                override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                    val result = panel.preview()
                    val file = SqlScratchHelper.open(project, result.executableSql)
                    if (file == null) {
                        panel.setStatus("Unable to open SQL scratch.")
                        return
                    }
                    val started = SqlScratchHelper.runInCurrentEditor(project)
                    panel.setStatus(
                        if (started) "Execution action was triggered."
                        else "Opened SQL scratch, but no runnable SQL console was available.",
                    )
                }
            },
            cancelAction.apply {
                putValue(Action.NAME, "Close")
            },
        )
    }
}
