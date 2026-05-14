package com.codex.idea.mybatislog.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MyBatisLiveLogAssemblerTest {
    @Test
    fun `emit statement when preparing and parameters arrive`() {
        val statements = mutableListOf<ParsedStatement>()
        val assembler = MyBatisLiveLogAssembler(statements::add)

        assembler.acceptLine("2026-05-14 10:00:00 DEBUG ==>  Preparing: SELECT * FROM user WHERE id = ?")
        assembler.acceptLine("2026-05-14 10:00:00 DEBUG ==> Parameters: 7(Long)")

        assertEquals(1, statements.size)
        assertEquals("SELECT * FROM user WHERE id = 7", statements.first().executableSql)
    }

    @Test
    fun `flush pending sql when process ends without parameters`() {
        val statements = mutableListOf<ParsedStatement>()
        val assembler = MyBatisLiveLogAssembler(statements::add)

        assembler.acceptLine("==>  Preparing: DELETE FROM user WHERE deleted = ?")
        assembler.flush()

        assertEquals(1, statements.size)
        assertEquals("DELETE FROM user WHERE deleted = ?", statements.first().executableSql)
    }
}
