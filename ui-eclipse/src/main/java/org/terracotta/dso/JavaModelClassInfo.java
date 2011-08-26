/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.object.bytecode.aspectwerkz.SimpleClassInfo;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class JavaModelClassInfo extends SimpleClassInfo {
  private IType                             fType;
  private String                            fSuperClassSig;
  private ClassInfo                         fSuperClass;
  private String[]                          fInterfaceSigs;
  private ClassInfo[]                       fInterfaces;
  private JavaModelAnnotationInfo           fAnnotationInfo;
  private final Map<IMethod, SoftReference> fMethodInfoCache  = new HashMap<IMethod, SoftReference>();
  private final Map<String, SoftReference>  fFieldInfoCache   = new HashMap<String, SoftReference>();

  private static final String[]             NO_INTERFACE_SIGS = new String[0];
  private static final ClassInfo[]          NO_INTERFACES     = new ClassInfo[0];

  public JavaModelClassInfo(String classname) {
    super(classname);
    fInterfaceSigs = NO_INTERFACE_SIGS;
    fInterfaces = NO_INTERFACES;
    fAnnotationInfo = new JavaModelAnnotationInfo();
  }

  public JavaModelClassInfo(IType type) {
    super(type.getFullyQualifiedName('$'));
    fType = type;
    try {
      fSuperClassSig = type.getSuperclassTypeSignature();
      if (fSuperClassSig != null) {
        fSuperClass = new JavaModelClassInfo(JdtUtils.getResolvedTypeName(fSuperClassSig, type));
      }
      fInterfaceSigs = NO_INTERFACE_SIGS;
      fInterfaces = NO_INTERFACES;
      if (type.isClass() || type.isAnnotation() || type.isInterface()) {
        fInterfaceSigs = type.getSuperInterfaceTypeSignatures();
        fInterfaces = new JavaModelClassInfo[fInterfaceSigs.length];
        for (int i = 0; i < fInterfaceSigs.length; i++) {
          fInterfaces[i] = new JavaModelClassInfo(JdtUtils.getResolvedTypeName(fInterfaceSigs[i], type));
        }
      }
    } catch (JavaModelException jme) {
      fInterfaceSigs = NO_INTERFACE_SIGS;
      fInterfaces = NO_INTERFACES;
    }
    fAnnotationInfo = new JavaModelAnnotationInfo(type);
  }

  public IType getType() {
    return fType;
  }

  public ClassInfo getSuperclass() {
    return fSuperClass;
  }

  public ClassInfo[] getInterfaces() {
    return fInterfaces.clone();
  }

  public MethodInfo getMethod(ClassInformationFactory classInfoFactory, IMethod method) throws JavaModelException {
    MethodInfo info = null;
    synchronized (fMethodInfoCache) {
      SoftReference ref = fMethodInfoCache.get(method);
      if (ref != null) {
        info = (MethodInfo) ref.get();
      }
      if (info == null) {
        info = new JavaModelMethodInfo(classInfoFactory, method);
        fMethodInfoCache.put(method, new SoftReference(info));
      }
    }
    return info;
  }

  public FieldInfo getField(ClassInformationFactory classInfoFactory, IField field) {
    FieldInfo info = null;
    synchronized (fFieldInfoCache) {
      String key = field.getElementName();
      SoftReference ref = fFieldInfoCache.get(key);
      if (ref != null) {
        info = (FieldInfo) ref.get();
      }
      if (info == null) {
        info = new JavaModelFieldInfo(classInfoFactory, field);
        fFieldInfoCache.put(key, new SoftReference(info));
      }
    }
    return info;
  }

  public void clearAnnotations() {
    fAnnotationInfo.clear();
  }

  public void addAnnotation(Annotation annotation) {
    fAnnotationInfo.addAnnotation(annotation);
  }

  public void addAnnotation(String fqcn) {
    fAnnotationInfo.addAnnotation(fqcn);
  }

  public AnnotationElement.Annotation[] getAnnotations() {
    return fAnnotationInfo.getAnnotations();
  }

  public boolean isStale(IType type) {
    try {
      if (fType == null) { return true; }
      if (fType == type) { return false; }
      String superClassSig = fType.getSuperclassTypeSignature();
      if (superClassSig == null && fSuperClassSig != null || superClassSig != null && fSuperClassSig == null
          || superClassSig != null && !superClassSig.equals(fSuperClassSig)) { return true; }

      String[] interfaceSigs = fType.getSuperInterfaceTypeSignatures();
      if (interfaceSigs.length != fInterfaceSigs.length) { return true; }
      for (int i = 0; i < interfaceSigs.length; i++) {
        if (!interfaceSigs[i].equals(fInterfaceSigs[i])) { return true; }
      }
    } catch (JavaModelException jme) {/**/
    }
    return false;
  }
}
