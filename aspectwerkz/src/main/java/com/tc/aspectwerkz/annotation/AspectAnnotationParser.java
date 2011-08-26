/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.annotation;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.definition.AdviceDefinition;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.DefinitionParserHelper;
import com.tc.aspectwerkz.definition.DeploymentScope;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.util.Strings;

import java.util.Iterator;
import java.util.List;

/**
 * Extracts the aspects annotations from the class files and creates a meta-data representation of them.
 * <br/>
 * Note: we are not using reflection to loop over fields, etc, so that we do not trigger nested loading, which could be
 * potential target classes.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class AspectAnnotationParser {

  /**
   * The sole instance.
   */
  private final static AspectAnnotationParser INSTANCE = new AspectAnnotationParser();

  /**
   * Private constructor to prevent subclassing.
   */
  private AspectAnnotationParser() {
  }

  /**
   * Parse the attributes and create and return a meta-data representation of them.
   *
   * @param classInfo the class to extract attributes from
   * @param aspectDef the aspect definition
   * @param loader
   */
  public static void parse(final ClassInfo classInfo, final AspectDefinition aspectDef, final ClassLoader loader) {
    INSTANCE.doParse(classInfo, aspectDef, loader);
  }

  /**
   * Parse the attributes and create and return a meta-data representation of them.
   *
   * @param classInfo the class to extract attributes from
   * @param aspectDef the aspect definition
   * @param loader
   */
  private void doParse(final ClassInfo classInfo, final AspectDefinition aspectDef, final ClassLoader loader) {
    if (classInfo == null) {
      throw new IllegalArgumentException("class to parse can not be null");
    }

    Aspect aspectAnnotation = (Aspect) AsmAnnotations.getAnnotation(
            AnnotationConstants.ASPECT,
            classInfo
    );

    String aspectName = classInfo.getName();
    String deploymentModelAsString = null;

    if (aspectAnnotation != null) {
      if (aspectAnnotation.value() != null) {
        //@Aspect(perJVM)
        deploymentModelAsString = aspectAnnotation.value();
      } else {
        if (aspectAnnotation.name() != null) {
          //@Aspect(name=..)
          aspectName = aspectAnnotation.name();
        }
        if (aspectAnnotation.deploymentModel() != null) {
          //@Aspect(deploymentModel=..)
          deploymentModelAsString = aspectAnnotation.deploymentModel();
        }
      }
    }

    // attribute settings override the xml settings
    aspectDef.setDeploymentModel(DeploymentModel.getDeploymentModelFor(deploymentModelAsString));
    String className = classInfo.getName();
    parseFieldAttributes(classInfo, aspectDef);
    parseMethodAttributes(classInfo, className, aspectName, aspectDef);
  }

  /**
   * Parses the field attributes and creates a meta-data representation of them.
   *
   * @param classInfo the class to extract attributes from
   * @param aspectDef the aspect definition
   */
  private void parseFieldAttributes(final ClassInfo classInfo, final AspectDefinition aspectDef) {
    if (aspectDef == null) {
      throw new IllegalArgumentException("aspect definition can not be null");
    }
    if (classInfo == null) {
      return;
    }

    FieldInfo[] fieldList = classInfo.getFields();
    for (int i = 0; i < fieldList.length; i++) {
      FieldInfo field = fieldList[i];

      // expression ie pointcut or deployment scopes
      Expression expression = (Expression) AsmAnnotations.getAnnotation(AnnotationConstants.EXPRESSION, field);
      if (expression != null) {
        if (field.getType().getName().equals(DeploymentScope.class.getName())) {
          DefinitionParserHelper.createAndAddDeploymentScopeDef(
                  field.getName(),
                  expression.value(),
                  aspectDef.getSystemDefinition()
          );
        } else {
          DefinitionParserHelper.createAndAddPointcutDefToAspectDef(
                  field.getName(),
                  expression.value(),
                  aspectDef
          );
        }
      }

      // introduce
      Introduce introduce = (Introduce) AsmAnnotations.getAnnotation(AnnotationConstants.INTRODUCE, field);
      if (introduce != null) {
        DefinitionParserHelper.createAndAddInterfaceIntroductionDefToAspectDef(
                introduce.value(),
                field.getName(),
                field.getType().getName(),
                aspectDef
        );
      }
    }

    // recursive call, next iteration based on super class
    parseFieldAttributes(classInfo.getSuperclass(), aspectDef);
  }

  /**
   * Parses the method attributes and creates a meta-data representation of them.
   *
   * @param classInfo       the class
   * @param aspectClassName the aspect class name
   * @param aspectName      the aspect name
   * @param aspectDef       the aspect definition
   */
  private void parseMethodAttributes(final ClassInfo classInfo,
                                     final String aspectClassName,
                                     final String aspectName,
                                     final AspectDefinition aspectDef) {
    if (classInfo == null) {
      throw new IllegalArgumentException("class can not be null");
    }
    if (aspectClassName == null) {
      throw new IllegalArgumentException("aspect class name can not be null");
    }
    if (aspectName == null) {
      throw new IllegalArgumentException("aspect name can not be null " + aspectClassName);
    }
    if (aspectDef == null) {
      throw new IllegalArgumentException("aspect definition can not be null");
    }
    // getDefault complete method list (includes inherited ones)
    List methodList = ClassInfoHelper.createMethodList(classInfo);

    // iterate first on all method to lookup @Expression Pointcut annotations so that they can be resolved
    parsePointcutAttributes(methodList, aspectDef);

    // iterate on the advice annotations
    for (Iterator it = methodList.iterator(); it.hasNext();) {
      MethodInfo method = (MethodInfo) it.next();
      try {
        // create the advice name out of the class and method name, <classname>.<methodname>
        parseAroundAttributes(method, aspectName, aspectClassName, aspectDef);
        parseBeforeAttributes(method, aspectName, aspectClassName, aspectDef);
        parseAfterAttributes(method, aspectName, aspectClassName, aspectDef);
      } catch (DefinitionException e) {
        System.err.println("AW::WARNING - unable to register advice: " + e.toString());
        // TODO AV - better handling of reg issue (f.e. skip the whole aspect, in DocumentParser, based on DefinitionE
      }
    }
  }

  /**
   * Parses the method pointcut attributes.
   *
   * @param methodList
   * @param aspectDef
   */
  private void parsePointcutAttributes(final List methodList, final AspectDefinition aspectDef) {
    for (Iterator it = methodList.iterator(); it.hasNext();) {
      MethodInfo method = (MethodInfo) it.next();

      // Pointcut with signature
      Expression annotation = (Expression) AsmAnnotations.getAnnotation(AnnotationConstants.EXPRESSION, method);
      if (annotation != null) {
        DefinitionParserHelper.createAndAddPointcutDefToAspectDef(
                getAdviceNameAsInSource(method),
                annotation.value(), aspectDef
        );
      }
    }
  }

  /**
   * Parses the around attributes.
   *
   * @param method
   * @param aspectName
   * @param aspectClassName
   * @param aspectDef
   */
  private void parseAroundAttributes(final MethodInfo method,
                                     final String aspectName,
                                     final String aspectClassName,
                                     final AspectDefinition aspectDef) {
    Around aroundAnnotation = (Around) AsmAnnotations.getAnnotation(AnnotationConstants.AROUND, method);
    if (aroundAnnotation != null) {
      AdviceDefinition adviceDef = DefinitionParserHelper.createAdviceDefinition(
              getAdviceNameAsInSource(method),
              AdviceType.AROUND,
              aroundAnnotation.value(),
              null,
              aspectName,
              aspectClassName,
              method,
              aspectDef
      );
      aspectDef.addAroundAdviceDefinition(adviceDef);
    }
  }

  /**
   * Parses the before attributes.
   *
   * @param method
   * @param aspectName
   * @param aspectClassName
   * @param aspectDef
   */
  private void parseBeforeAttributes(final MethodInfo method,
                                     final String aspectName,
                                     final String aspectClassName,
                                     final AspectDefinition aspectDef) {
    Before beforeAnnotation = (Before) AsmAnnotations.getAnnotation(AnnotationConstants.BEFORE, method);
    if (beforeAnnotation != null) {
      AdviceDefinition adviceDef = DefinitionParserHelper.createAdviceDefinition(
              getAdviceNameAsInSource(method),
              AdviceType.BEFORE,
              beforeAnnotation.value(),
              null,
              aspectName,
              aspectClassName,
              method,
              aspectDef
      );
      aspectDef.addBeforeAdviceDefinition(adviceDef);
    }
  }

  /**
   * Parses the after attributes.
   *
   * @param method
   * @param aspectName
   * @param aspectClassName
   * @param aspectDef
   */
  private void parseAfterAttributes(final MethodInfo method,
                                    final String aspectName,
                                    final String aspectClassName,
                                    final AspectDefinition aspectDef) {
    After annotationAft = (After) AsmAnnotations.getAnnotation(AnnotationConstants.AFTER, method);
    if (annotationAft != null) {
      AdviceDefinition adviceDef = DefinitionParserHelper.createAdviceDefinition(
              getAdviceNameAsInSource(method),
              AdviceType.AFTER,
              annotationAft.value(),
              null,
              aspectName,
              aspectClassName,
              method,
              aspectDef
      );
      aspectDef.addAfterAdviceDefinition(adviceDef);
    }

    AfterReturning annotationRet = (AfterReturning) AsmAnnotations.getAnnotation(AnnotationConstants.AFTER_RETURNING, method);
    if (annotationRet != null) {
      AdviceDefinition adviceDef = DefinitionParserHelper.createAdviceDefinition(
              getAdviceNameAsInSource(method),
              AdviceType.AFTER_RETURNING,
              getExpressionElseValue(annotationRet.value(), annotationRet.pointcut()),
              annotationRet.type(),
              aspectName,
              aspectClassName,
              method,
              aspectDef
      );
      aspectDef.addAfterAdviceDefinition(adviceDef);
    }

    AfterThrowing annotationThr = (AfterThrowing) AsmAnnotations.getAnnotation(AnnotationConstants.AFTER_THROWING, method);
    if (annotationThr != null) {
      AdviceDefinition adviceDef = DefinitionParserHelper.createAdviceDefinition(
              getAdviceNameAsInSource(method),
              AdviceType.AFTER_THROWING,
              getExpressionElseValue(annotationThr.value(), annotationThr.pointcut()),
              annotationThr.type(),
              aspectName,
              aspectClassName,
              method,
              aspectDef
      );
      aspectDef.addAfterAdviceDefinition(adviceDef);
    }

    AfterFinally annotationFin = (AfterFinally) AsmAnnotations.getAnnotation(AnnotationConstants.AFTER_FINALLY, method);
    if (annotationFin != null) {
      AdviceDefinition adviceDef = DefinitionParserHelper.createAdviceDefinition(
              getAdviceNameAsInSource(method),
              AdviceType.AFTER_FINALLY,
              annotationFin.value(),
              null,
              aspectName,
              aspectClassName,
              method,
              aspectDef
      );
      aspectDef.addAfterAdviceDefinition(adviceDef);
    }
  }

  /**
   * Returns the call signature of a Pointcut or advice with signature methodName(paramType paramName, ...) [we ignore
   * the return type] If there is no parameters, the call signature is not "name()" but just "name"
   *
   * @param methodInfo
   * @return string representation (see javavadoc)
   */
  public static String getAdviceNameAsInSource(final MethodInfo methodInfo) {
    StringBuffer buffer = new StringBuffer(methodInfo.getName());
    if (methodInfo.getParameterNames() == null
            || methodInfo.getParameterNames().length != methodInfo.getParameterTypes().length
            || (methodInfo.getParameterNames().length > 0 && methodInfo.getParameterNames()[0] == null)) {
      return methodInfo.getName();
    }
    if (methodInfo.getParameterNames().length > 0) {
      buffer.append('(');
      for (int i = 0; i < methodInfo.getParameterNames().length; i++) {
        if (i > 0) {
          buffer.append(", ");
        }
        String parameterName = methodInfo.getParameterNames()[i];
        buffer.append(methodInfo.getParameterTypes()[i].getName());
        buffer.append(' ').append(parameterName);
      }
      buffer.append(')');
    }
    return buffer.toString();
  }

  /**
   * Handles specific syntax for @AfterXXX annotation, where we can write it using the default "value" element
   * or instead specify the pointcut using "pointcut", and optionally a "type" element.
   *
   * @param value
   * @param pointcut
   * @return the one of value or expression which is not null. Both cannot be specified at the same time
   */
  public static String getExpressionElseValue(String value, String pointcut) {
    if (!Strings.isNullOrEmpty(pointcut)) {
      return pointcut;
    } else if (!Strings.isNullOrEmpty(value)) {
      return value;
    } else {
      throw new DefinitionException("neither expression nor value had a valid value");
    }
  }

}