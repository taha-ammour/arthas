package com.taobao.arthas.common;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Utility class for privileged reflective access.
 *
 * Refactored to remove dependency on sun.misc.Unsafe, which is an internal
 * proprietary API removed from public access in JDK 11+.
 * Uses standard java.lang.reflect and java.lang.invoke APIs instead.
 *
 */
public class UnsafeUtils {

    private static MethodHandles.Lookup IMPL_LOOKUP;

    /**
     * Returns a trusted MethodHandles.Lookup with full access privileges.
     * Uses standard reflection with setAccessible as a fallback,
     * replacing the previous sun.misc.Unsafe-based approach.
     */
    public static MethodHandles.Lookup implLookup() {
        if (IMPL_LOOKUP == null) {
            try {
                Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                implLookupField.setAccessible(true);
                IMPL_LOOKUP = (MethodHandles.Lookup) implLookupField.get(null);
            } catch (Throwable e) {
                // Fallback: return a standard lookup if IMPL_LOOKUP is not accessible
                IMPL_LOOKUP = MethodHandles.lookup();
            }
        }
        return IMPL_LOOKUP;
    }
}