package com.azedcods.home_buddy_v2.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

public final class AuthUserId {

    private AuthUserId() {}

    public static Long requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }

        Object principal = auth.getPrincipal();
        if (principal == null) throw new IllegalStateException("Missing principal");

        // Common cases
        if (principal instanceof Long l) return l;
        if (principal instanceof Integer i) return i.longValue();
        if (principal instanceof String s) {
            try { return Long.parseLong(s); } catch (Exception ignored) {}
        }

        // Reflection fallback: getId() or getUserId()
        Long reflected = tryReadLong(principal, "getId");
        if (reflected != null) return reflected;

        reflected = tryReadLong(principal, "getUserId");
        if (reflected != null) return reflected;

        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
    }

    private static Long tryReadLong(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object val = m.invoke(obj);
            if (val instanceof Long l) return l;
            if (val instanceof Integer i) return i.longValue();
            if (val instanceof String s) {
                try { return Long.parseLong(s); } catch (Exception ignored) {}
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
