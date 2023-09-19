package com.github.forax.framework.injector;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

public final class InjectorRegistry {
    private final HashMap<Class<?>, Supplier<?>> registry = new HashMap<>();

    public InjectorRegistry() {
    }
  public <T>void registerInstance(Class<T> type, T instance) {
      Objects.requireNonNull(type);
      Objects.requireNonNull(instance);
      registerProvider(type, () -> instance);
  }

    public <T> void registerProvider(Class<T> type, Supplier<T> supplier) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(supplier);
        var existing = registry.putIfAbsent(type, supplier);
        if (existing != null) {
            throw new IllegalStateException("They're is already a value in the register for the type : " + type.getName());
        }
    }

  public <T> T lookupInstance(Class<T> type) {
    Objects.requireNonNull(type);
    var instance = registry.get(type);
    if (instance == null) {
        throw new IllegalStateException("The type " + type.getName() + " doesn't have an associated instance in the register");
    }
    return type.cast(instance.get());
  }

  static List<PropertyDescriptor> findInjectableProperties(Class<?> type) {
    Objects.requireNonNull(type);
    return Arrays.stream(Utils.beanInfo(type)
      .getPropertyDescriptors())
            .filter(propertyDescriptor -> {
                var setter = propertyDescriptor.getWriteMethod();
                return setter != null && setter.isAnnotationPresent(Inject.class);
            })
 /*     .filter(propertyDescriptor -> !Objects.isNull(
        propertyDescriptor.getWriteMethod())
        && propertyDescriptor.getWriteMethod()
        .isAnnotationPresent(Inject.class))*/
      .toList();
  }

  private Optional<Constructor<?>> findInjectableConstructor (Class<?> type) {
        var constructors = Arrays.stream(type.getConstructors())
          .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
          .toList();
        return switch (constructors.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(constructors.get(0));
            default -> throw new IllegalStateException("Too many injectable constructors " + type.getName());
        };
  }
  public <T> void registerProviderClass(Class<T> type, Class<? extends T> providerClass) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(providerClass);
    var constructor = findInjectableConstructor(providerClass)
            .orElseGet(() -> Utils.defaultConstructor(providerClass));
    var properties = findInjectableProperties(providerClass);
    registerProvider(type, () -> {
      var arguments = Arrays.stream(constructor.getParameterTypes())
              .map(this::lookupInstance).toArray();
      var instance = Utils.newInstance(constructor, arguments);
      for (var property : properties) {
        Utils.invokeMethod(instance, property.getWriteMethod(), lookupInstance(property.getPropertyType()));
      }
      return providerClass.cast(instance);
    });
  }

  public <T> void registerProviderClass(Class<T> providerClass) {
        Objects.requireNonNull(providerClass);
        registerProviderClass(providerClass, providerClass);
  }
}