package com.gitai.blame

import com.intellij.openapi.editor.EditorGutterAction
import com.intellij.openapi.editor.TextAnnotationGutterProvider
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorFontType
import java.awt.Color
import java.awt.Cursor

class GitAiBlameGutterProvider(
    private val attributions: Map<Int, LineAttribution>
) : TextAnnotationGutterProvider, EditorGutterAction {

    private val agentColors = mapOf(
        "github-copilot" to ColorKey.createColorKey("GITAI_COPILOT", Color(0x6CC644)),
        "copilot" to ColorKey.createColorKey("GITAI_COPILOT", Color(0x6CC644)),
        "cursor" to ColorKey.createColorKey("GITAI_CURSOR", Color(0xA855F7)),
        "claude" to ColorKey.createColorKey("GITAI_CLAUDE", Color(0xE8956A)),
        "gemini" to ColorKey.createColorKey("GITAI_GEMINI", Color(0x4285F4)),
        "windsurf" to ColorKey.createColorKey("GITAI_WINDSURF", Color(0x38BDF8)),
        "codex" to ColorKey.createColorKey("GITAI_CODEX", Color(0xF97316)),
    )
    private val defaultColor = ColorKey.createColorKey("GITAI_DEFAULT", Color(0x9CA3AF))

    private fun shortenAgent(tool: String): String = when {
        tool.contains("github-copilot", ignoreCase = true) -> "copilot"
        tool.contains("copilot", ignoreCase = true) -> "copilot"
        else -> tool.lowercase()
    }

    private fun shortenModel(model: String): String = when {
        model.isBlank() || model == "unknown" -> ""
        else -> model
            .removePrefix("claude-")
            .removePrefix("gpt-")
            .removePrefix("gemini-")
    }

    override fun getLineText(line: Int, editor: com.intellij.openapi.editor.Editor?): String? {
        val attr = attributions[line] ?: return null
        val agent = shortenAgent(attr.prompt.agentId.tool)
        val model = shortenModel(attr.prompt.agentId.model)
        return if (model.isNotEmpty()) " $agent · $model " else " $agent "
    }

    override fun getToolTip(line: Int, editor: com.intellij.openapi.editor.Editor?): String? {
        val attr = attributions[line] ?: return null
        val p = attr.prompt
        return buildString {
            append("<html><b>🤖 AI-authored</b><br/>")
            append("<b>Agent:</b> ${p.agentId.tool}<br/>")
            append("<b>Model:</b> ${p.agentId.model}<br/>")
            if (p.humanAuthor.isNotEmpty()) append("<b>Author:</b> ${p.humanAuthor}<br/>")
            append("<b>Lines:</b> +${p.totalAdditions} added, ${p.acceptedLines} accepted")
            if (p.overriddenLines > 0) append(", ${p.overriddenLines} overridden")
            append("</html>")
        }
    }

    override fun getStyle(line: Int, editor: com.intellij.openapi.editor.Editor?): EditorFontType =
        EditorFontType.ITALIC

    override fun getColor(line: Int, editor: com.intellij.openapi.editor.Editor?): ColorKey? {
        val attr = attributions[line] ?: return null
        val tool = attr.prompt.agentId.tool.lowercase()
        return agentColors[tool] ?: defaultColor
    }

    override fun getBgColor(line: Int, editor: com.intellij.openapi.editor.Editor?): Color? = null

    override fun getPopupActions(line: Int, editor: com.intellij.openapi.editor.Editor?): MutableList<com.intellij.openapi.actionSystem.AnAction> =
        mutableListOf()

    override fun gutterClosed() {}

    // EditorGutterAction
    override fun doAction(lineNum: Int) {}
    override fun getCursor(lineNum: Int): Cursor = Cursor.getDefaultCursor()
}

