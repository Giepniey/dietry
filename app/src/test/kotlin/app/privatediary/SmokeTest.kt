package app.privatediary

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class SmokeTest {
    // Guards the "fully offline" requirement: fails the build if INTERNET is ever added.
    @Test
    fun manifestDeclaresNoInternetPermission() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.INTERNET"))
    }
}
