package com.codex.idea.mybatislog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyBatisLogParserTest {
    @Test
    fun `convert single statement`() {
        val log = """
            ==>  Preparing: SELECT * FROM user WHERE id = ? AND status = ?
            ==> Parameters: 42(Long), ACTIVE(String)
        """.trimIndent()

        val result = MyBatisLogParser.parse(log)

        assertEquals(1, result.statements.size)
        assertEquals(
            "SELECT * FROM user WHERE id = 42 AND status = 'ACTIVE'",
            result.statements.first().executableSql,
        )
    }

    @Test
    fun `keep commas inside string parameters`() {
        val log = """
            ==>  Preparing: SELECT * FROM user WHERE nickname = ? AND age >= ?
            ==> Parameters: hello,world(String), 18(Integer)
        """.trimIndent()

        val result = MyBatisLogParser.parse(log)

        assertEquals(
            "SELECT * FROM user WHERE nickname = 'hello,world' AND age >= 18",
            result.statements.first().executableSql,
        )
    }

    @Test
    fun `support multiple statements`() {
        val log = """
            ==>  Preparing: SELECT * FROM user WHERE id = ?
            ==> Parameters: 1(Long)
            <==      Total: 1
            ==>  Preparing: UPDATE user SET deleted = ? WHERE id = ?
            ==> Parameters: true(Boolean), 2(Long)
            <==    Updates: 1
        """.trimIndent()

        val result = MyBatisLogParser.parse(log)

        assertEquals(2, result.statements.size)
        assertEquals("SELECT * FROM user WHERE id = 1", result.statements[0].executableSql)
        assertEquals("UPDATE user SET deleted = true WHERE id = 2", result.statements[1].executableSql)
    }

    @Test
    fun `warn when parameters are missing`() {
        val log = "==>  Preparing: SELECT * FROM user WHERE id = ?"

        val result = MyBatisLogParser.parse(log)

        assertTrue(result.statements.first().warning?.contains("missing", ignoreCase = true) == true)
        assertEquals("SELECT * FROM user WHERE id = ?", result.statements.first().executableSql)
    }
}
