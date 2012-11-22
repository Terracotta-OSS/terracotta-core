/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

class NoOpInvocationHandler implements InvocationHandler {

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // no-op and return default for primitive types and null for Objects.
    if (method.getReturnType().isPrimitive()) {
      if (method.getReturnType() == Byte.TYPE) {
        return 0;
      } else if (method.getReturnType() == Short.TYPE) {
        return 0;
      } else if (method.getReturnType() == Integer.TYPE) {
        return 0;
      } else if (method.getReturnType() == Long.TYPE) {
        return 0L;
      } else if (method.getReturnType() == Float.TYPE) {
        return 0.0f;
      } else if (method.getReturnType() == Double.TYPE) {
        return 0.0d;
      } else if (method.getReturnType() == Character.TYPE) {
        return '\u0000';
      } else if (method.getReturnType() == Boolean.TYPE) { return false; }
    } else if (Map.class.isAssignableFrom(method.getReturnType())) {
      return Collections.EMPTY_MAP;
    } else if (List.class.isAssignableFrom(method.getReturnType())) {
      return Collections.EMPTY_LIST;
    } else if (Set.class.isAssignableFrom(method.getReturnType())) { return Collections.EMPTY_SET; }
    return null;
  }
}