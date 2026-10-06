package com.itmonteur.hospitalerp.audit.internal;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientAddressTest {

    private static MockHttpServletRequest request(String remoteAddr, String realIp) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (realIp != null) {
            request.addHeader(ClientAddress.REAL_IP_HEADER, realIp);
        }
        return request;
    }

    @Test
    void directCallerIsRecordedAsItIs() {
        assertThat(ClientAddress.of(request("203.0.113.7", null))).isEqualTo("203.0.113.7");
    }

    @Test
    void behindTheLocalProxyTheRealClientIsRecorded() {
        // Docker network (nginx), loopback (local proxy), IPv6 loopback and unique-local
        assertThat(ClientAddress.of(request("172.18.0.5", "203.0.113.7"))).isEqualTo("203.0.113.7");
        assertThat(ClientAddress.of(request("127.0.0.1", " 198.51.100.2 "))).isEqualTo("198.51.100.2");
        assertThat(ClientAddress.of(request("0:0:0:0:0:0:0:1", "198.51.100.2"))).isEqualTo("198.51.100.2");
        assertThat(ClientAddress.of(request("fd00::5", "198.51.100.2"))).isEqualTo("198.51.100.2");
    }

    @Test
    void aCallerOnTheInternetCannotFakeItsAddress() {
        assertThat(ClientAddress.of(request("203.0.113.7", "10.0.0.1"))).isEqualTo("203.0.113.7");
        assertThat(ClientAddress.of(request("2001:db8::1", "10.0.0.1"))).isEqualTo("2001:db8::1");
    }

    @Test
    void onlyIpLiteralsAreChecked() {
        // a host name is never looked up in DNS
        assertThat(ClientAddress.isPrivateOrLocal("localhost")).isFalse();
        assertThat(ClientAddress.isPrivateOrLocal(null)).isFalse();
    }

    @Test
    void longValuesAreCut() {
        assertThat(ClientAddress.of(request("10.0.0.1", "x".repeat(100)))).hasSize(45);
    }

    @Test
    void outsideARequestThereIsNoAddress() {
        assertThat(ClientAddress.current()).isNull();
    }
}
