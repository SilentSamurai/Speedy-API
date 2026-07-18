package com.github.silent.samurai.speedy.conversion.walker.java;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Small JDK-only replacement for the subset of Spring BeanWrapper used by the
 * Java/Speedy conversion walkers.
 */
final class JavaBeanAccessor {

    private static final ClassValue<Map<String, PropertyDescriptor>> PROPERTY_CACHE = new ClassValue<>() {
        @Override
        protected Map<String, PropertyDescriptor> computeValue(Class<?> type) {
            try {
                return Arrays.stream(Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors())
                        .collect(Collectors.toUnmodifiableMap(PropertyDescriptor::getName, Function.identity()));
            } catch (IntrospectionException e) {
                throw new IllegalStateException("Cannot inspect JavaBean " + type.getName(), e);
            }
        }
    };

    private final Object bean;
    private final Map<String, PropertyDescriptor> properties;

    private JavaBeanAccessor(Object bean) {
        this.bean = bean;
        this.properties = PROPERTY_CACHE.get(bean.getClass());
    }

    static JavaBeanAccessor forBean(Object bean) {
        return new JavaBeanAccessor(bean);
    }

    boolean isReadableProperty(String name) {
        PropertyDescriptor property = properties.get(name);
        return property != null && property.getReadMethod() != null;
    }

    boolean isWritableProperty(String name) {
        PropertyDescriptor property = properties.get(name);
        return property != null && property.getWriteMethod() != null;
    }

    Object getPropertyValue(String name) throws ReflectiveOperationException {
        return invoke(method(name, true));
    }

    void setPropertyValue(String name, Object value) throws ReflectiveOperationException {
        invoke(method(name, false), value);
    }

    private Method method(String name, boolean read) throws NoSuchMethodException {
        PropertyDescriptor property = properties.get(name);
        Method method = property == null ? null : read ? property.getReadMethod() : property.getWriteMethod();
        if (method == null) {
            throw new NoSuchMethodException("Property '" + name + "' is not " + (read ? "readable" : "writable"));
        }
        return method;
    }

    private Object invoke(Method method, Object... arguments) throws ReflectiveOperationException {
        try {
            if (!method.canAccess(bean) && !method.trySetAccessible()) {
                throw new IllegalAccessException("Cannot access JavaBean method " + method);
            }
            return method.invoke(bean, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException reflectiveFailure) {
                throw reflectiveFailure;
            }
            throw e;
        }
    }
}
