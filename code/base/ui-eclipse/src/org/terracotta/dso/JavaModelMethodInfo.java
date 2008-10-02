/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;

import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.object.bytecode.aspectwerkz.AsmMethodInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;

public class JavaModelMethodInfo extends AsmMethodInfo {
  private JavaModelAnnotationInfo fAnnotationInfo;

  public JavaModelMethodInfo(ClassInfoFactory classInfoFactory, IMethod method) throws JavaModelException {
    this(classInfoFactory, method.getFlags(), PatternHelper.getFullyQualifiedName(method.getDeclaringType()), method
        .isConstructor() ? "__INIT__" : method.getElementName(), PatternHelper.getSignature(method), method
        .getExceptionTypes());
    fAnnotationInfo = new JavaModelAnnotationInfo(method);
  }

  public JavaModelMethodInfo(ClassInfoFactory classInfoFactory, int modifiers, String className, String methodName,
                             String desc, String[] exceptions) {
    super(classInfoFactory, modifiers, className, methodName, desc, exceptions);
    fAnnotationInfo = new JavaModelAnnotationInfo();
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
}
