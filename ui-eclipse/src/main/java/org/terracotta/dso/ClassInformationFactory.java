/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class ClassInformationFactory extends com.tc.object.bytecode.aspectwerkz.ClassInfoFactory {
  private final Map<String, SoftReference> fClassInfoCache = new HashMap<String, SoftReference>();

  private static final ClassInfo           intClassInfo    = JavaClassInfo.getClassInfo(Integer.TYPE);
  private static final ClassInfo           doubleClassInfo = JavaClassInfo.getClassInfo(Double.TYPE);
  private static final ClassInfo           floatClassInfo  = JavaClassInfo.getClassInfo(Float.TYPE);
  private static final ClassInfo           longClassInfo   = JavaClassInfo.getClassInfo(Long.TYPE);
  private static final ClassInfo           charClassInfo   = JavaClassInfo.getClassInfo(Character.TYPE);
  private static final ClassInfo           byteClassInfo   = JavaClassInfo.getClassInfo(Byte.TYPE);

  public ClassInformationFactory() {
    super();
    fClassInfoCache.put("int", new SoftReference(intClassInfo));
    fClassInfoCache.put("double", new SoftReference(doubleClassInfo));
    fClassInfoCache.put("float", new SoftReference(floatClassInfo));
    fClassInfoCache.put("long", new SoftReference(longClassInfo));
    fClassInfoCache.put("char", new SoftReference(charClassInfo));
    fClassInfoCache.put("byte", new SoftReference(byteClassInfo));
  }

  @Override
  public ClassInfo getClassInfo(String className) {
    ClassInfo info = null;
    synchronized (fClassInfoCache) {
      SoftReference ref = fClassInfoCache.get(className);
      if (ref != null) {
        info = (ClassInfo) ref.get();
      }
      if (info == null) {
        info = new JavaModelClassInfo(className);
        fClassInfoCache.put(className, new SoftReference(info));
      }
    }
    return info;
  }

  public ClassInfo getClassInfo(IType type) {
    if (type == null) return null;
    JavaModelClassInfo info = null;
    synchronized (fClassInfoCache) {
      String className = type.getFullyQualifiedName('$');
      SoftReference ref = fClassInfoCache.get(className);
      if (ref != null) {
        info = (JavaModelClassInfo) ref.get();
      }
      if (info == null || info.isStale(type)) {
        info = new JavaModelClassInfo(type);
        fClassInfoCache.put(className, new SoftReference(info));
      }
    }
    return info;
  }

  public void clear(IType type) {
    if (type != null) {
      clear(type.getFullyQualifiedName('$'));
    }
  }

  public void clear(String className) {
    fClassInfoCache.remove(className);
  }

  public MethodInfo getMethodInfo(IMethod method) throws JavaModelException {
    JavaModelClassInfo classInfo = (JavaModelClassInfo) getClassInfo(method.getDeclaringType());
    return classInfo.getMethod(this, method);
  }

  public FieldInfo getFieldInfo(IField field) {
    JavaModelClassInfo classInfo = (JavaModelClassInfo) getClassInfo(field.getDeclaringType());
    return classInfo.getField(this, field);
  }
}
