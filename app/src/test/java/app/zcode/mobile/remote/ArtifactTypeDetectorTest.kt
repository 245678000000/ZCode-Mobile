package app.zcode.mobile.remote

import app.zcode.mobile.model.ArtifactKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtifactTypeDetectorTest {
    @Test
    fun detectsExtensions() {
        assertEquals(ArtifactKind.Markdown, ArtifactTypeDetector.fromName("a.md"))
        assertEquals(ArtifactKind.Html, ArtifactTypeDetector.fromName("a.html"))
        assertEquals(ArtifactKind.Image, ArtifactTypeDetector.fromName("a.png"))
        assertEquals(ArtifactKind.Pdf, ArtifactTypeDetector.fromName("a.pdf"))
        assertEquals(ArtifactKind.Json, ArtifactTypeDetector.fromName("a.json"))
        assertEquals(ArtifactKind.Code, ArtifactTypeDetector.fromName("Home.tsx"))
        assertEquals(ArtifactKind.Unknown, ArtifactTypeDetector.fromName("noext"))
        assertTrue(ArtifactTypeDetector.looksLikeArtifactName("notes.md"))
    }
}
