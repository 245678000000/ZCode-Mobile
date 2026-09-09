package app.zcode.mobile.model

import android.net.Uri

enum class ArtifactKind {
    Markdown,
    Html,
    Image,
    Pdf,
    Code,
    Json,
    Text,
    Unknown,
}

data class Artifact(
    val name: String,
    val uri: Uri,
    val kind: ArtifactKind,
    val mimeType: String? = null,
) {
    companion object {
        fun from(name: String, uri: Uri, mimeType: String? = null): Artifact {
            return Artifact(
                name = name,
                uri = uri,
                kind = kindFor(name, mimeType),
                mimeType = mimeType,
            )
        }

        fun kindFor(name: String, mimeType: String? = null): ArtifactKind {
            val lower = name.lowercase()
            val mime = mimeType?.lowercase().orEmpty()
            return when {
                lower.endsWith(".md") || mime.contains("markdown") -> ArtifactKind.Markdown
                lower.endsWith(".html") || lower.endsWith(".htm") || mime.contains("text/html") -> ArtifactKind.Html
                lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".webp") || lower.endsWith(".gif") || mime.startsWith("image/") -> ArtifactKind.Image
                lower.endsWith(".pdf") || mime.contains("pdf") -> ArtifactKind.Pdf
                lower.endsWith(".json") || mime.contains("json") -> ArtifactKind.Json
                lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".ts") ||
                    lower.endsWith(".tsx") || lower.endsWith(".js") || lower.endsWith(".py") ||
                    lower.endsWith(".go") || lower.endsWith(".rs") || lower.endsWith(".sh") -> ArtifactKind.Code
                lower.endsWith(".txt") || mime.startsWith("text/") -> ArtifactKind.Text
                else -> ArtifactKind.Unknown
            }
        }
    }
}
