/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.object.bytecode.aspectwerkz.SimpleClassInfo;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaModelClassInfo extends SimpleClassInfo {
  private IType fType;
  private String fSuperClassSig;
  private ClassInfo fSuperClass;
  private String[] fInterfaceSigs;
  private ClassInfo[] fInterfaces;
  private List<AnnotationElement.Annotation> fAnnotations = new ArrayList<AnnotationElement.Annotation>();
  private final Map<IMethod, SoftReference> fMethodInfoCache = new HashMap<IMethod, SoftReference>();
  private final Map<IField, SoftReference> fFieldInfoCache = new HashMap<IField, SoftReference>();

  private static final String[] NO_INTERFACE_SIGS = new String[0];
  private static final ClassInfo[] NO_INTERFACES = new ClassInfo[0];
  
  public JavaModelClassInfo(String classname) {
    super(classname);
    fInterfaceSigs = NO_INTERFACE_SIGS;
    fInterfaces = NO_INTERFACES;
  }
  
  public JavaModelClassInfo(IType type) {
    super(type.getFullyQualifiedName('$'));
    fType = type;
    try {
      fSuperClassSig = type.getSuperclassTypeSignature();
      if(fSuperClassSig != null) {
        fSuperClass = new JavaModelClassInfo(JdtUtils.getResolvedTypeName(fSuperClassSig, type));
      }
      fInterfaceSigs = NO_INTERFACE_SIGS;
      fInterfaces = NO_INTERFACES;
      if(type.isClass()) {
        fInterfaceSigs = type.getSuperInterfaceTypeSignatures();
        fInterfaces = new JavaModelClassInfo[fInterfaceSigs.length];
        for(int i = 0; i < fInterfaceSigs.length; i++) {
          fInterfaces[i] = new JavaModelClassInfo(JdtUtils.getResolvedTypeName(fInterfaceSigs[i], type));
        }
      }
    } catch(JavaModelException jme) {
      fInterfaceSigs = NO_INTERFACE_SIGS;
      fInterfaces = NO_INTERFACES;
    }
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

  public MethodInfo getMethod(ClassInfoFactory classInfoFactory, IMethod method) throws JavaModelException {
    MethodInfo info = null;
    synchronized (fMethodInfoCache) {
      SoftReference ref = fMethodInfoCache.get(method);
      if(ref != null) {
        info = (MethodInfo) ref.get();
      }
      if (info == null) {
        info = new JavaModelMethodInfo(classInfoFactory, method);
        fMethodInfoCache.put(method, new SoftReference(info));
      }
    }
    return info;
  }
  
  public FieldInfo getField(ClassInfoFactory classInfoFactory, IField field) {
    FieldInfo info = null;
    synchronized (fFieldInfoCache) {
      SoftReference ref = fFieldInfoCache.get(field);
      if(ref != null) {
        info = (FieldInfo) ref.get();
      }
      if (info == null) {
        info = new JavaModelFieldInfo(classInfoFactory, field);
        fFieldInfoCache.put(field, new SoftReference(info));
      }
    }
    return info;
  }
  
  public void clearAnnotations() {
    fAnnotations.clear();
  }
  
  public void addAnnotation(Annotation annotation) {
    IAnnotationBinding binding = annotation.resolveAnnotationBinding();
    String name = binding.getAnnotationType().getQualifiedName();
    fAnnotations.add(new AnnotationElement.Annotation(name));
  }
  
  public AnnotationElement.Annotation[] getAnnotations() {
    return fAnnotations.toArray(new AnnotationElement.Annotation[0]);
  }
  
  public boolean isStale() {
    try {
      if(fType == null) return true;
      String superClassSig = fType.getSuperclassTypeSignature();
      if(superClassSig == null && fSuperClassSig != null ||
          superClassSig != null && fSuperClassSig == null ||
          superClassSig != null && !superClassSig.equals(fSuperClassSig)) { return true; }

      String[] interfaceSigs = fType.getSuperInterfaceTypeSignatures();
      if(interfaceSigs.length != fInterfaceSigs.length) return true;
      for(int i = 0; i < interfaceSigs.length; i++) {
        if(!interfaceSigs[i].equals(fInterfaceSigs[i])) return true;
      }
    } catch(JavaModelException jme) {/**/}
    return false;
  }
}
