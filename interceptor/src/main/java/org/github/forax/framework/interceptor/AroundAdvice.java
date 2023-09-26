package org.github.forax.framework.interceptor;

import java.lang.reflect.Method;
import java.util.List;

public interface AroundAdvice {
  void before(Object instance, Method method, Object[] args) throws Throwable;
  void after(Object instance, Method method, Object[] args, Object result) throws Throwable;


}
