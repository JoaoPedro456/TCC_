package com.tcc.backend_TCC.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

/**
 * Utilitário seguro para extração e sanitização do IP do cliente.
 * Previne IP spoofing, Header injection e caracteres maliciosos.
 */
public final class IpUtil {

    private static final Pattern IP_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    // Configuração de proxies confiáveis adicionais via variável de ambiente
    private static final Set<String> CONFIGURED_PROXIES;

    static {
        Set<String> proxies = new HashSet<>();
        proxies.add("127.0.0.1");
        proxies.add("::1");
        proxies.add("0:0:0:0:0:0:0:1");

        String envProxies = System.getenv("TRUSTED_PROXIES");
        if (envProxies != null && !envProxies.isBlank()) {
            Arrays.stream(envProxies.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(proxies::add);
        }
        CONFIGURED_PROXIES = Collections.unmodifiableSet(proxies);
    }

    private IpUtil() {}

    /**
     * Retrieves the client IP address in a secure manner.
     * <p>
     * If the request originates from a trusted proxy (loopback, private container networks,
     * or defined in {@code TRUSTED_PROXIES}), the method will honor the {@code X-Forwarded-For}
     * header and return the right‑most valid IP (the original client before trusted hops).
     * Otherwise the header is ignored and {@code request.getRemoteAddr()} is used.
     * This prevents attackers from spoofing the header when the request does not pass through a known proxy.
     * </p>
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String remoteAddr = request.getRemoteAddr();
        boolean trusted = isTrustedProxy(remoteAddr);
        String ip = null;

        if (trusted) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isBlank()) {
                String[] parts = xfHeader.split(",");
                for (int i = parts.length - 1; i >= 0; i--) {
                    String candidate = parts[i].trim();
                    if (isValid(candidate)) {
                        ip = candidate;
                        break;
                    }
                }
            }
        }

        // Fallback to remote address if we did not obtain a valid IP from the header.
        if (ip == null || ip.isBlank() || !isValid(ip)) {
            ip = remoteAddr;
        }

        if (ip == null || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }

    private static boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        if (CONFIGURED_PROXIES.contains(remoteAddr)) {
            return true;
        }
        // Ambientes em nuvem / containers (Render, Docker, AWS, Kubernetes):
        // Conexões internas do roteador de borda chegam através de redes privadas RFC 1918:
        // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        if (remoteAddr.startsWith("10.") || remoteAddr.startsWith("192.168.")) {
            return true;
        }
        if (remoteAddr.startsWith("172.")) {
            try {
                String[] parts = remoteAddr.split("\\.");
                if (parts.length >= 2) {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static boolean isValid(String ip) {
        return ip != null && ip.length() >= 7 && ip.length() <= 45 && IP_PATTERN.matcher(ip).matches();
    }

    /**
     * Sanitiza strings que serão incluídas em logs para prevenir Log Injection (CRLF).
     */
    public static String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n]", "_").trim();
    }
}
