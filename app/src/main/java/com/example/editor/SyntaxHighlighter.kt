package com.example.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "package", "import", "class", "interface", "fun", "val", "var", "if", "else", "for", "while", "return",
        "public", "private", "protected", "override", "data", "object", "sealed", "open", "internal", "null",
        "true", "false", "try", "catch", "finally", "throw", "when", "is", "as", "in", "this", "super",
        "break", "continue", "do", "enum", "lateinit", "suspend", "abstract", "extends", "implements",
        "native", "strictfp", "synchronized", "transient", "volatile", "void", "int", "long", "float",
        "double", "boolean", "char", "byte", "short", "static", "final"
    )

    private val KEYWORD_PATTERN = Regex("\\b(${KEYWORDS.joinToString("|")})\\b")
    private val STRING_PATTERN = Regex("\".*?\"")
    private val COMMENT_PATTERN = Regex("//.*|/\\*.*?\\*/")
    private val NUMBER_PATTERN = Regex("\\b\\d+\\b")

    private var lastCode: String? = null
    private var lastExtension: String? = null
    private var lastResult: AnnotatedString? = null

    fun highlight(code: String, extension: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        if (code.length > 50000) return AnnotatedString(code)
        
        if (code == lastCode && extension == lastExtension) {
            return lastResult ?: AnnotatedString(code)
        }

        val result = when (extension) {
            "java", "kt" -> highlightCode(code, Color(0xFFBB86FC))
            "xml" -> highlightXml(code)
            else -> AnnotatedString(code)
        }
        
        lastCode = code
        lastExtension = extension
        lastResult = result
        
        return result
    }

    private fun highlightCode(code: String, keywordColor: Color): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            
            // Apply styles in a specific order to handle overlaps better
            // Keywords
            KEYWORD_PATTERN.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }
            
            // Numbers
            NUMBER_PATTERN.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFFFFB74D)), match.range.first, match.range.last + 1)
            }
            
            // Strings (should override keywords/numbers)
            STRING_PATTERN.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFF03DAC5)), match.range.first, match.range.last + 1)
            }
            
            // Comments (should override everything)
            COMMENT_PATTERN.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFF6B6B6B)), match.range.first, match.range.last + 1)
            }
        }
    }

    private fun highlightXml(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            
            // Tags
            Regex("<[^>]+>").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFFBB86FC)), match.range.first, match.range.last + 1)
            }
            
            // Attributes (simple version)
            Regex("\\b[a-zA-Z0-9_:]+(?==)").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFF03DAC5)), match.range.first, match.range.last + 1)
            }
            
            // Attribute values
            Regex("\".*?\"").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFFCF6679)), match.range.first, match.range.last + 1)
            }
        }
    }
}
