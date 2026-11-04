package com.wasimaster.wmkeyboard.netlog

import com.wasimaster.wmkeyboard.core.netlog.LocalNetworkPolicy
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.UnknownHostException

class LocalNetworkPolicyTest {

    @Test
    fun allowsPrivateIpv4AndLoopback() {
        listOf(
            "10.0.0.2",
            "172.16.0.1",
            "172.31.255.254",
            "192.168.1.10",
            "169.254.1.2",
            "127.0.0.1",
        ).forEach { LocalNetworkPolicy.checkHost(it) }
    }

    @Test
    fun allowsLocalhostAndValidatedMdnsNames() {
        LocalNetworkPolicy.checkHost("localhost")
        LocalNetworkPolicy.checkHost("device.localhost")
        LocalNetworkPolicy.checkHost("printer.local") {
            arrayOf(java.net.InetAddress.getByName("192.168.1.25"))
        }
    }

    @Test
    fun blocksMdnsNamesThatResolveOutsideTheLan() {
        assertThrows(UnknownHostException::class.java) {
            LocalNetworkPolicy.checkHost("spoofed.local") {
                arrayOf(java.net.InetAddress.getByName("203.0.113.5"))
            }
        }
    }

    @Test
    fun allowsPrivateIpv6() {
        listOf("::1", "fd12:3456:789a::1", "fe80::1234")
            .forEach(LocalNetworkPolicy::checkHost)
    }

    @Test
    fun blocksPublicIpv4AndIpv6() {
        listOf("1.1.1.1", "8.8.8.8", "172.15.0.1", "192.0.2.1", "2001:4860:4860::8888")
            .forEach { host -> assertThrows(UnknownHostException::class.java) { LocalNetworkPolicy.checkHost(host) } }
    }

    @Test
    fun blocksArbitraryDnsNamesWithoutResolvingThem() {
        listOf("example.com", "api.openai.com", "myserver.lan", "public.example.localdomain")
            .forEach { host -> assertThrows(UnknownHostException::class.java) { LocalNetworkPolicy.checkHost(host) } }
    }
}
