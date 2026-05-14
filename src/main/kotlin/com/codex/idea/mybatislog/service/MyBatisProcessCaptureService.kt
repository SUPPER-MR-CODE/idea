package com.codex.idea.mybatislog.service

import com.codex.idea.mybatislog.core.MyBatisLiveLogAssembler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class MyBatisProcessCaptureService(
    private val project: Project,
) {
    private val attachedKey = Key.create<Boolean>("mybatis.log.helper.attached")
    private val sessions = ConcurrentHashMap<ProcessHandler, CaptureSession>()

    fun attach(environment: ExecutionEnvironment, handler: ProcessHandler) {
        if (handler.getUserData(attachedKey) == true) {
            return
        }
        handler.putUserData(attachedKey, true)

        val sourceName = environment.runProfile.name
        val historyService = project.getService(MyBatisSqlHistoryService::class.java)
        val session = CaptureSession(
            sourceName = sourceName,
            assembler = MyBatisLiveLogAssembler { statement ->
                historyService.addEntry(sourceName, statement.executableSql, statement.warning)
            },
        )
        sessions[handler] = session

        handler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                session.acceptChunk(event.text)
            }

            override fun processTerminated(event: ProcessEvent) {
                session.flush()
                sessions.remove(handler)
            }
        })
    }

    private class CaptureSession(
        private val sourceName: String,
        private val assembler: MyBatisLiveLogAssembler,
    ) {
        private val pendingChunk = StringBuilder()

        fun acceptChunk(chunk: String) {
            pendingChunk.append(chunk)
            var newlineIndex = pendingChunk.indexOf("\n")
            while (newlineIndex >= 0) {
                val line = pendingChunk.substring(0, newlineIndex)
                pendingChunk.delete(0, newlineIndex + 1)
                assembler.acceptLine(line)
                newlineIndex = pendingChunk.indexOf("\n")
            }
        }

        fun flush() {
            if (pendingChunk.isNotEmpty()) {
                assembler.acceptLine(pendingChunk.toString())
                pendingChunk.clear()
            }
            assembler.flush()
        }
    }
}
