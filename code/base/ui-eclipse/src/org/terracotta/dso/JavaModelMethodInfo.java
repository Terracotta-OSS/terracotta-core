/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;

import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.object.bytecode.aspectwerkz.AsmMethodInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;

import java.util.ArrayList;
import java.util.List;

public class JavaModelMethodInfo extends AsmMethodInfo {
  private List<AnnotationElement.Annotation> fAnnotations = new ArrayList<AnnotationElement.Annotation>();
  
  public JavaModelMethodInfo(ClassInfoFactory classInfoFactory, IMethod method) throws JavaModelException {
    this(classInfoFactory,
        method.getFlags(),
        PatternHelper.getFullyQualifiedName(method.getDeclaringType()),
        method.isConstructor() ? "__INIT__" : method.getElementName(),
        PatternHelper.getSignature(method),
        method.getExceptionTypes());
  }
  
  public JavaModelMethodInfo(ClassInfoFactory classInfoFactory, int modifiers, String className, String methodName, String desc, String[] exceptions) {
    super(classInfoFactory, modifiers, className, methodName, desc, exceptions);
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
}
