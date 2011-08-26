/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;

import com.tc.backport175.bytecode.AnnotationElement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JavaModelAnnotationInfo {
  private List<AnnotationElement.Annotation> fAnnotations;
  private static final Class                 fAnnotatableClass;

  static {
    Class c = null;
    try {
      c = Class.forName("org.eclipse.jdt.core.IAnnotatable");
    } catch (ClassNotFoundException cnfe) {
      /**/
    }
    fAnnotatableClass = c;
  }

  public JavaModelAnnotationInfo() {
    fAnnotations = new ArrayList<AnnotationElement.Annotation>();    
  }
  
  public JavaModelAnnotationInfo(IMember member) {
    this();
    if (isAnnotatable(member)) {
      String[] annoTypes = getAnnotationTypes(member);
      for (String annoType : annoTypes) {
        addAnnotation(annoType);
      }
    }
  }

  private static String[] getAnnotationTypes(IMember member) {
    IType declaringType;
    if(member instanceof IType) {
      declaringType = (IType)member;
    } else {
      declaringType = member.getDeclaringType();
    }
    List<String> result = new ArrayList<String>();
    try {
      Method getter = member.getClass().getMethod("getAnnotations");
      if (getter != null) {
        Object[] annos = (Object[]) getter.invoke(member);
        for (Object anno : annos) {
          Method getName = anno.getClass().getMethod("getElementName");
          String name = (String) getName.invoke(anno);
          String[][] annoFullNames = declaringType.resolveType(name);
          if (annoFullNames != null) {
            String[] elems = annoFullNames[0];
            String fqcn = elems[0] + "." + elems[1];
            result.add(fqcn);
          }
        }
      }
    } catch (Exception e) {
      /**/
    }
    return result.toArray(new String[result.size()]);
  }

  public void clearAnnotations() {
    fAnnotations.clear();
  }

  public void addAnnotation(Annotation annotation) {
    IAnnotationBinding binding = annotation.resolveAnnotationBinding();
    String name = binding.getAnnotationType().getQualifiedName();
    addAnnotation(name);
  }

  public void addAnnotation(String fqcn) {
    fAnnotations.add(new AnnotationElement.Annotation(fqcn));
  }

  public AnnotationElement.Annotation[] getAnnotations() {
    return fAnnotations.toArray(new AnnotationElement.Annotation[0]);
  }

  public void clear() {
    fAnnotations.clear();
  }
  
  public static boolean isAnnotatable(Object target) {
    return fAnnotatableClass != null && fAnnotatableClass.isAssignableFrom(target.getClass());
  }
}
