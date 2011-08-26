/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

import com.tc.backport175.Annotation;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;

/**
 * Helper class to extract annotations by their name from a ClassInfo structure using backport API.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
public class AsmAnnotations {
  /**
   * Return the annotation with a specific name for a specific class.
   *
   * @param annotationName the annotation name
   * @param classInfo      the ClassInfo object to find the annotation on.
   * @return the annotation or null
   */
  public static Annotation getAnnotation(final String annotationName, final ClassInfo classInfo) {
    return classInfo.getAnnotationReader().getAnnotation(annotationName);
  }

  /**
   * Return the annotation with a specific name for a specific method.
   *
   * @param annotationName the annotation name
   * @param methodInfo     the MethodInfo object to find the annotation on.
   * @return the annotation or null
   */
  public static Annotation getAnnotation(final String annotationName, final MethodInfo methodInfo) {
    return methodInfo.getDeclaringType().getAnnotationReader().getMethodAnnotation(
            annotationName,
            methodInfo.getName(),
            methodInfo.getSignature(),
            methodInfo.getDeclaringType().getClassLoader()
    );
  }

  /**
   * Return the annotation with a specific name for a specific constructor.
   *
   * @param annotationName  the annotation name
   * @param constructorInfo the ConstructorInfo object to find the annotation on.
   * @return the annotation or null
   */
  public static Annotation getAnnotation(final String annotationName, final ConstructorInfo constructorInfo) {
    return constructorInfo.getDeclaringType().getAnnotationReader().getConstructorAnnotation(
            annotationName,
            constructorInfo.getSignature(),
            constructorInfo.getDeclaringType().getClassLoader()
    );
  }

  /**
   * Return the annotation with a specific name for a specific field.
   *
   * @param annotationName the annotation name
   * @param fieldInfo      the FieldInfo object to find the annotation on.
   * @return the annotation or null
   */
  public static Annotation getAnnotation(final String annotationName, final FieldInfo fieldInfo) {
    return fieldInfo.getDeclaringType().getAnnotationReader().getFieldAnnotation(
            annotationName,
            fieldInfo.getName(),
            fieldInfo.getSignature(),
            fieldInfo.getDeclaringType().getClassLoader()
    );
  }
}
