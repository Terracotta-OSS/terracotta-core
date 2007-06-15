/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IType;

import com.tc.aspectwerkz.reflect.ClassInfo;

import java.util.HashMap;
import java.util.Map;

public class ClassInfoFactory extends com.tc.object.bytecode.aspectwerkz.ClassInfoFactory {
  private final Map classInfoCache = new HashMap();

  public ClassInfo getClassInfo(String className) {
    ClassInfo info;
    synchronized (classInfoCache) {
      info = (ClassInfo) classInfoCache.get(className);
      if (info == null) {
        info = new JavaModelClassInfo(className);
        classInfoCache.put(className, info);
      }
    }
    return info;
  }

  public ClassInfo getClassInfo(IType type) {
    JavaModelClassInfo info;
    synchronized (classInfoCache) {
      String className = type.getFullyQualifiedName('$');
      info = (JavaModelClassInfo) classInfoCache.get(className);
      if (info == null || info.isStale()) {
        info = new JavaModelClassInfo(type);
        classInfoCache.put(className, info);
      }
    }
    return info;
  }

}
