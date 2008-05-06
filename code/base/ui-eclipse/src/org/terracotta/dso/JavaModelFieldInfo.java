/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.exception.ImplementMe;

import java.util.ArrayList;
import java.util.List;

public class JavaModelFieldInfo implements FieldInfo {
  private ClassInfoFactory fClassInfoFactory;
  private IField fField;
  private List<AnnotationElement.Annotation> fAnnotations = new ArrayList<AnnotationElement.Annotation>();
  
  public JavaModelFieldInfo(ClassInfoFactory classInfoFactory, IField field) {
    fClassInfoFactory = classInfoFactory;
    fField = field;
  }

  public ClassInfo getType() {
    return fClassInfoFactory.getClassInfo(determineFieldType());
  }

  public IType determineFieldType() {
    try {
      String sig = fField.getTypeSignature();
      IType declaringType = fField.getDeclaringType();
      String typeName = JdtUtils.getResolvedTypeName(sig, declaringType);
      IJavaProject javaProject = fField.getJavaProject();
      
      if (typeName != null) { return JdtUtils.findType(javaProject, typeName); }
    } catch (JavaModelException jme) {/**/
    }

    return null;
  }

  public ClassInfo getDeclaringType() {
    return fClassInfoFactory.getClassInfo(fField.getDeclaringType());
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
