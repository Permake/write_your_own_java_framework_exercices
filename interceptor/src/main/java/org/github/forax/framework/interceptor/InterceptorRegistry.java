package org.github.forax.framework.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Stream;

public final class InterceptorRegistry {
//  private final HashMap<Class<? extends Annotation>, List<AroundAdvice>> annotationMap
//          = new HashMap<>();
  private final HashMap<Class<? extends Annotation>, List<Interceptor>> interceptorMap
    = new HashMap<>();

  private final HashMap<Method, Invocation> CACHE = new HashMap<>();

  private Invocation computeInvocation(Method method) {
      return CACHE.computeIfAbsent(method, m ->
              getInvocation(findInterceptors(method)));
  };



  public void addAroundAdvice(Class<? extends Annotation> annotationClass, AroundAdvice aroundAdvice) {
//    Objects.requireNonNull(aroundAdvice);
//    Objects.requireNonNull(annotationClass);
//    annotationMap.computeIfAbsent(annotationClass, __ ->
//            new ArrayList<>()).add(aroundAdvice);
      addInterceptor(annotationClass, (instance, method, args, invocation) -> {
          aroundAdvice.before(instance, method, args);
          Object value = null;
          try {
              return value = invocation.proceed(instance, method, args);
          } finally {
              aroundAdvice.after(instance, method, args, value);
          }
      });
  }

//  List<AroundAdvice> findAdvices(Method method) {
//      return Arrays.stream(method.getAnnotations()).flatMap(annotation ->
//              annotationMap.getOrDefault(annotation.annotationType(), List.of()).stream()).toList();
//  }

  public void addInterceptor(Class<? extends Annotation> annotationClass, Interceptor interceptor) {
      Objects.requireNonNull(annotationClass);
      Objects.requireNonNull(interceptor);
      interceptorMap.computeIfAbsent(annotationClass, __ -> new ArrayList<>()).add(interceptor);
      CACHE.clear(); // invalidate cache
  }
    List<Interceptor> findInterceptors(Method method) {
        return Stream.of(Arrays.stream(method.getDeclaringClass().getAnnotations()),
                        Arrays.stream(method.getAnnotations()),
                        Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream))
                .flatMap(s -> s)
                .flatMap(interceptor ->
                interceptorMap.getOrDefault(interceptor.annotationType(), List.of()).stream()).toList();
    }

    static Invocation getInvocation(List<Interceptor> interceptorList) {
      Invocation invocation = Utils::invokeMethod;
      for (var interceptor: interceptorList.reversed()) {
          var tmp = invocation;
          invocation = (instance, method, args) ->
                  interceptor.intercept(instance, method, args, tmp);
      }
      return invocation;
    }

  public <T> T createProxy(Class<T> type, T delegate) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(delegate);
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
            new Class<?>[] {type},
            (Object __, Method method, Object[] args) -> {
//                var interceptors = findInterceptor(method);
//                var invocation = getInvocation(interceptors);
                var invocation = computeInvocation(method);
                return invocation.proceed(delegate, method, args);
//                var advices = findAdvices(method);
//                for (var advice : advices) {
//                    advice.before(delegate, method, args);
//                }
//                Object value = null;
//                try {
//                    value = Utils.invokeMethod(delegate, method, args);
//                    return type.cast(value);
//                }
//                finally {
//                    for (var advice : advices.reversed()) {
//                        advice.after(delegate, method, args, value);
//                    }
//                }
            }));
  }

}
