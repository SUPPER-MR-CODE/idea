package com.codex.idea.mybatislog.ui

import com.codex.idea.mybatislog.service.MyBatisSqlColorSettingsService
import com.codex.idea.mybatislog.service.MyBatisSqlHistoryEntry
import com.codex.idea.mybatislog.service.MyBatisSqlHistoryService
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.ShowSettingsUtil
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
import java.awt.Dimension
import java.awt.Font
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.KeyStroke
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel

class MyBatisSqlToolWindowPanel(
    private val project: Project,
) : SimpleToolWindowPanel(false, true), Disposable {
    private val historyService = project.getService(MyBatisSqlHistoryService::class.java)
    private val listModel = DefaultListModel<MyBatisSqlHistoryEntry>()
    private val list = JBList(listModel).apply {
        cellRenderer = SqlHistoryCellRenderer()
        fixedCellHeight = -1
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        emptyText.text = "Captured MyBatis SQL will appear here."
    }
    private val copyButton = createToolbarButton("Copy SQL") { copySelected() }
    private val clearButton = createToolbarButton("Clear History") { historyService.clear() }
    private val appearanceButton = createToolbarButton("Colors & Font") {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, MyBatisSqlColorSettingsConfigurable::class.java)
    }
    private val copyPopupMenu = JPopupMenu().apply {
        add(
            JMenuItem("Copy SQL").apply {
                addActionListener { copySelected() }
            },
        )
    }

    private val historyListener: (List<MyBatisSqlHistoryEntry>) -> Unit = { entries ->
        listModel.removeAllElements()
        entries.forEach(listModel::addElement)
        updateToolbarState()
    }

    init {
        setContent(
            JPanel(BorderLayout()).apply {
                add(createToolbar(), BorderLayout.NORTH)
                add(ScrollPaneFactory.createScrollPane(list), BorderLayout.CENTER)
            },
        )
        updateToolbarState()

        historyService.addListener(historyListener)
        list.addListSelectionListener { updateToolbarState() }
        installCopyInteractions()

        val busConnection = project.messageBus.connect(this)
        busConnection.subscribe(MyBatisSqlColorSettingsService.TOPIC, object : MyBatisSqlColorSettingsService.Listener {
            override fun settingsChanged() {
                list.cellRenderer = SqlHistoryCellRenderer()
                list.revalidate()
                list.repaint()
            }
        })
    }

    private fun createToolbar(): JComponent {
        return JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            border = JBUI.Borders.empty(4, 8)
            add(copyButton)
            add(clearButton)
            add(appearanceButton)
        }
    }

    private fun copySelected() {
        val selected = list.selectedValuesList
        if (selected.isEmpty()) {
            return
        }
        val text = selected.joinToString(separator = "\n\n") { it.sql }
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    private fun installCopyInteractions() {
        list.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copySelectedSql")
        list.actionMap.put(
            "copySelectedSql",
            object : javax.swing.AbstractAction() {
                override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                    copySelected()
                }
            },
        )
        list.addMouseListener(
            object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent) = maybeShowPopup(event)

                override fun mouseReleased(event: MouseEvent) = maybeShowPopup(event)

                private fun maybeShowPopup(event: MouseEvent) {
                    if (!event.isPopupTrigger) {
                        return
                    }
                    val index = list.locationToIndex(event.point)
                    if (index < 0) {
                        return
                    }
                    val bounds = list.getCellBounds(index, index) ?: return
                    if (!bounds.contains(event.point)) {
                        return
                    }
                    if (!list.isSelectedIndex(index)) {
                        list.setSelectedIndex(index)
                    }
                    updateToolbarState()
                    copyPopupMenu.show(event.component, event.x, event.y)
                }
            },
        )
    }

    private fun updateToolbarState() {
        copyButton.isEnabled = list.selectedIndices.isNotEmpty()
        clearButton.isEnabled = !listModel.isEmpty
    }

    private fun createToolbarButton(text: String, onClick: () -> Unit): JButton {
        return JButton(text).apply {
            isFocusable = false
            margin = JBUI.insets(5, 8)
            border = JBUI.Borders.empty(4, 6)
            isBorderPainted = false
            isContentAreaFilled = false
            isOpaque = false
            addActionListener { onClick() }
        }
    }

    override fun dispose() {
        historyService.removeListener(historyListener)
    }

    private class SqlHistoryCellRenderer : ListCellRenderer<MyBatisSqlHistoryEntry> {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        override fun getListCellRendererComponent(
            list: JList<out MyBatisSqlHistoryEntry>,
            value: MyBatisSqlHistoryEntry,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val settings = MyBatisSqlColorSettingsService.getInstance()
            val operationColor = settings.colorFor(value.operation)
            val sqlFontSize = settings.sqlFontSize()
            val sqlFontStyle = if (settings.isSqlFontBold()) Font.BOLD else Font.PLAIN

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
                font = Font(Font.MONOSPACED, sqlFontStyle, sqlFontSize)
                foreground = operationColor
                background = if (isSelected) list.selectionBackground else list.background
                border = JBUI.Borders.empty(8, 0, 6, 0)
            }
            val sqlAreaWidth = (list.width - JBUI.scale(56)).coerceAtLeast(JBUI.scale(220))
            sqlArea.setSize(sqlAreaWidth, Short.MAX_VALUE.toInt())
            sqlArea.preferredSize = Dimension(sqlAreaWidth, sqlArea.preferredSize.height)

            val root = JPanel(BorderLayout()).apply {
                background = if (isSelected) list.selectionBackground else list.background
                border = JBUI.Borders.empty(10, 16, 10, 16)
                add(top, BorderLayout.NORTH)
                add(sqlArea, BorderLayout.CENTER)
                value.warning?.takeIf { it.isNotBlank() }?.let { warning ->
                    add(
                        JBLabel(warning).apply {
                            foreground = Color(0x996600)
                            border = JBUI.Borders.empty(8, 0, 2, 0)
                        },
                        BorderLayout.SOUTH,
                    )
                }
            }
            return root
        }
    }
}
