/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.StringUtils;

import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.backport175.bytecode.AnnotationElement.NamedValue;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RootNameUtil {

  private static final String   TIM_ANNOTATIONS_ROOT_CLASSNAME_DOTS = "org.terracotta.modules.annotations.Root";
  private static final Pattern  VALID_ROOT_NAME_PATTERN             = Pattern
                                                                        .compile("[A-Za-z_][A-Za-z_0-9$]*(.[A-Za-z_][A-Za-z_0-9$]*)*");

  private static final TCLogger consoleLogger                       = CustomerLogging.getConsoleLogger();

  public static String getAnnotatedOrDefaultRootName(FieldInfo fieldInfo) {
    String rootNameFromAnnotation = getAnnotatedRootNameIfPresent(fieldInfo);
    if (StringUtils.isBlank(rootNameFromAnnotation)) return getDefaultRootName(fieldInfo);
    else return rootNameFromAnnotation;
  }

  public static String getDefaultRootName(FieldInfo fieldInfo) {
    return fieldInfo.getDeclaringType().getName() + "." + fieldInfo.getName();
  }

  public static String getAnnotatedRootNameIfPresent(FieldInfo fieldInfo) {
    com.tc.backport175.bytecode.AnnotationElement.Annotation[] annotations = fieldInfo.getAnnotations();
    for (com.tc.backport175.bytecode.AnnotationElement.Annotation annotation : annotations) {
      if (annotation.getInterfaceName().equals(TIM_ANNOTATIONS_ROOT_CLASSNAME_DOTS)) {
        if (annotation.getElements().size() == 0) return null;
        if (annotation.getElements().size() != 1) { throw new AssertionError("Expected one attribute for annotation "
                                                                             + TIM_ANNOTATIONS_ROOT_CLASSNAME_DOTS); }
        String name = (String) ((NamedValue) annotation.getElements().get(0)).getValue();
        if (StringUtils.isBlank(name)) return name;
        if (!isValidRootName(name)) {
          String msg = "Root-name specified for " + getDefaultRootName(fieldInfo) + " is not valid - " + name;
          consoleLogger.error(msg);
          throw new AssertionError(msg);
        }
        return name;
      }
    }
    return null;
  }

  public static boolean isValidRootName(String name) {
    Matcher m = VALID_ROOT_NAME_PATTERN.matcher(name);
    return m.matches();
  }
}
