package app.privatediary.crypto

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Password-based key derivation. The app uses Argon2id; unit tests use a fast stand-in. */
interface Kdf {
    fun derive(password: ByteArray, salt: ByteArray, memoryKiB: Int, iterations: Int, parallelism: Int): ByteArray
}

/** Extra layer that binds the vault file to this device (Android Keystore in the app). */
interface DeviceWrap {
    fun wrap(data: ByteArray): ByteArray
    fun unwrap(data: ByteArray): ByteArray
}

/**
 * Key hierarchy: a random 256-bit data key (DEK) encrypts the database. The DEK is stored only
 * wrapped: AES-256-GCM under a key derived from the master password (Argon2id), then wrapped again
 * by the device key. Changing the password re-wraps the DEK; the data is not re-encrypted.
 *
 * File layout: header (magic, memory, iterations, parallelism, salt) || deviceWrap(iv || GCM(DEK)).
 * The header is authenticated as GCM associated data.
 */
class VaultCrypto(
    private val kdf: Kdf,
    private val device: DeviceWrap,
    private val random: SecureRandom = SecureRandom(),
    private val memoryKiB: Int = 65536,
    private val iterations: Int = 3,
    private val parallelism: Int = 1,
) {
    class Created(val file: ByteArray, val dek: ByteArray)

    fun create(password: ByteArray): Created {
        val dek = ByteArray(KEY_LEN).also(random::nextBytes)
        return Created(seal(dek, password), dek)
    }

    /** Returns the data key, or null if the password is wrong or the file was tampered with. */
    fun unlock(file: ByteArray, password: ByteArray): ByteArray? {
        check(file.size >= HEADER_LEN + IV_LEN + KEY_LEN + TAG_LEN) { "Vault file is truncated" }
        val header = file.copyOfRange(0, HEADER_LEN)
        val buf = ByteBuffer.wrap(header)
        check(buf.int == MAGIC) { "Not a vault file" }
        val m = buf.int
        val t = buf.int
        val p = buf.int
        // Bounds stop a corrupted file from demanding absurd memory or time.
        check(m in 8..(1 shl 20) && t in 1..64 && p in 1..16) { "Vault parameters out of range" }
        val salt = ByteArray(SALT_LEN).also { buf.get(it) }

        val inner = device.unwrap(file.copyOfRange(HEADER_LEN, file.size))
        val kek = kdf.derive(password, salt, m, t, p)
        return try {
            gcm(Cipher.DECRYPT_MODE, kek, inner.copyOfRange(0, IV_LEN), inner.copyOfRange(IV_LEN, inner.size), header)
        } catch (e: AEADBadTagException) {
            null
        } finally {
            kek.fill(0)
        }
    }

    /** Returns a new vault file under the new password, or null if the old password is wrong. */
    fun changePassword(file: ByteArray, oldPassword: ByteArray, newPassword: ByteArray): ByteArray? {
        val dek = unlock(file, oldPassword) ?: return null
        return try {
            seal(dek, newPassword) // fresh salt every time
        } finally {
            dek.fill(0)
        }
    }

    private fun seal(dek: ByteArray, password: ByteArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_LEN)
            .putInt(MAGIC).putInt(memoryKiB).putInt(iterations).putInt(parallelism).put(salt).array()
        val kek = kdf.derive(password, salt, memoryKiB, iterations, parallelism)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val inner = iv + gcm(Cipher.ENCRYPT_MODE, kek, iv, dek, header)
        kek.fill(0)
        return header + device.wrap(inner)
    }

    private fun gcm(mode: Int, key: ByteArray, iv: ByteArray, input: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LEN * 8, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(input)
    }

    private companion object {
        const val MAGIC = 0x50445631 // "PDV1"
        const val KEY_LEN = 32
        const val SALT_LEN = 16
        const val IV_LEN = 12
        const val TAG_LEN = 16
        const val HEADER_LEN = 4 + 4 + 4 + 4 + SALT_LEN
    }
}

const val MIN_PASSWORD_LENGTH = 10

/** Returns a user-facing problem, or null if the new password is acceptable. */
fun validateNewPassword(password: String, confirm: String): String? = when {
    password.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters."
    password != confirm -> "The passwords don't match."
    else -> null
}
