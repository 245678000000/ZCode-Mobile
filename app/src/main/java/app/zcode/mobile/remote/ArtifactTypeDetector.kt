package app.zcode.mobile.remote

import app.zcode.mobile.model.ArtifactKind

object ArtifactTypeDetector {
    fun fromName(name: String, mime: String? = null): ArtifactKind {
        return app.zcode.mobile.model.Artifact.kindFor(name, mime)
    }

    fun looksLikeArtifactName(name: String): Boolean {
        val kind = fromName(name)
        return kind != ArtifactKind.Unknown
    }
}
