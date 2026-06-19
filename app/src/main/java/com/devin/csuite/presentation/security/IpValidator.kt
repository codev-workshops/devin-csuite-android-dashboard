package com.devin.csuite.presentation.security

object IpValidator {

    private val IPV4_REGEX = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(/(\d{1,2}))?$""")
    private val IPV6_FULL_REGEX = Regex("""^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}(/(\d{1,3}))?$""")
    private val IPV6_COMPRESSED_REGEX = Regex("""^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}(/(\d{1,3}))?$""")

    fun validate(input: String): ValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("IP address cannot be empty")

        if (trimmed.contains('.')) {
            return validateIpv4(trimmed)
        }
        if (trimmed.contains(':')) {
            return validateIpv6(trimmed)
        }
        return ValidationResult.Invalid("Invalid IP address format")
    }

    private fun validateIpv4(input: String): ValidationResult {
        val match = IPV4_REGEX.matchEntire(input)
            ?: return ValidationResult.Invalid("Invalid IPv4 format")

        val octets = listOf(
            match.groupValues[1].toIntOrNull() ?: -1,
            match.groupValues[2].toIntOrNull() ?: -1,
            match.groupValues[3].toIntOrNull() ?: -1,
            match.groupValues[4].toIntOrNull() ?: -1
        )
        if (octets.any { it < 0 || it > 255 }) {
            return ValidationResult.Invalid("IPv4 octets must be 0-255")
        }

        val cidrPart = match.groupValues[6]
        if (cidrPart.isNotEmpty()) {
            val prefix = cidrPart.toIntOrNull()
            if (prefix == null || prefix < 0 || prefix > 32) {
                return ValidationResult.Invalid("IPv4 CIDR prefix must be 0-32")
            }
        }

        return ValidationResult.Valid
    }

    private fun validateIpv6(input: String): ValidationResult {
        val parts = input.split("/")
        val ipPart = parts[0]
        val cidrPart = parts.getOrNull(1)

        if (ipPart.contains(":::")) {
            return ValidationResult.Invalid("Invalid IPv6 format")
        }

        val isValid = IPV6_FULL_REGEX.matches(input) ||
            IPV6_COMPRESSED_REGEX.matches(input) ||
            isValidCompressedIpv6(ipPart)

        if (!isValid) {
            return ValidationResult.Invalid("Invalid IPv6 format")
        }

        if (cidrPart != null) {
            val prefix = cidrPart.toIntOrNull()
            if (prefix == null || prefix < 0 || prefix > 128) {
                return ValidationResult.Invalid("IPv6 CIDR prefix must be 0-128")
            }
        }

        return ValidationResult.Valid
    }

    private fun isValidCompressedIpv6(ip: String): Boolean {
        if (ip.contains(":::")) return false
        val doubleColonCount = ip.windowed(2).count { it == "::" }
        if (doubleColonCount > 1) return false

        val groups = if (ip.contains("::")) {
            val parts = ip.split("::")
            val left = if (parts[0].isEmpty()) emptyList() else parts[0].split(":")
            val right = if (parts.size > 1 && parts[1].isEmpty()) emptyList() else parts.getOrElse(1) { "" }.split(":")
            left + right
        } else {
            ip.split(":")
        }

        if (groups.isEmpty() || groups.size > 8) return false
        return groups.all { group ->
            group.isEmpty() || (group.length <= 4 && group.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' })
        }
    }

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
