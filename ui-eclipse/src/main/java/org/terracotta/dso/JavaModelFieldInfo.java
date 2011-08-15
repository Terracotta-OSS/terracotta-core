/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.exception.ImplementMe;

public class JavaModelFieldInfo implements FieldInfo {
  private ClassInformationFactory        fClassInfoFactory;
  private IField                  fField;
  private JavaModelAnnotationInfo fAnnotationInfo;
  private String                  fTypeName;
  private IType                   fType;
  private ClassInfo               fClassInfo;

  public JavaModelFieldInfo(ClassInformationFactory classInfoFactory, IField field) {
    fClassInfoFactory = classInfoFactory;
    fField = field;
    fAnnotationInfo = new JavaModelAnnotationInfo(field);
  }

  public ClassInfo getType() {
    if (fClassInfo == null) {
      IType type = getFieldType();
      if (type != null) {
        fClassInfo = fClassInfoFactory.getClassInfo(type);
      } else {
        fClassInfo = fClassInfoFactory.getClassInfo(getTypeName());
      }
    }
    return fClassInfo;
  }

  private String getTypeName() {
    if (fTypeName == null) {
      fTypeName = resolveTypeName();
    }
    return fTypeName;
  }

  private String resolveTypeName() {
    try {
      String sig = fField.getTypeSignature();
      IType declaringType = fField.getDeclaringType();
      return JdtUtils.getResolvedTypeName(sig, declaringType);
    } catch (JavaModelException jme) {
      return "java.lang.Object";
    }
  }

  private IType getFieldType() {
    if (fType == null) {
      fType = determineFieldType();
    }
    return fType;
  }

  public IType determineFieldType() {
    try {
      String typeName = getTypeName();
      IJavaProject javaProject = fField.getJavaProject();
      if (typeName != null) { return JdtUtils.findType(javaProject, typeName); }
    } catch (JavaModelException jme) {
      /**/
    }
    return null;
  }

  public ClassInfo getDeclaringType() {
    return fClassInfoFactory.getClassInfo(fField.getDeclaringType());
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

  public String getGenericsSignature() {
    throw new ImplementMe();
  }

  public int getModifiers() {
    throw new ImplementMe();
  }

  public String getName() {
    return fField.getElementName();
  }

  public String getSignature() {
    throw new ImplementMe();
  }
}
