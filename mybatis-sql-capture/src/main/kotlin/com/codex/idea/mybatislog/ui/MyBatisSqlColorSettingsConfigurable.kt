package com.codex.idea.mybatislog.ui

import com.codex.idea.mybatislog.service.MyBatisSqlColorSettingsService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class MyBatisSqlColorSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {
    private var panel: JPanel? = null
    private var selectColorPanel: ColorPanel? = null
    private var insertColorPanel: ColorPanel? = null
    private var updateColorPanel: ColorPanel? = null
    private var deleteColorPanel: ColorPanel? = null
    private var fontSizeSpinner: JSpinner? = null

    override fun getId(): String = "com.codex.idea.mybatislog.settings"

    override fun getDisplayName(): String = "MyBatis SQL"

    override fun createComponent(): JComponent {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        selectColorPanel = ColorPanel()
        insertColorPanel = ColorPanel()
        updateColorPanel = ColorPanel()
        deleteColorPanel = ColorPanel()
        fontSizeSpinner = JSpinner(SpinnerNumberModel(settings.sqlFontSize(), 10, 28, 1))

        panel = FormBuilder.createFormBuilder()
            .addComponent(
                JBLabel("Customize SQL operation colors and the SQL font size used in the MyBatis SQL panel."),
            )
            .addLabeledComponent("SELECT Color", selectColorPanel!!)
            .addLabeledComponent("INSERT Color", insertColorPanel!!)
            .addLabeledComponent("UPDATE Color", updateColorPanel!!)
            .addLabeledComponent("DELETE Color", deleteColorPanel!!)
            .addLabeledComponent("SQL Font Size", fontSizeSpinner!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        return selectColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.SELECT) ||
            insertColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.INSERT) ||
            updateColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.UPDATE) ||
            deleteColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.DELETE) ||
            (fontSizeSpinner?.value as? Int) != settings.sqlFontSize()
    }

    override fun apply() {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        settings.updateAppearance(
            select = selectColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.SELECT),
            insert = insertColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.INSERT),
            update = updateColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.UPDATE),
            delete = deleteColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.DELETE),
            sqlFontSize = (fontSizeSpinner!!.value as Number).toInt(),
        )
    }

    override fun reset() {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        selectColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.SELECT)
        insertColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.INSERT)
        updateColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.UPDATE)
        deleteColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.DELETE)
        fontSizeSpinner?.value = settings.sqlFontSize()
    }

    override fun disposeUIResources() {
        panel = null
        selectColorPanel = null
        insertColorPanel = null
        updateColorPanel = null
        deleteColorPanel = null
        fontSizeSpinner = null
    }
}
