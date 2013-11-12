/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;

import com.terracotta.toolkit.collections.map.ToolkitCacheImplInterface;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class LocalMethodUtil {
  private static final Map<ToolkitObjectType, Set<String>> localMethods = new HashMap<ToolkitObjectType, Set<String>>();
  static {
    Set<String> cacheLocalMethodSet = new HashSet<String>();
    cacheLocalMethodSet.add("unsafeLocalGet");
    cacheLocalMethodSet.add("containsLocalKey");
    cacheLocalMethodSet.add("localSize");
    cacheLocalMethodSet.add("localKeySet");
    cacheLocalMethodSet.add("localOnHeapSizeInBytes");
    cacheLocalMethodSet.add("localOffHeapSizeInBytes");
    cacheLocalMethodSet.add("localOnHeapSize");
    cacheLocalMethodSet.add("localOffHeapSize");
    cacheLocalMethodSet.add("containsKeyLocalOnHeap");
    cacheLocalMethodSet.add("containsKeyLocalOffHeap");
    cacheLocalMethodSet.add("disposeLocally");
    validateMethodNamesExist(ToolkitCacheImplInterface.class, cacheLocalMethodSet);
    localMethods.put(ToolkitObjectType.CACHE, cacheLocalMethodSet);
    localMethods.put(ToolkitObjectType.STORE, cacheLocalMethodSet);
  }

  static boolean isLocal(ToolkitObjectType objectType, String methodName) {
    Set<String> set = localMethods.get(objectType);
    if (set == null) { return false; }
    return set.contains(methodName);
  }

  private static void validateMethodNamesExist(Class klazz, Set<String> methodToCheck) {
    for (String methodName : methodToCheck) {
      if (!exist(klazz, methodName)) { throw new AssertionError("Method " + methodName + " does not exist in class "
                                                                + klazz.getName()); }
    }
  }

  private static boolean exist(Class klazz, String method) {
    Method[] methods = klazz.getMethods();
    for (Method m : methods) {
      if (m.getName().equals(method)) { return true; }
    }
    return false;
  }

}
