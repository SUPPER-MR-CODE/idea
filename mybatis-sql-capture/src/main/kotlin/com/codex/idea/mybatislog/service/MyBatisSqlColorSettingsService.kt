package com.codex.idea.mybatislog.service

import com.codex.idea.mybatislog.core.SqlOperationKind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import java.awt.Color

@State(name = "MyBatisSqlColorSettings", storages = [Storage("mybatis-sql-helper.xml")])
@Service(Service.Level.APP)
class MyBatisSqlColorSettingsService : PersistentStateComponent<MyBatisSqlColorSettingsService.State> {
    data class State(
        var selectColorHex: String = "#2E7D32",
        var insertColorHex: String = "#EF6C00",
        var updateColorHex: String = "#1565C0",
        var deleteColorHex: String = "#C62828",
        var sqlFontSize: Int = 13,
        var sqlFontBold: Boolean = false,
    )

    interface Listener {
        fun settingsChanged()
    }

    companion object {
        val TOPIC: Topic<Listener> = Topic.create("MyBatis SQL Color Settings", Listener::class.java)

        fun getInstance(): MyBatisSqlColorSettingsService {
            return ApplicationManager.getApplication().getService(MyBatisSqlColorSettingsService::class.java)
        }
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun colorFor(kind: SqlOperationKind): Color {
        val hex = when (kind) {
            SqlOperationKind.SELECT -> state.selectColorHex
            SqlOperationKind.INSERT -> state.insertColorHex
            SqlOperationKind.UPDATE -> state.updateColorHex
            SqlOperationKind.DELETE -> state.deleteColorHex
            SqlOperationKind.UNKNOWN -> "#616161"
        }
        return Color.decode(hex)
    }

    fun updateColors(select: Color, insert: Color, update: Color, delete: Color) {
        state = State(
            selectColorHex = toHex(select),
            insertColorHex = toHex(insert),
            updateColorHex = toHex(update),
            deleteColorHex = toHex(delete),
            sqlFontSize = state.sqlFontSize,
            sqlFontBold = state.sqlFontBold,
        )
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).settingsChanged()
    }

    fun sqlFontSize(): Int = state.sqlFontSize.coerceIn(10, 28)

    fun isSqlFontBold(): Boolean = state.sqlFontBold

    fun updateAppearance(
        select: Color,
        insert: Color,
        update: Color,
        delete: Color,
        sqlFontSize: Int,
        sqlFontBold: Boolean,
    ) {
        state = State(
            selectColorHex = toHex(select),
            insertColorHex = toHex(insert),
            updateColorHex = toHex(update),
            deleteColorHex = toHex(delete),
            sqlFontSize = sqlFontSize.coerceIn(10, 28),
            sqlFontBold = sqlFontBold,
        )
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).settingsChanged()
    }

    private fun toHex(color: Color): String {
        return "#%02X%02X%02X".format(color.red, color.green, color.blue)
    }
}
