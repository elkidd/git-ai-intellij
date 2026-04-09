package com.gitai.blame

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileEditor.FileDocumentManager

class GitAiBlameToggleAction : ToggleAction(
    "Toggle Git AI Blame",
    "Show/hide AI authorship annotations in the gutter",
    null
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean =
        GitAiBlameService.getInstance().enabled

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val service = GitAiBlameService.getInstance()
        val nowEnabled = service.toggle()
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        if (nowEnabled) {
            val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return
            service.loadBlameAsync(file, editor) { attributions ->
                if (attributions.isNotEmpty() && service.enabled) {
                    val provider = GitAiBlameGutterProvider(attributions)
                    editor.gutter.registerTextAnnotation(provider)
                }
            }
        } else {
            editor.gutter.closeAllAnnotations()
        }
    }
}
