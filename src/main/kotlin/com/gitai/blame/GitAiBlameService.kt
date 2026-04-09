package com.gitai.blame

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service
class GitAiBlameService {

    private val cache = ConcurrentHashMap<String, Map<Int, LineAttribution>>()
    private val log = Logger.getInstance(GitAiBlameService::class.java)
    private var binaryPath: String? = null
    private var binaryChecked = false

    var enabled = false
        private set

    fun toggle(): Boolean {
        enabled = !enabled
        if (!enabled) cache.clear()
        return enabled
    }

    fun loadBlameAsync(file: VirtualFile, editor: Editor, onReady: (Map<Int, LineAttribution>) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = loadBlame(file)
            cache[file.path] = result
            invokeLater {
                onReady(result)
            }
        }
    }

    fun getCached(file: VirtualFile): Map<Int, LineAttribution>? = cache[file.path]

    fun invalidate(file: VirtualFile) {
        cache.remove(file.path)
    }

    private fun loadBlame(file: VirtualFile): Map<Int, LineAttribution> {
        val binary = findGitAiBinary() ?: return emptyMap()
        val filePath = file.path
        val workDir = File(filePath).parentFile ?: return emptyMap()

        return try {
            val cmd = GeneralCommandLine(binary, "blame", filePath, "--json")
                .withWorkDirectory(workDir)
                .withCharset(Charsets.UTF_8)

            val handler = CapturingProcessHandler(cmd)
            val result = handler.runProcess(10_000)

            if (result.exitCode != 0) {
                log.debug("git-ai blame failed for $filePath: ${result.stderr}")
                return emptyMap()
            }

            GitAiBlameParser.parse(result.stdout)
        } catch (e: Exception) {
            log.debug("git-ai blame error for $filePath", e)
            emptyMap()
        }
    }

    private fun findGitAiBinary(): String? {
        if (binaryChecked) return binaryPath
        binaryChecked = true

        val home = System.getProperty("user.home")
        val knownPath = "$home/.git-ai/bin/git-ai"
        if (File(knownPath).canExecute()) {
            binaryPath = knownPath
            return binaryPath
        }

        binaryPath = try {
            val cmd = GeneralCommandLine("which", "git-ai").withCharset(Charsets.UTF_8)
            val result = CapturingProcessHandler(cmd).runProcess(3_000)
            val path = result.stdout.trim()
            if (result.exitCode == 0 && path.isNotEmpty()) path else null
        } catch (e: Exception) {
            null
        }
        return binaryPath
    }

    companion object {
        fun getInstance(): GitAiBlameService =
            ApplicationManager.getApplication().getService(GitAiBlameService::class.java)
    }
}
