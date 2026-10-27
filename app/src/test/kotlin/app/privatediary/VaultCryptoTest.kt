package app.privatediary

import app.privatediary.crypto.DeviceWrap
import app.privatediary.crypto.Kdf
import app.privatediary.crypto.VaultCrypto
import app.privatediary.crypto.validateNewPassword
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultCryptoTest {
    // Test-only stand-ins: the app uses Argon2id and the Android Keystore, which need a device.
    private val fastKdf = object : Kdf {
        override fun derive(password: ByteArray, salt: ByteArray, memoryKiB: Int, iterations: Int, parallelism: Int): ByteArray =
            Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(salt, "HmacSHA256")) }.doFinal(password)
    }
    private val identityWrap = object : DeviceWrap {
        override fun wrap(data: ByteArray) = data
        override fun unwrap(data: ByteArray) = data
    }
    private val crypto = VaultCrypto(fastKdf, identityWrap)
    private val password = "correct horse battery".toByteArray()

    @Test
    fun correctPasswordReturnsTheSameKey() {
        val created = crypto.create(password)
        assertEquals(32, created.dek.size)
        assertArrayEquals(created.dek, crypto.unlock(created.file, password))
    }

    @Test
    fun wrongPasswordIsRejected() {
        val created = crypto.create(password)
        assertNull(crypto.unlock(created.file, "wrong password!".toByteArray()))
    }

    @Test
    fun tamperedCiphertextOrHeaderIsRejected() {
        val created = crypto.create(password)
        val badTail = created.file.clone().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        val badSalt = created.file.clone().also { it[20] = (it[20].toInt() xor 1).toByte() }
        assertNull(crypto.unlock(badTail, password))
        assertNull(crypto.unlock(badSalt, password))
    }

    @Test
    fun changePasswordKeepsTheKeyAndRetiresTheOldPassword() {
        val created = crypto.create(password)
        val newPassword = "a brand new passphrase".toByteArray()
        val changed = crypto.changePassword(created.file, password, newPassword)
        assertNotNull(changed)
        assertArrayEquals(created.dek, crypto.unlock(changed!!, newPassword))
        assertNull(crypto.unlock(changed, password))
        assertNull(crypto.changePassword(created.file, "not it".toByteArray(), newPassword))
    }

    @Test
    fun everyVaultGetsAFreshSaltAndKey() {
        val a = crypto.create(password)
        val b = crypto.create(password)
        assertFalse(a.file.copyOfRange(16, 32).contentEquals(b.file.copyOfRange(16, 32)))
        assertFalse(a.dek.contentEquals(b.dek))
    }

    @Test
    fun passwordRules() {
        assertNotNull(validateNewPassword("short", "short"))
        assertNotNull(validateNewPassword("long enough password", "different"))
        assertTrue(validateNewPassword("long enough password", "long enough password") == null)
    }
}
