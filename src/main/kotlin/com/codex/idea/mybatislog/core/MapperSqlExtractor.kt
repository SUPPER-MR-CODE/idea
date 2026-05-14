package com.codex.idea.mybatislog.core

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText

data class ExtractedSqlTemplate(
    val title: String,
    val sqlTemplate: String,
)

object MapperSqlExtractor {
    private val statementTagNames = setOf("select", "insert", "update", "delete")
    private val annotationNames = setOf("Select", "Insert", "Update", "Delete")

    fun extract(file: PsiFile, editor: Editor): ExtractedSqlTemplate? {
        val element = file.findElementAt(editor.caretModel.offset) ?: return null

        return extractFromXml(element) ?: extractFromAnnotation(element)
    }

    private fun extractFromXml(element: PsiElement): ExtractedSqlTemplate? {
        val statementTag = generateSequence(PsiTreeUtil.getParentOfType(element, XmlTag::class.java)) { current ->
            PsiTreeUtil.getParentOfType(current, XmlTag::class.java)
        }.firstOrNull { it.name.lowercase() in statementTagNames } ?: return null

        val statementId = statementTag.getAttributeValue("id") ?: statementTag.name
        val template = flattenXmlTag(statementTag)
        if (template.isBlank()) {
            return null
        }

        return ExtractedSqlTemplate("Mapper XML: $statementId", template)
    }

    private fun flattenXmlTag(tag: XmlTag): String {
        val builder = StringBuilder()

        fun appendTag(current: XmlTag) {
            current.value.children.forEach { child ->
                when (child) {
                    is XmlText -> builder.append(' ').append(child.value.trim())
                    is XmlTag -> appendTag(child)
                }
            }
        }

        appendTag(tag)
        return builder.toString()
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun extractFromAnnotation(element: PsiElement): ExtractedSqlTemplate? {
        val annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation::class.java) ?: return null
        val qualifiedName = annotation.qualifiedName ?: return null
        if (qualifiedName.substringAfterLast('.') !in annotationNames) {
            return null
        }

        val value = annotation.findAttributeValue("value") ?: return null
        val template = extractAnnotationValue(value)?.replace(Regex("""\s+"""), " ")?.trim().orEmpty()
        if (template.isBlank()) {
            return null
        }

        return ExtractedSqlTemplate("Mapper Annotation: ${qualifiedName.substringAfterLast('.')}", template)
    }

    private fun extractAnnotationValue(value: PsiAnnotationMemberValue): String? {
        return when (value) {
            is PsiLiteralExpression -> value.value as? String
            is PsiArrayInitializerMemberValue -> value.initializers
                .mapNotNull(::extractAnnotationValue)
                .joinToString(separator = " ")
                .ifBlank { null }

            is PsiPolyadicExpression -> value.operands
                .mapNotNull { operand ->
                    when (operand) {
                        is PsiLiteralExpression -> operand.value as? String
                        else -> null
                    }
                }
                .joinToString(separator = " ")
                .ifBlank { null }

            else -> null
        }
    }
}
