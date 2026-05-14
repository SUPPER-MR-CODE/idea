package com.codex.idea.mybatislog.ui

import com.codex.idea.mybatislog.core.SqlTemplatePreviewResult
import com.codex.idea.mybatislog.core.SqlTemplatePreviewer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane

class SqlTemplatePreviewPanel(initialTemplate: String) : JPanel(BorderLayout()) {
    private val templateArea = JBTextArea(initialTemplate)
    private val parametersArea = JBTextArea()
    private val outputArea = JBTextArea()
    private val statusLabel = JBLabel("Fill parameters and click Preview.")

    init {
        templateArea.lineWrap = true
        templateArea.wrapStyleWord = true

        parametersArea.lineWrap = true
        parametersArea.wrapStyleWord = true

        outputArea.isEditable = false
        outputArea.lineWrap = true
        outputArea.wrapStyleWord = true

        val topSplit = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            labeledPanel("SQL Template", JBScrollPane(templateArea)),
            labeledPanel("Parameters", JBScrollPane(parametersArea)),
        ).apply {
            resizeWeight = 0.7
        }

        val rootSplit = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            topSplit,
            labeledPanel("Executable SQL", JBScrollPane(outputArea)),
        ).apply {
            resizeWeight = 0.55
        }

        add(rootSplit, BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)
    }

    fun preview(): SqlTemplatePreviewResult {
        val result = SqlTemplatePreviewer.preview(templateArea.text, parametersArea.text)
        outputArea.text = result.renderOutput()
        statusLabel.text = when {
            result.warnings.isEmpty() -> "Preview generated."
            else -> result.warnings.first()
        }
        return result
    }

    fun preferredFocusComponent(): JComponent = parametersArea

    fun outputText(): String = outputArea.text

    fun setStatus(message: String) {
        statusLabel.text = message
    }

    private fun labeledPanel(title: String, content: JBScrollPane): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JBLabel(title), BorderLayout.NORTH)
            add(content, BorderLayout.CENTER)
        }
    }
}
