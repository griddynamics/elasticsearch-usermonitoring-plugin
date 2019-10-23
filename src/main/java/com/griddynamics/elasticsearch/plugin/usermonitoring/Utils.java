package com.griddynamics.elasticsearch.plugin.usermonitoring;

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.xpack.core.security.authc.Authentication;
import org.elasticsearch.xpack.core.security.user.User;

import java.lang.reflect.*;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.security.AccessController.doPrivileged;

public class Utils {
    private static final Permission[] PERMISSIONS = {
        new RuntimePermission("accessDeclaredMembers"),
        new ReflectPermission("suppressAccessChecks")
    };
    private static ConcurrentMap<FieldKey, FiledExtractor> extractorCache = new ConcurrentHashMap<>();

    private static final String AUTH_THREAD_CONTEXT_KEY = "_xpack_security_authentication";

    private static final Set<String> SYSTEM_USERS = Sets.newHashSet("_xpack", "_xpack_security"); //TODO property

    public static User extractUser(ThreadContext threadContext) {
        User user = null;
        Authentication auth = threadContext.getTransient(AUTH_THREAD_CONTEXT_KEY);
        if (auth != null && auth.getUser() != null) {
            user = auth.getUser();
        }
        return user;
    }

    public static boolean isSystemUser(User user) {
        return user != null && SYSTEM_USERS.contains(user.principal());
    }

    @SuppressWarnings("unchecked")
    public static  <T> T extract(String fieldName, Class<T> objectClass, Object extractFrom) {
        FiledExtractor extractor = extractorCache.computeIfAbsent(new FieldKey(fieldName, extractFrom.getClass()), key -> {

            Field fieldRes = doPrivileged((PrivilegedAction<Field>) () -> {
                Field field;
                try {
                    field = key.objectClass.getDeclaredField(key.fieldName);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Can not extract field '" + key.fieldName + "' from clazz " + key.objectClass.getName());
                }
                makeAccessible(field);
                return field;
            }, null, PERMISSIONS);
            return obj -> getFieldValue(fieldRes, obj);

        });
        return (T) extractor.extract(extractFrom);
    }

    private static <T extends AccessibleObject & Member> boolean isAccessible(final T member) {
        Objects.requireNonNull(member, "No member provided");
        return Modifier.isPublic(member.getModifiers()) && Modifier.isPublic(member.getDeclaringClass().getModifiers());
    }

    private static void makeAccessible(final Field field) {
        Objects.requireNonNull(field, "No field provided");
        if ((!isAccessible(field) || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    private static Object getFieldValue(final Field field, final Object instance) {
        try {
            return field.get(instance);
        } catch (final IllegalAccessException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @FunctionalInterface
    private interface FiledExtractor {
        Object extract(Object obj);
    }

    private static class FieldKey {
        final String fieldName;
        final Class<?> objectClass;

        FieldKey(String fieldName, Class<?> objectClass) {
            this.fieldName = fieldName;
            this.objectClass = objectClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FieldKey fieldKey = (FieldKey) o;
            return Objects.equals(fieldName, fieldKey.fieldName) &&
                Objects.equals(objectClass, fieldKey.objectClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldName, objectClass);
        }
    }
}
