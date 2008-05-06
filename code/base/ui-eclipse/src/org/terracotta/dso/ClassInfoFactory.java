/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class ClassInfoFactory extends com.tc.object.bytecode.aspectwerkz.ClassInfoFactory {
  private final Map<String, SoftReference> classInfoCache = new HashMap<String, SoftReference>();

  public ClassInfo getClassInfo(String className) {
    ClassInfo info = null;
    synchronized (classInfoCache) {
      SoftReference ref = classInfoCache.get(className);
      if(ref != null) {
        info = (JavaModelClassInfo) ref.get();
      }
      if (info == null) {
        info = new JavaModelClassInfo(className);
        classInfoCache.put(className, new SoftReference(info));
      }
    }
    return info;
  }

  public ClassInfo getClassInfo(IType type) {
    JavaModelClassInfo info = null;
    synchronized (classInfoCache) {
      String className = type.getFullyQualifiedName('$');
      SoftReference ref = classInfoCache.get(className);
      if(ref != null) {
        info = (JavaModelClassInfo) ref.get();
      }
      if (info == null || info.isStale()) {
        info = new JavaModelClassInfo(type);
        classInfoCache.put(className, new SoftReference(info));
      }
    }
    return info;
  }

  public MethodInfo getMethodInfo(IMethod method) throws JavaModelException {
    JavaModelClassInfo classInfo = (JavaModelClassInfo)getClassInfo(method.getDeclaringType());
    return classInfo.getMethod(this, method);
  }

  public FieldInfo getFieldInfo(IField field) {
    JavaModelClassInfo classInfo = (JavaModelClassInfo)getClassInfo(field.getDeclaringType());
    return classInfo.getField(this, field);
  }
}
