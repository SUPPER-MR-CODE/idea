package com.codex.idea.mybatislog.ui

import com.codex.idea.mybatislog.service.MyBatisSqlColorSettingsService
import com.codex.idea.mybatislog.service.MyBatisSqlHistoryEntry
import com.codex.idea.mybatislog.service.MyBatisSqlHistoryService
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.time.format.DateTimeFormatter
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class MyBatisSqlToolWindowPanel(
    private val project: Project,
) : SimpleToolWindowPanel(true, true), Disposable {
    private val historyService = project.getService(MyBatisSqlHistoryService::class.java)
    private val listModel = DefaultListModel<MyBatisSqlHistoryEntry>()
    private val list = JBList(listModel).apply {
        cellRenderer = SqlHistoryCellRenderer()
        fixedCellHeight = -1
    }

    private val historyListener: (List<MyBatisSqlHistoryEntry>) -> Unit = { entries ->
        listModel.removeAllElements()
        entries.forEach(listModel::addElement)
    }

    init {
        setContent(ScrollPaneFactory.createScrollPane(list))
        setToolbar(createToolbar())

        historyService.addListener(historyListener)

        val busConnection = project.messageBus.connect(this)
        busConnection.subscribe(MyBatisSqlColorSettingsService.TOPIC, object : MyBatisSqlColorSettingsService.Listener {
            override fun settingsChanged() {
                list.repaint()
            }
        })
    }

    private fun createToolbar(): JComponent {
        val panel = JPanel(BorderLayout())
        val actions = DefaultActionGroup().apply {
            add(object : AnAction("Copy Selected SQL") {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) {
                    copySelected()
                }
            })
            add(object : AnAction("Clear History") {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) {
                    historyService.clear()
                }
            })
        }

        val toolbar = ActionManager.getInstance().createActionToolbar(
            "MyBatisSqlToolWindow",
            actions,
            true,
        )
        toolbar.targetComponent = this
        panel.add(toolbar.component, BorderLayout.CENTER)
        return panel
    }

    private fun copySelected() {
        val selected = list.selectedValuesList
        if (selected.isEmpty()) {
            return
        }
        val text = selected.joinToString(separator = "\n\n") { it.sql }
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    override fun dispose() {
        historyService.removeListener(historyListener)
    }

    private class SqlHistoryCellRenderer : ListCellRenderer<MyBatisSqlHistoryEntry> {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        override fun getListCellRendererComponent(
            list: javax.swing.JList<out MyBatisSqlHistoryEntry>,
            value: MyBatisSqlHistoryEntry,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val settings = MyBatisSqlColorSettingsService.getInstance()
            val operationColor = settings.colorFor(value.operation)

            val badge = JBLabel(value.operation.displayName).apply {
                foreground = operationColor
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(operationColor, 1, true),
                    JBUI.Borders.empty(2, 6),
                )
            }

            val meta = JBLabel("${value.timestamp.format(timeFormatter)}  ${value.source}").apply {
                foreground = list.foreground
            }

            val top = JPanel(BorderLayout(8, 0)).apply {
                isOpaque = false
                add(badge, BorderLayout.WEST)
                add(meta, BorderLayout.CENTER)
            }

            val sqlArea = JBTextArea(value.sql).apply {
                isEditable = false
                lineWrap = true
                wrapStyleWord = true
                font = Font(Font.MONOSPACED, Font.PLAIN, list.font.size)
                foreground = operationColor
                background = if (isSelected) list.selectionBackground else list.background
                border = JBUI.Borders.emptyTop(6)
            }

            val root = JPanel(BorderLayout()).apply {
                background = if (isSelected) list.selectionBackground else list.background
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                    JBUI.Borders.empty(8),
                )
                add(top, BorderLayout.NORTH)
                add(sqlArea, BorderLayout.CENTER)
                value.warning?.takeIf { it.isNotBlank() }?.let { warning ->
                    add(
                        JBLabel(warning).apply {
                            foreground = Color(0x996600)
                            border = JBUI.Borders.emptyTop(6)
                        },
                        BorderLayout.SOUTH,
                    )
                }
            }
            return root
        }
    }
}
