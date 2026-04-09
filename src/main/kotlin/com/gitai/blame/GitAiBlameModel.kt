package com.gitai.blame

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AgentId(
    val tool: String = "unknown",
    val id: String = "",
    val model: String = "unknown"
)

data class PromptInfo(
    @SerializedName("agent_id") val agentId: AgentId = AgentId(),
    @SerializedName("human_author") val humanAuthor: String = "",
    @SerializedName("total_additions") val totalAdditions: Int = 0,
    @SerializedName("total_deletions") val totalDeletions: Int = 0,
    @SerializedName("accepted_lines") val acceptedLines: Int = 0,
    @SerializedName("overriden_lines") val overriddenLines: Int = 0
)

data class BlameResponse(
    val lines: Map<String, String> = emptyMap(),
    val prompts: Map<String, PromptInfo> = emptyMap()
)

data class LineAttribution(
    val promptId: String,
    val prompt: PromptInfo
) {
    val agentLabel: String
        get() = "${prompt.agentId.tool} (${prompt.agentId.model})"
}

object GitAiBlameParser {
    private val gson = Gson()

    fun parse(json: String): Map<Int, LineAttribution> {
        val response = gson.fromJson(json, BlameResponse::class.java) ?: return emptyMap()
        val result = mutableMapOf<Int, LineAttribution>()

        for ((lineSpec, promptId) in response.lines) {
            val prompt = response.prompts[promptId] ?: continue
            val attribution = LineAttribution(promptId, prompt)

            // Line specs can be: "5", "51-52,54,58-110"
            for (segment in lineSpec.split(",")) {
                val trimmed = segment.trim()
                if (trimmed.contains("-")) {
                    val parts = trimmed.split("-")
                    val start = parts[0].toIntOrNull() ?: continue
                    val end = parts[1].toIntOrNull() ?: continue
                    for (line in start..end) {
                        result[line - 1] = attribution // convert to 0-based
                    }
                } else {
                    val line = trimmed.toIntOrNull() ?: continue
                    result[line - 1] = attribution // convert to 0-based
                }
            }
        }
        return result
    }
}
