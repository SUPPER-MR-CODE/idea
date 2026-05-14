package com.codex.idea.mybatislog.service

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

class MyBatisExecutionListener(private val project: Project) : ExecutionListener {
    override fun processStarted(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
    ) {
        project.getService(MyBatisProcessCaptureService::class.java).attach(env, handler)
    }
}
