package com.tcc.backend_TCC.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IpUtil#getClientIp(HttpServletRequest)}.
 */
public class IpUtilTest {

    @Test
    @DisplayName("Trusted proxy returns right‑most valid IP from X-Forwarded-For header")
    void testTrustedProxyHeaderParsing() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // Remote address is a trusted proxy (127.0.0.1 as defined in IpUtil)
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        // Header contains two IPs; the method should pick the right‑most valid one.
        Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 198.51.100.7");

        String clientIp = IpUtil.getClientIp(request);
        assertEquals("198.51.100.7", clientIp);
    }

    @Test
    @DisplayName("Untrusted source ignores X-Forwarded-For and uses remote address")
    void testUntrustedSourceIgnoresHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // Remote address not in trusted list
        Mockito.when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        // Header present but should be ignored
        Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        String clientIp = IpUtil.getClientIp(request);
        assertEquals("203.0.113.5", clientIp);
    }
}
