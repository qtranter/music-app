package com.audiomack.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.util.Base64
import com.audiomack.BuildConfig
import com.audiomack.GENERAL_PREFERENCES
import com.audiomack.utils.SecureSharedPreferences.SecurePreferencesException
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @param context your current context.
 * @param preferenceName name of preferences file (preferenceName.xml). Defaults to [GENERAL_PREFERENCES]
 * @param secureKey the key used for encryption, finding a good key scheme is hard.
 * Hardcoding your key in the application is bad, but better than plaintext preferences.
 * Having the user enter the key upon application launch is a safe(r) alternative,
 * but annoying to the user. Defaults to [BuildConfig.AM_PREFERENCES_SECRET]
 * @param encryptKeys settings this to false will only encrypt the values,
 * true will encrypt both values and keys. Keys can contain a lot of information about
 * the plaintext value of the value which can be used to decipher the value. Defaults to true
 * @throws SecurePreferencesException
 */
class SecureSharedPreferences(
    context: Context?,
    preferenceName: String = GENERAL_PREFERENCES,
    secureKey: String = BuildConfig.AM_PREFERENCES_SECRET,
    private val encryptKeys: Boolean = true
) : OnSharedPreferenceChangeListener {

    private val context = context ?: throw IllegalStateException("Context is null")
    private val preferences: SharedPreferences by lazy {
        this.context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    }

    private val writer: Cipher by lazy { Cipher.getInstance(TRANSFORMATION) }
    private val reader: Cipher by lazy { Cipher.getInstance(TRANSFORMATION) }
    private val keyWriter: Cipher by lazy { Cipher.getInstance(KEY_TRANSFORMATION) }
    private val keyReader: Cipher by lazy { Cipher.getInstance(KEY_TRANSFORMATION) }

    private val _changeObservable = PublishSubject.create<String>()
    val changeObservable: Observable<String> get() = _changeObservable

    init {
        try {
            initCiphers(secureKey)
        } catch (e: GeneralSecurityException) {
            throw SecurePreferencesException(e)
        } catch (e: UnsupportedEncodingException) {
            throw SecurePreferencesException(e)
        }

        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun contains(key: String): Boolean = preferences.contains(toKey(key))

    fun clear() = preferences.edit().clear().apply()

    @Throws(SecurePreferencesException::class)
    fun getString(key: String): String? = toKey(key)?.let {
        preferences.getString(it, null)?.let { value ->
            decrypt(value)
        }
    }

    fun put(key: String, value: String?) =
        value?.let { toKey(key)?.let { putValue(it, value) } } ?: remove(key)

    fun remove(key: String) = preferences.edit().remove(toKey(key)).apply()

    @Throws(
        UnsupportedEncodingException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        GeneralSecurityException::class,
        UnsupportedEncodingException::class
    )
    private fun initCiphers(secureKey: String) {
        val ivSpec: IvParameterSpec = getIv()
        val secretKey: SecretKeySpec = getSecretKey(secureKey)
        writer.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        reader.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        keyWriter.init(Cipher.ENCRYPT_MODE, secretKey)
        keyReader.init(Cipher.DECRYPT_MODE, secretKey)
    }

    private fun getIv(): IvParameterSpec {
        val iv = ByteArray(writer.blockSize)
        val byteArray = BYTE_ARRAY_SOURCE.toByteArray()
        System.arraycopy(byteArray, 0, iv, 0, writer.blockSize)
        return IvParameterSpec(iv)
    }

    @Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class)
    private fun getSecretKey(key: String): SecretKeySpec =
        SecretKeySpec(createKeyBytes(key), TRANSFORMATION)

    @Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class)
    private fun createKeyBytes(key: String): ByteArray =
        MessageDigest.getInstance(SECRET_KEY_HASH_TRANSFORMATION).apply { reset() }
            .digest(key.toByteArray(CHARSET))

    private fun toKey(key: String): String? = if (encryptKeys) {
        encrypt(key, keyWriter)
    } else {
        key
    }

    @Throws(SecurePreferencesException::class)
    private fun putValue(key: String, value: String) {
        val secureValueEncoded: String? = encrypt(value, writer)
        preferences.edit().putString(key, secureValueEncoded).apply()
    }

    @Throws(SecurePreferencesException::class)
    private fun encrypt(value: String, writer: Cipher): String? {
        val secureValue: ByteArray = try {
            convert(writer, value.toByteArray(CHARSET))
        } catch (e: Exception) {
            return null
        }
        return Base64.encodeToString(secureValue, Base64.NO_WRAP)
    }

    private fun decrypt(securedEncodedValue: String?, cipher: Cipher = reader): String? = try {
        val securedValue = Base64.decode(securedEncodedValue, Base64.NO_WRAP)
        val value = convert(cipher, securedValue)
        String(value, CHARSET)
    } catch (e: Exception) {
        null
    }

    @Throws(SecurePreferencesException::class)
    private fun convert(cipher: Cipher, bs: ByteArray): ByteArray = try {
        cipher.doFinal(bs)
    } catch (e: Exception) {
        throw SecurePreferencesException(e)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        decrypt(key, keyReader)?.let { _changeObservable.onNext(it) }
    }

    class SecurePreferencesException(override val cause: Throwable?) : RuntimeException(cause)

    companion object {
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val KEY_TRANSFORMATION = "AES/ECB/PKCS5Padding"
        private const val SECRET_KEY_HASH_TRANSFORMATION = "SHA-256"
        private const val BYTE_ARRAY_SOURCE = "fldsjfodasjifudslfjdsaofshaufihadsf"
        private val CHARSET = Charset.forName("UTF-8")
    }
}
