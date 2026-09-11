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
    val id: String = "",
    val taskId: String? = null,
    val name: String,
    val kind: ArtifactKind,
    val sourceUrl: String? = null,
    val localUri: Uri? = null,
    val mimeType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val uri: Uri
        get() = localUri ?: sourceUrl?.let { Uri.parse(it) } ?: Uri.EMPTY

    companion object {
        fun from(name: String, uri: Uri, mimeType: String? = null): Artifact {
            return Artifact(
                id = name,
                name = name,
                kind = kindFor(name, mimeType),
                localUri = uri,
                mimeType = mimeType,
            )
        }

        fun kindFor(name: String, mimeType: String? = null): ArtifactKind {
            val lower = name.lowercase()
            val mime = mimeType?.lowercase().orEmpty()
            val path = lower.substringBefore('?')
            return when {
                path.endsWith(".md") || mime.contains("markdown") -> ArtifactKind.Markdown
                path.endsWith(".html") || path.endsWith(".htm") || mime.contains("text/html") -> ArtifactKind.Html
                path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                    path.endsWith(".webp") || path.endsWith(".gif") || mime.startsWith("image/") -> ArtifactKind.Image
                path.endsWith(".pdf") || mime.contains("pdf") -> ArtifactKind.Pdf
                path.endsWith(".json") || mime.contains("json") -> ArtifactKind.Json
                path.endsWith(".kt") || path.endsWith(".java") || path.endsWith(".ts") ||
                    path.endsWith(".tsx") || path.endsWith(".js") || path.endsWith(".py") ||
                    path.endsWith(".go") || path.endsWith(".rs") || path.endsWith(".sh") -> ArtifactKind.Code
                path.endsWith(".txt") || mime.startsWith("text/") -> ArtifactKind.Text
                else -> ArtifactKind.Unknown
            }
        }
    }
}
