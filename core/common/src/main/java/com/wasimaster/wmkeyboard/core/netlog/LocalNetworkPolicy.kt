package com.wasimaster.wmkeyboard.core.netlog

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Destination policy for the LAN-only edition.
 *
 * This is deliberately deny-by-default. IP literals are accepted only when
 * they identify loopback, RFC1918, IPv4 link-local, IPv6 ULA or IPv6 link-local
 * addresses. Hostnames are accepted only for localhost and mDNS .local names;
 * arbitrary DNS names are refused without attempting a lookup. That avoids
 * leaking a hostname to a public resolver and prevents a public hostname from
 * being treated as local merely because its label says "self-hosted".
 *
 * Redirects and raw socket call sites are checked again at their network
 * boundary (NetLog.callTo / NetLogInterceptor), not just at the initial URL.
 */
object LocalNetworkPolicy {

    /** Throws before a connection or DNS lookup is attempted for a non-LAN host. */
    fun checkHost(host: String) {
        val normalized = host.trim().trimEnd('.').lowercase()
        if (normalized.isEmpty()) throw blocked(host)
        if (normalized == "localhost" || normalized.endsWith(".localhost") ||
            normalized.endsWith(".local")
        ) return

        val ipv4 = parseIpv4(normalized)
        if (ipv4 != null) {
            if (isLocalIpv4(ipv4)) return
            throw blocked(host)
        }

        // A colon is required before invoking the parser, so this cannot cause
        // a DNS lookup for an arbitrary hostname.
        if (':' in normalized) {
            val address = runCatching { InetAddress.getByName(normalized) }.getOrNull()
            if (address != null && isLocalIpv6(address.address)) return
        }

        throw blocked(host)
    }

    /** Checks an already-resolved peer without performing another name lookup. */
    fun checkAddress(address: InetAddress) {
        val allowed = when (address) {
            is Inet4Address -> isLocalIpv4(address.address.map { it.toInt() and 0xff }.toIntArray()) ||
                address.address.contentEquals(byteArrayOf(-1, -1, -1, -1))
            is Inet6Address -> isLocalIpv6(address.address)
            else -> false
        }
        if (!allowed) throw blocked(address.hostAddress.orEmpty())
    }

    private fun parseIpv4(host: String): IntArray? {
        val parts = host.split('.')
        if (parts.size != 4) return null
        val octets = parts.map { part ->
            if (part.isEmpty() || part.length > 3 || part.any { !it.isDigit() }) return null
            // Reject alternate/octal-looking spellings.
            if (part.length > 1 && part.startsWith('0')) return null
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
        }
        return octets.toIntArray()
    }

    private fun isLocalIpv4(ip: IntArray): Boolean {
        val a = ip[0]
        val b = ip[1]
        return a == 10 ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 169 && b == 254) ||
            a == 127
    }

    private fun isLocalIpv6(bytes: ByteArray): Boolean {
        if (bytes.size != 16) return false
        val loopback = bytes.take(15).all { it == 0.toByte() } && bytes[15] == 1.toByte()
        if (loopback) return true
        // fc00::/7 (unique-local) and fe80::/10 (link-local).
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        return (first and 0xfe) == 0xfc || (first == 0xfe && (second and 0xc0) == 0x80)
    }

    private fun blocked(host: String) =
        UnknownHostException("LAN-only build blocked non-local destination: $host")
}
