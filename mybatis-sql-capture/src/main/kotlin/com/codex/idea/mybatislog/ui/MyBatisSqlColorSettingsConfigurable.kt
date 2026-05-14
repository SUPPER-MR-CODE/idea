package com.codex.idea.mybatislog.ui

import com.codex.idea.mybatislog.service.MyBatisSqlColorSettingsService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.ColorPanel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class MyBatisSqlColorSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {
    private var panel: JPanel? = null
    private var selectColorPanel: ColorPanel? = null
    private var insertColorPanel: ColorPanel? = null
    private var updateColorPanel: ColorPanel? = null
    private var deleteColorPanel: ColorPanel? = null

    override fun getId(): String = "com.codex.idea.mybatislog.settings"

    override fun getDisplayName(): String = "MyBatis SQL"

    override fun createComponent(): JComponent {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        selectColorPanel = ColorPanel()
        insertColorPanel = ColorPanel()
        updateColorPanel = ColorPanel()
        deleteColorPanel = ColorPanel()

        panel = FormBuilder.createFormBuilder()
            .addComponentFillVertically(JPanel(), 0)
            .addLabeledComponent("SELECT Color", selectColorPanel!!)
            .addLabeledComponent("INSERT Color", insertColorPanel!!)
            .addLabeledComponent("UPDATE Color", updateColorPanel!!)
            .addLabeledComponent("DELETE Color", deleteColorPanel!!)
            .panel

        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        return selectColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.SELECT) ||
            insertColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.INSERT) ||
            updateColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.UPDATE) ||
            deleteColorPanel?.selectedColor != settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.DELETE)
    }

    override fun apply() {
        MyBatisSqlColorSettingsService.getInstance().updateColors(
            select = selectColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.SELECT),
            insert = insertColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.INSERT),
            update = updateColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.UPDATE),
            delete = deleteColorPanel!!.selectedColor ?: MyBatisSqlColorSettingsService.getInstance().colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.DELETE),
        )
    }

    override fun reset() {
        val settings = MyBatisSqlColorSettingsService.getInstance()
        selectColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.SELECT)
        insertColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.INSERT)
        updateColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.UPDATE)
        deleteColorPanel?.selectedColor = settings.colorFor(com.codex.idea.mybatislog.core.SqlOperationKind.DELETE)
    }

    override fun disposeUIResources() {
        panel = null
        selectColorPanel = null
        insertColorPanel = null
        updateColorPanel = null
        deleteColorPanel = null
    }
}
