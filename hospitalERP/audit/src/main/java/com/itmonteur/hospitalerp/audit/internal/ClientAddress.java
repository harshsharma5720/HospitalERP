package com.itmonteur.hospitalerp.audit.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * The client's IP address for an audit entry. Behind the Docker nginx, the request's own address is nginx;
 * nginx passes the real client in {@code X-Real-IP}. That header is only believed when the request comes
 * from a private or local address, so a caller on the internet can't fake it.
 */
public final class ClientAddress {

    static final String REAL_IP_HEADER = "X-Real-IP";
    private static final int MAX_LENGTH = 45; // longest IPv6 text form
    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private ClientAddress() {
    }

    /** The current request's client address, or null outside a web request (e.g. a scheduled job). */
    public static String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return of(attributes.getRequest());
        }
        return null;
    }

    static String of(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String realIp = request.getHeader(REAL_IP_HEADER);
        if (realIp != null && !realIp.isBlank() && isPrivateOrLocal(remote)) {
            return cut(realIp.trim());
        }
        return cut(remote);
    }

    static boolean isPrivateOrLocal(String address) {
        // Only IP literals: InetAddress.getByName would look up a host name in DNS
        if (address == null || !(IPV4.matcher(address).matches() || address.contains(":"))) {
            return false;
        }
        try {
            InetAddress inet = InetAddress.getByName(address);
            return inet.isLoopbackAddress() || inet.isSiteLocalAddress() || inet.isLinkLocalAddress()
                    || (inet instanceof Inet6Address && (inet.getAddress()[0] & 0xfe) == 0xfc); // fc00::/7
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static String cut(String value) {
        return value == null || value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}
