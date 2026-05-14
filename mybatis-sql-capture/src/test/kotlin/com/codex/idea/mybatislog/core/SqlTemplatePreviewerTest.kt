package com.codex.idea.mybatislog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlTemplatePreviewerTest {
    @Test
    fun `preview mapper sql with hash placeholders`() {
        val result = SqlTemplatePreviewer.preview(
            "select * from user where id = #{id} and status = #{status}",
            "42(Long), ACTIVE(String)",
        )

        assertEquals(
            "select *\nfrom user\nwhere id = 42\nand status = 'ACTIVE'",
            result.executableSql,
        )
    }

    @Test
    fun `keep dollar placeholders and warn`() {
        val result = SqlTemplatePreviewer.preview(
            "select * from ${'$'}{table} where id = #{id}",
            "1(Long)",
        )

        assertTrue(result.warnings.any { it.contains("${'$'}{}", ignoreCase = false) })
        assertEquals(
            "select *\nfrom ${'$'}{table}\nwhere id = 1",
            result.executableSql,
        )
    }
}
