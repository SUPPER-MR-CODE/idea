package com.codex.idea.mybatislog.ui

import com.codex.idea.mybatislog.service.MyBatisSqlColorSettingsService
import com.codex.idea.mybatislog.service.MyBatisSqlHistoryEntry
import com.codex.idea.mybatislog.service.MyBatisSqlHistoryService
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.datatransfer.StringSelection
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MyBatisSqlToolWindowPanel(
    private val project: Project,
) : SimpleToolWindowPanel(false, true), Disposable {
    private val historyService = project.getService(MyBatisSqlHistoryService::class.java)
    private var allEntries: List<MyBatisSqlHistoryEntry> = emptyList()
    private var visibleEntries: List<MyBatisSqlHistoryEntry> = emptyList()
    private val selectedEntryIds = linkedSetOf<Long>()
    private var emptyMessage = "Captured MyBatis SQL will appear here."
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val sqlListPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = UIUtil.getListBackground()
        border = JBUI.Borders.empty()
        isFocusable = true
        addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) {
                    revalidate()
                    repaint()
                }
            },
        )
    }
    private val sqlListScrollPane = ScrollPaneFactory.createScrollPane(sqlListPanel).apply {
        border = BorderFactory.createMatteBorder(
            1,
            0,
            1,
            0,
            JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(),
        )
        viewportBorder = null
        viewport.background = sqlListPanel.background
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }
    private val searchField = JBTextField().apply {
        emptyText.text = "Filter SQL / source / operation / warning"
        columns = 28
        document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = applyFilter()

                override fun removeUpdate(event: DocumentEvent) = applyFilter()

                override fun changedUpdate(event: DocumentEvent) = applyFilter()
            },
        )
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
        allEntries = entries
        applyFilter()
    }

    init {
        setContent(
            JPanel(BorderLayout()).apply {
                add(createToolbar(), BorderLayout.NORTH)
                add(sqlListScrollPane, BorderLayout.CENTER)
            },
        )
        installCopyInteractions()
        updateToolbarState()

        historyService.addListener(historyListener)

        val busConnection = project.messageBus.connect(this)
        busConnection.subscribe(MyBatisSqlColorSettingsService.TOPIC, object : MyBatisSqlColorSettingsService.Listener {
            override fun settingsChanged() {
                rebuildSqlList()
            }
        })
    }

    private fun createToolbar(): JComponent {
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            add(copyButton)
            add(clearButton)
            add(appearanceButton)
        }
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(searchField)
        }
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8)
            add(leftPanel, BorderLayout.WEST)
            add(rightPanel, BorderLayout.EAST)
        }
    }

    private fun copySelected() {
        val selected = visibleEntries.filter { selectedEntryIds.contains(it.id) }
        if (selected.isEmpty()) {
            return
        }
        val text = selected.joinToString(separator = "\n\n") { it.sql }
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    private fun installCopyInteractions() {
        val copyKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)
        val copyActionKey = "copySelectedSql"
        sqlListPanel.getInputMap(JComponent.WHEN_FOCUSED).put(copyKey, copyActionKey)
        sqlListPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(copyKey, copyActionKey)
        sqlListPanel.actionMap.put(
            copyActionKey,
            object : javax.swing.AbstractAction() {
                override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                    copySelected()
                }
            },
        )
    }

    private fun updateToolbarState() {
        copyButton.isEnabled = selectedEntryIds.isNotEmpty()
        clearButton.isEnabled = allEntries.isNotEmpty()
        syncToolbarButtonColors()
    }

    private fun applyFilter() {
        val keyword = searchField.text.trim().lowercase()
        visibleEntries = if (keyword.isEmpty()) {
            allEntries
        } else {
            allEntries.filter { entry ->
                entry.sql.lowercase().contains(keyword) ||
                    entry.source.lowercase().contains(keyword) ||
                    entry.operation.displayName.lowercase().contains(keyword) ||
                    entry.warning?.lowercase()?.contains(keyword) == true
            }
        }

        val visibleIds = visibleEntries.mapTo(mutableSetOf()) { it.id }
        selectedEntryIds.retainAll(visibleIds)
        emptyMessage = if (keyword.isEmpty()) {
            "Captured MyBatis SQL will appear here."
        } else {
            "No SQL matches the current filter."
        }
        rebuildSqlList()
        updateToolbarState()
    }

    private fun rebuildSqlList() {
        sqlListPanel.removeAll()
        if (visibleEntries.isEmpty()) {
            sqlListPanel.add(
                JBLabel(emptyMessage).apply {
                    foreground = JBColor.GRAY
                    border = JBUI.Borders.empty(12, 16)
                    alignmentX = Component.LEFT_ALIGNMENT
                },
            )
        } else {
            visibleEntries.forEachIndexed { index, entry ->
                sqlListPanel.add(createSqlEntryPanel(entry))
                if (index < visibleEntries.lastIndex) {
                    sqlListPanel.add(Box.createVerticalStrut(JBUI.scale(4)))
                }
            }
        }
        sqlListPanel.revalidate()
        sqlListPanel.repaint()
    }

    private fun createSqlEntryPanel(entry: MyBatisSqlHistoryEntry): JComponent {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        val operationColor = settings.colorFor(entry.operation)
        val sqlFontStyle = if (settings.isSqlFontBold()) Font.BOLD else Font.PLAIN
        val sqlFont = Font(Font.MONOSPACED, sqlFontStyle, settings.sqlFontSize())
        val isSelected = selectedEntryIds.contains(entry.id)
        val rowBackground = if (isSelected) UIUtil.getListSelectionBackground(true) else sqlListPanel.background

        val badge = JBLabel(entry.operation.displayName).apply {
            foreground = operationColor
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(operationColor, 1, true),
                JBUI.Borders.empty(2, 6),
            )
        }

        val meta = JBLabel("${entry.timestamp.format(timeFormatter)}  ${entry.source}").apply {
            foreground = JBColor.foreground()
        }

        val top = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(badge, BorderLayout.WEST)
            add(meta, BorderLayout.CENTER)
        }

        val root = object : JPanel(BorderLayout()) {
            override fun getMaximumSize(): Dimension {
                return Dimension(Int.MAX_VALUE, preferredSize.height)
            }
        }.apply {
            background = rowBackground
            border = JBUI.Borders.empty(10, 16, 10, 16)
            alignmentX = Component.LEFT_ALIGNMENT
            add(top, BorderLayout.NORTH)
            add(SqlTextView(entry.sql, operationColor, sqlFont), BorderLayout.CENTER)
            entry.warning?.takeIf { it.isNotBlank() }?.let { warning ->
                add(
                    JBLabel(warning).apply {
                        foreground = Color(0x996600)
                        border = JBUI.Borders.empty(8, 0, 2, 0)
                    },
                    BorderLayout.SOUTH,
                )
            }
        }

        val rowMouseListener = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (event.isPopupTrigger) {
                    showCopyPopup(event, entry)
                    return
                }
                if (SwingUtilities.isLeftMouseButton(event)) {
                    selectEntry(entry, event.isControlDown || event.isMetaDown)
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                if (event.isPopupTrigger) {
                    showCopyPopup(event, entry)
                }
            }
        }
        attachMouseListener(root, rowMouseListener)
        return root
    }

    private fun selectEntry(entry: MyBatisSqlHistoryEntry, additive: Boolean) {
        if (!additive) {
            selectedEntryIds.clear()
        }
        if (additive && selectedEntryIds.contains(entry.id)) {
            selectedEntryIds.remove(entry.id)
        } else {
            selectedEntryIds.add(entry.id)
        }
        rebuildSqlList()
        updateToolbarState()
        sqlListPanel.requestFocusInWindow()
    }

    private fun showCopyPopup(event: MouseEvent, entry: MyBatisSqlHistoryEntry) {
        val popupPoint = SwingUtilities.convertPoint(event.component, event.point, sqlListPanel)
        if (!selectedEntryIds.contains(entry.id)) {
            selectedEntryIds.clear()
            selectedEntryIds.add(entry.id)
            rebuildSqlList()
            updateToolbarState()
        }
        sqlListPanel.requestFocusInWindow()
        copyPopupMenu.show(sqlListPanel, popupPoint.x, popupPoint.y)
    }

    private fun attachMouseListener(component: Component, listener: MouseAdapter) {
        component.addMouseListener(listener)
        if (component is Container) {
            component.components.forEach { child -> attachMouseListener(child, listener) }
        }
    }

    private fun syncToolbarButtonColors() {
        copyButton.foreground = if (copyButton.isEnabled) JBColor.foreground() else JBColor.GRAY
        clearButton.foreground = if (clearButton.isEnabled) JBColor.foreground() else JBColor.GRAY
        appearanceButton.foreground = JBColor.foreground()
    }

    private fun createToolbarButton(text: String, onClick: () -> Unit): JButton {
        return JButton(text).apply {
            isFocusable = false
            margin = JBUI.insets(5, 8)
            border = JBUI.Borders.empty(4, 6)
            isBorderPainted = false
            isContentAreaFilled = false
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            val normalColor = JBColor.foreground()
            val hoverColor = JBColor(0x2F6BFF, 0x78A6FF)
            foreground = normalColor
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(event: MouseEvent) {
                        if (isEnabled) {
                            foreground = hoverColor
                        }
                    }

                    override fun mouseExited(event: MouseEvent) {
                        foreground = if (isEnabled) normalColor else JBColor.GRAY
                    }
                },
            )
            addActionListener { onClick() }
        }
    }

    override fun dispose() {
        historyService.removeListener(historyListener)
    }

    private class SqlTextView(
        private val sql: String,
        private val sqlColor: Color,
        sqlFont: Font,
    ) : JComponent() {
        private val padding = JBUI.insets(8, 0, 6, 0)

        init {
            font = sqlFont
            foreground = sqlColor
            isOpaque = false
            border = null
        }

        override fun getPreferredSize(): Dimension {
            val widthHint = listOf(parent?.width ?: 0, width, JBUI.scale(900)).first { it > 0 }
            val textWidth = (widthHint - padding.left - padding.right).coerceAtLeast(JBUI.scale(220))
            val metrics = getFontMetrics(font)
            val lines = wrapLines(metrics, textWidth)
            return Dimension(widthHint, padding.top + padding.bottom + lines.size * metrics.height)
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val graphics2d = graphics.create() as Graphics2D
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                graphics2d.font = font
                graphics2d.color = sqlColor
                val metrics = graphics2d.fontMetrics
                val textWidth = (width - padding.left - padding.right).coerceAtLeast(JBUI.scale(220))
                var y = padding.top + metrics.ascent
                wrapLines(metrics, textWidth).forEach { line ->
                    graphics2d.drawString(line, padding.left, y)
                    y += metrics.height
                }
            } finally {
                graphics2d.dispose()
            }
        }

        private fun wrapLines(metrics: FontMetrics, maxWidth: Int): List<String> {
            val wrappedLines = mutableListOf<String>()
            sql.lines().forEach { line ->
                if (line.isEmpty()) {
                    wrappedLines += ""
                    return@forEach
                }
                var remaining = line
                while (remaining.isNotEmpty() && metrics.stringWidth(remaining) > maxWidth) {
                    val breakIndex = findBreakIndex(remaining, metrics, maxWidth)
                    wrappedLines += remaining.substring(0, breakIndex).trimEnd()
                    remaining = remaining.substring(breakIndex).trimStart()
                }
                wrappedLines += remaining
            }
            return wrappedLines.ifEmpty { listOf("") }
        }

        private fun findBreakIndex(text: String, metrics: FontMetrics, maxWidth: Int): Int {
            var lastWhitespace = -1
            for (index in 1..text.length) {
                if (text[index - 1].isWhitespace()) {
                    lastWhitespace = index
                }
                if (metrics.stringWidth(text.substring(0, index)) > maxWidth) {
                    return if (lastWhitespace > 0) {
                        lastWhitespace
                    } else {
                        (index - 1).coerceAtLeast(1)
                    }
                }
            }
            return text.length
        }
    }
}
