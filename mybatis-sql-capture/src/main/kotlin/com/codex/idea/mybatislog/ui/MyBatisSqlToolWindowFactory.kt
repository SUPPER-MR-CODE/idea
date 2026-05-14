package com.codex.idea.mybatislog.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class MyBatisSqlToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = MyBatisSqlToolWindowPanel(project)
        val content = contentFactory().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun contentFactory(): ContentFactory {
        return runCatching {
            ContentFactory::class.java.getMethod("getInstance").invoke(null) as ContentFactory
        }.getOrElse {
            val serviceField = ContentFactory::class.java.getField("SERVICE")
            val service = serviceField.get(null)
            service.javaClass.getMethod("getInstance").invoke(service) as ContentFactory
        }
    }
}
