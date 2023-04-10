import java.math.BigInteger
import java.security.*
import java.util.*

data class Block(
    val id: Int,
    val timestamp: String,
    val money: Int,
    var hash: String,
    val prevHash: String,
    val validator: String
) {
    init {
        hash = calculateHash()
    }

    fun calculateHash(): String {
        return "$id$timestamp$money$prevHash".hash()
    }

    fun isValidatorNull(): Boolean {
        return this.validator.isNullOrEmpty()
    }
}

fun String.hash(algorithm: String = "SHA-256"): String {
    val messageDigest = MessageDigest.getInstance(algorithm)
    messageDigest.update(this.toByteArray())
    return String.format("%064x", BigInteger(1, messageDigest.digest()))
}