package com.devin.csuite.presentation.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IpValidatorTest {

    // ---- Valid IPv4 Addresses ----

    @Test
    fun `valid IPv4 - standard address`() {
        assertValid("192.168.1.1")
    }

    @Test
    fun `valid IPv4 - loopback`() {
        assertValid("127.0.0.1")
    }

    @Test
    fun `valid IPv4 - all zeros`() {
        assertValid("0.0.0.0")
    }

    @Test
    fun `valid IPv4 - max octets`() {
        assertValid("255.255.255.255")
    }

    @Test
    fun `valid IPv4 - class A private`() {
        assertValid("10.0.0.1")
    }

    @Test
    fun `valid IPv4 - class B private`() {
        assertValid("172.16.0.1")
    }

    // ---- Valid IPv4 with CIDR ----

    @Test
    fun `valid IPv4 CIDR - slash 8`() {
        assertValid("10.0.0.0/8")
    }

    @Test
    fun `valid IPv4 CIDR - slash 24`() {
        assertValid("192.168.0.0/24")
    }

    @Test
    fun `valid IPv4 CIDR - slash 32`() {
        assertValid("192.168.1.1/32")
    }

    @Test
    fun `valid IPv4 CIDR - slash 0`() {
        assertValid("0.0.0.0/0")
    }

    @Test
    fun `valid IPv4 CIDR - slash 16`() {
        assertValid("172.16.0.0/16")
    }

    // ---- Invalid IPv4 Addresses ----

    @Test
    fun `invalid IPv4 - octet over 255`() {
        assertInvalid("999.1.1.1")
    }

    @Test
    fun `invalid IPv4 - too few octets`() {
        assertInvalid("1.2.3")
    }

    @Test
    fun `invalid IPv4 - alpha characters`() {
        assertInvalid("abc.def.ghi.jkl")
    }

    @Test
    fun `invalid IPv4 - octet 256`() {
        assertInvalid("256.0.0.1")
    }

    @Test
    fun `invalid IPv4 - too many octets`() {
        assertInvalid("1.2.3.4.5")
    }

    @Test
    fun `invalid IPv4 - negative octet`() {
        assertInvalid("-1.0.0.1")
    }

    @Test
    fun `invalid IPv4 - empty octet`() {
        assertInvalid("192..1.1")
    }

    @Test
    fun `invalid IPv4 CIDR - prefix 33`() {
        assertInvalid("192.168.1.0/33")
    }

    @Test
    fun `invalid IPv4 CIDR - negative prefix`() {
        assertInvalid("192.168.1.0/-1")
    }

    // ---- Valid IPv6 Addresses ----

    @Test
    fun `valid IPv6 - loopback`() {
        assertValid("::1")
    }

    @Test
    fun `valid IPv6 - abbreviated`() {
        assertValid("2001:db8::1")
    }

    @Test
    fun `valid IPv6 - full address`() {
        assertValid("2001:0db8:85a3:0000:0000:8a2e:0370:7334")
    }

    @Test
    fun `valid IPv6 - all zeros compressed`() {
        assertValid("::")
    }

    @Test
    fun `valid IPv6 - link local`() {
        assertValid("fe80::1")
    }

    @Test
    fun `valid IPv6 - multiple groups compressed`() {
        assertValid("2001:db8::8a2e:370:7334")
    }

    // ---- Valid IPv6 with CIDR ----

    @Test
    fun `valid IPv6 CIDR - slash 32`() {
        assertValid("2001:db8::/32")
    }

    @Test
    fun `valid IPv6 CIDR - slash 128`() {
        assertValid("::1/128")
    }

    @Test
    fun `valid IPv6 CIDR - slash 0`() {
        assertValid("::/0")
    }

    @Test
    fun `valid IPv6 CIDR - slash 64`() {
        assertValid("fe80::/64")
    }

    // ---- Invalid IPv6 Formats ----

    @Test
    fun `invalid IPv6 - triple colon`() {
        assertInvalid("2001:::1")
    }

    @Test
    fun `invalid IPv6 - too many groups`() {
        assertInvalid("2001:db8:85a3:0000:0000:8a2e:0370:7334:extra")
    }

    @Test
    fun `invalid IPv6 - invalid hex character`() {
        assertInvalid("2001:db8::gggg")
    }

    @Test
    fun `invalid IPv6 - group too long`() {
        assertInvalid("2001:db8::12345")
    }

    @Test
    fun `invalid IPv6 CIDR - prefix 129`() {
        assertInvalid("::1/129")
    }

    @Test
    fun `invalid IPv6 CIDR - negative prefix`() {
        assertInvalid("::1/-1")
    }

    // ---- CIDR Prefix Length Validation ----

    @Test
    fun `IPv4 CIDR boundary - 0`() {
        assertValid("10.0.0.0/0")
    }

    @Test
    fun `IPv4 CIDR boundary - 32`() {
        assertValid("10.0.0.1/32")
    }

    @Test
    fun `IPv4 CIDR out of range - 33`() {
        assertInvalid("10.0.0.0/33")
    }

    @Test
    fun `IPv6 CIDR boundary - 0`() {
        assertValid("::/0")
    }

    @Test
    fun `IPv6 CIDR boundary - 128`() {
        assertValid("::1/128")
    }

    @Test
    fun `IPv6 CIDR out of range - 129`() {
        assertInvalid("::1/129")
    }

    // ---- Empty / Blank Input Rejection ----

    @Test
    fun `empty string is rejected`() {
        val result = IpValidator.validate("")
        assertTrue(result is IpValidator.ValidationResult.Invalid)
        assertEquals("IP address cannot be empty", (result as IpValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun `blank string is rejected`() {
        val result = IpValidator.validate("   ")
        assertTrue(result is IpValidator.ValidationResult.Invalid)
        assertEquals("IP address cannot be empty", (result as IpValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun `whitespace-only string is rejected`() {
        val result = IpValidator.validate("\t\n ")
        assertTrue(result is IpValidator.ValidationResult.Invalid)
    }

    @Test
    fun `no dots or colons is rejected`() {
        val result = IpValidator.validate("notanip")
        assertTrue(result is IpValidator.ValidationResult.Invalid)
        assertEquals("Invalid IP address format", (result as IpValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun `valid IP with leading and trailing whitespace is accepted`() {
        assertValid("  192.168.1.1  ")
    }

    // ---- Helpers ----

    private fun assertValid(input: String) {
        val result = IpValidator.validate(input)
        assertEquals(
            "Expected '$input' to be valid but got: $result",
            IpValidator.ValidationResult.Valid,
            result
        )
    }

    private fun assertInvalid(input: String) {
        val result = IpValidator.validate(input)
        assertTrue(
            "Expected '$input' to be invalid but got Valid",
            result is IpValidator.ValidationResult.Invalid
        )
    }
}
