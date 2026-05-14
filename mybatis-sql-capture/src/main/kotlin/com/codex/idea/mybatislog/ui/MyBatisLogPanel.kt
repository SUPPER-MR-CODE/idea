package com.codex.idea.mybatislog.ui

import com.codex.idea.mybatislog.core.MyBatisLogParser
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane

class MyBatisLogPanel : JPanel(BorderLayout()) {
    private val inputArea = JBTextArea()
    private val outputArea = JBTextArea()
    private val statusLabel = JBLabel("Paste MyBatis logs and click Convert.")

    init {
        inputArea.lineWrap = true
        inputArea.wrapStyleWord = true

        outputArea.isEditable = false
        outputArea.lineWrap = true
        outputArea.wrapStyleWord = true

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 8)).apply {
            add(JButton("Convert").apply {
                addActionListener { convert() }
            })
            add(JButton("Clear").apply {
                addActionListener { clearAll() }
            })
        }

        val splitPane = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            labeledPanel("MyBatis Log", JBScrollPane(inputArea)),
            labeledPanel("Executable SQL", JBScrollPane(outputArea)),
        ).apply {
            resizeWeight = 0.5
        }

        add(buttonPanel, BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)
    }

    fun setInput(text: String) {
        inputArea.text = text
    }

    fun convert() {
        val result = MyBatisLogParser.parse(inputArea.text)
        outputArea.text = result.renderOutput()
        statusLabel.text = when {
            result.statements.isEmpty() -> result.warnings.firstOrNull() ?: "No statements found."
            result.statements.size == 1 -> "Converted 1 statement."
            else -> "Converted ${result.statements.size} statements."
        }
    }

    fun focusInput() {
        inputArea.requestFocusInWindow()
    }

    fun preferredFocusComponent(): JComponent = inputArea

    fun outputText(): String = outputArea.text

    fun setStatus(message: String) {
        statusLabel.text = message
    }

    private fun clearAll() {
        inputArea.text = ""
        outputArea.text = ""
        statusLabel.text = "Cleared."
        focusInput()
    }

    private fun labeledPanel(title: String, content: JBScrollPane): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JBLabel(title), BorderLayout.NORTH)
            add(content, BorderLayout.CENTER)
        }
    }
}
