/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.definition;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.expression.regexp.Pattern;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.ExpressionNamespace;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.util.Strings;

import java.util.Iterator;
import java.util.Collection;
import java.util.List;

/**
 * Helper class for the attribute and the XML definition parsers.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class DefinitionParserHelper {
  public static final String EXPRESSION_PREFIX = "AW_";

  /**
   * Creates and adds pointcut definition to aspect definition.
   *
   * @param name
   * @param expression
   * @param aspectDef
   */
  public static void createAndAddPointcutDefToAspectDef(final String name,
                                                        final String expression,
                                                        final AspectDefinition aspectDef) {
    PointcutDefinition pointcutDef = new PointcutDefinition(expression);
    aspectDef.addPointcutDefinition(pointcutDef);

    // name can be the "pcName(paramType paramName)"
    // extract the parameter name to type map
    // and register the pointcut using its name
    //TODO: support for same pc name and different signature
    String pointcutName = name;
    String pointcutCallSignature = null;
    if (name.indexOf("(") > 0) {
      pointcutName = name.substring(0, name.indexOf("("));
      pointcutCallSignature = name.substring(name.indexOf("(") + 1, name.lastIndexOf(")"));
    }

    // do a lookup first to avoid infinite recursion when:
    // <pointcut name="pc" ...> [will be registered as pc]
    // <advice bind-to="pc" ...> [will be registered as pc and should not override previous one !]
    ExpressionNamespace namespace = ExpressionNamespace.getNamespace(aspectDef.getQualifiedName());
    ExpressionInfo info = namespace.getExpressionInfoOrNull(pointcutName);
    if (info == null) {
      info = new ExpressionInfo(expression, aspectDef.getQualifiedName());
      // extract the pointcut signature map
      if (pointcutCallSignature != null) {
        String[] parameters = Strings.splitString(pointcutCallSignature, ",");
        for (int i = 0; i < parameters.length; i++) {
          String[] parameterInfo = Strings.splitString(
                  Strings.replaceSubString(parameters[i].trim(), "  ", " "),
                  " "
          );
          info.addArgument(parameterInfo[1], parameterInfo[0], aspectDef.getClassInfo().getClassLoader());
        }
      }
    }
    ExpressionNamespace.getNamespace(aspectDef.getQualifiedName()).addExpressionInfo(pointcutName, info);
  }

  /**
   * Creates and adds a prepared pointcut definition to virtual aspect definition.
   *
   * @param name
   * @param expression
   * @param systemDef
   */
  public static void createAndAddDeploymentScopeDef(final String name,//TODO Depl scpope(prasen)field name not unique - need FQN
                                                    final String expression,
                                                    final SystemDefinition systemDef) {
    AspectDefinition aspectDef = systemDef.getAspectDefinition(Virtual.class.getName());
    aspectDef.addPointcutDefinition(new PointcutDefinition(expression));
    systemDef.addDeploymentScope(new DeploymentScope(name, expression));
  }

  /**
   * Creates and adds an advisable definition to virtual aspect definition.
   *
   * @param expression
   * @param systemDef
   */
  public static void createAndAddAdvisableDef(final String expression, final SystemDefinition systemDef) {
    AspectDefinition virtualAspectDef = systemDef.getAspectDefinition(Virtual.class.getName());
    virtualAspectDef.addPointcutDefinition(new PointcutDefinition(expression));

    AdviceDefinition virtualAdviceDef = (AdviceDefinition) virtualAspectDef.getBeforeAdviceDefinitions().get(0);
    ExpressionInfo oldExpressionInfo = virtualAdviceDef.getExpressionInfo();
    String newExpression;
    if (oldExpressionInfo != null) {
      String oldExpression = oldExpressionInfo.toString();
      newExpression = oldExpression + " || " + expression;
    } else {
      newExpression = expression;
    }

    virtualAdviceDef.setExpressionInfo(
            new ExpressionInfo(
                    newExpression,
                    virtualAspectDef.getQualifiedName()
            )
    );
  }

  /**
   * Attaches all deployment scopes in a system to the virtual advice.
   *
   * @param systemDef the system definition
   */
  public static void attachDeploymentScopeDefsToVirtualAdvice(final SystemDefinition systemDef) {
    final AspectDefinition virtualAspectDef = systemDef.getAspectDefinition(Virtual.class.getName());
    final AdviceDefinition virtualAdviceDef = (AdviceDefinition) virtualAspectDef.getBeforeAdviceDefinitions().get(
            0
    );

    final StringBuffer newExpression = new StringBuffer();
    final ExpressionInfo oldExpressionInfo = virtualAdviceDef.getExpressionInfo();
    if (oldExpressionInfo != null) {
      String oldExpression = oldExpressionInfo.toString();
      newExpression.append(oldExpression);
    }
    final Collection deploymentScopes = systemDef.getDeploymentScopes();
    if (deploymentScopes.size() != 0 && oldExpressionInfo != null) {
      newExpression.append(" || ");
    }
    for (Iterator it = deploymentScopes.iterator(); it.hasNext();) {
      DeploymentScope deploymentScope = (DeploymentScope) it.next();
      newExpression.append(deploymentScope.getExpression());
      if (it.hasNext()) {
        newExpression.append(" || ");
      }
    }
    if (newExpression.length() != 0) {
      virtualAdviceDef.setExpressionInfo(
              new ExpressionInfo(
                      newExpression.toString(),
                      virtualAspectDef.getQualifiedName()
              )
      );
    }
  }

  /**
   * Creates and add mixin definition to system definition.
   *
   * @param mixinClassInfo
   * @param expression
   * @param deploymentModel
   * @param isTransient
   * @param systemDef
   * @return the mixin definition
   */
  public static MixinDefinition createAndAddMixinDefToSystemDef(final ClassInfo mixinClassInfo,
                                                                final String expression,
                                                                final DeploymentModel deploymentModel,
                                                                final boolean isTransient,
                                                                final SystemDefinition systemDef) {
    final MixinDefinition mixinDef = createMixinDefinition(
            mixinClassInfo,
            expression,
            deploymentModel,
            isTransient,
            systemDef
    );

    // check doublons - TODO change ArrayList to HashMap since NAME is a key
    MixinDefinition doublon = null;
    for (Iterator intros = systemDef.getMixinDefinitions().iterator(); intros.hasNext();) {
      MixinDefinition intro = (MixinDefinition) intros.next();
      if (intro.getMixinImpl().getName().equals(mixinDef.getMixinImpl().getName())) {
        doublon = intro;
        intro.addExpressionInfos(mixinDef.getExpressionInfos());
        break;
      }
    }
    if (doublon == null) {
      systemDef.addMixinDefinition(mixinDef);
    }
    return mixinDef;
  }

  /**
   * Creates and add interface introduction definition to aspect definition.
   *
   * @param expression
   * @param introductionName
   * @param interfaceClassName
   * @param aspectDef
   */
  public static void createAndAddInterfaceIntroductionDefToAspectDef(final String expression,
                                                                     final String introductionName,
                                                                     final String interfaceClassName,
                                                                     final AspectDefinition aspectDef) {
    // Introduction name is unique within an aspectDef only
    InterfaceIntroductionDefinition introDef = createInterfaceIntroductionDefinition(
            introductionName,
            expression,
            interfaceClassName,
            aspectDef
    );
    aspectDef.addInterfaceIntroductionDefinition(introDef);
  }

  /**
   * Creates a new advice definition.
   *
   * @param adviceName          the advice name
   * @param adviceType          the advice type
   * @param expression          the advice expression
   * @param specialArgumentType the arg
   * @param aspectName          the aspect name
   * @param aspectClassName     the aspect class name
   * @param methodInfo          the advice methodInfo
   * @param aspectDef           the aspect definition
   * @return the new advice definition
   */
  public static AdviceDefinition createAdviceDefinition(final String adviceName,
                                                        final AdviceType adviceType,
                                                        final String expression,
                                                        final String specialArgumentType,
                                                        final String aspectName,
                                                        final String aspectClassName,
                                                        final MethodInfo methodInfo,
                                                        final AspectDefinition aspectDef) {
    ExpressionInfo expressionInfo = new ExpressionInfo(
            expression,
            aspectDef.getQualifiedName()
    );

    // support for pointcut signature
    String adviceCallSignature = null;
    String resolvedSpecialArgumentType = specialArgumentType;
    if (adviceName.indexOf('(') > 0) {
      adviceCallSignature = adviceName.substring(adviceName.indexOf('(') + 1, adviceName.lastIndexOf(')'));
      String[] parameters = Strings.splitString(adviceCallSignature, ",");
      for (int i = 0; i < parameters.length; i++) {
        String[] parameterInfo = Strings.splitString(
                Strings.replaceSubString(parameters[i].trim(), "  ", " "),
                " "
        );
        // Note: for XML defined aspect, we support anonymous parameters like
        // advice(JoinPoint, Rtti) as well as abbreviations, so we have to assign
        // them a name here, as well as their real type
        String paramName, paramType = null;
        if (parameterInfo.length == 2) {
          paramName = parameterInfo[1];
          paramType = parameterInfo[0];
        } else {
          paramName = "anonymous_" + i;
          paramType = (String) Pattern.ABBREVIATIONS.get(parameterInfo[0]);
        }
        // skip the parameter if this ones is a after returning / throwing binding
        if (paramName.equals(specialArgumentType)) {
          resolvedSpecialArgumentType = paramType;
          expressionInfo.setSpecialArgumentName(paramName);
        } else {
          expressionInfo.addArgument(paramName, paramType, aspectDef.getClassInfo().getClassLoader());
        }
      }
    }

    // check that around advice return Object else the compiler will fail
    if (adviceType.equals(AdviceType.AROUND)) {
      if (!"java.lang.Object".equals(methodInfo.getReturnType().getName())) {
        throw new DefinitionException(
                "around advice must return java.lang.Object : " + aspectClassName + "." + methodInfo.getName()
        );
      }
    }

    final AdviceDefinition adviceDef = new AdviceDefinition(
            adviceName,
            adviceType,
            resolvedSpecialArgumentType,
            aspectName,
            aspectClassName,
            expressionInfo,
            methodInfo,
            aspectDef
    );
    return adviceDef;
  }

  /**
   * Creates an introduction definition.
   *
   * @param mixinClassInfo
   * @param expression
   * @param deploymentModel
   * @param isTransient
   * @param systemDef
   * @return
   */
  public static MixinDefinition createMixinDefinition(final ClassInfo mixinClassInfo,
                                                      final String expression,
                                                      final DeploymentModel deploymentModel,
                                                      final boolean isTransient,
                                                      final SystemDefinition systemDef) {
    final MixinDefinition mixinDef = new MixinDefinition(mixinClassInfo, deploymentModel, isTransient, systemDef);
    if (expression != null) {
      ExpressionInfo expressionInfo = new ExpressionInfo(expression, systemDef.getUuid());

      // auto-name the pointcut which is anonymous for introduction
      ExpressionNamespace.getNamespace(systemDef.getUuid()).addExpressionInfo(
              EXPRESSION_PREFIX + expression.hashCode(),
              expressionInfo
      );
      mixinDef.addExpressionInfo(expressionInfo);
    }
    return mixinDef;
  }

  /**
   * Creates a new interface introduction definition.
   *
   * @param introductionName   the introduction name
   * @param expression         the pointcut expression
   * @param interfaceClassName the class name of the interface
   * @param aspectDef          the aspect definition
   * @return the new introduction definition
   */
  public static InterfaceIntroductionDefinition createInterfaceIntroductionDefinition(final String introductionName,
                                                                                      final String expression,
                                                                                      final String interfaceClassName,
                                                                                      final AspectDefinition aspectDef) {
    final InterfaceIntroductionDefinition introDef = new InterfaceIntroductionDefinition(
            introductionName, interfaceClassName
    );
    if (expression != null) {
      ExpressionInfo expressionInfo = new ExpressionInfo(expression, aspectDef.getQualifiedName());

      // auto-name the pointcut which is anonymous for introduction
      ExpressionNamespace.getNamespace(aspectDef.getQualifiedName()).addExpressionInfo(
              EXPRESSION_PREFIX + expression.hashCode(),
              expressionInfo
      );
      introDef.addExpressionInfo(expressionInfo);
    }
    return introDef;
  }

  /**
   * Creates the advice definitions and adds them to the aspect definition.
   *
   * @param type      the type of advice
   * @param bindTo    the pointcut expresion
   * @param name      the name of the advice
   * @param method    the method implementing the advice
   * @param aspectDef the aspect definition
   */
  public static void createAndAddAdviceDefsToAspectDef(final String type,
                                                       final String bindTo,
                                                       final String name,
                                                       final MethodInfo method,
                                                       final AspectDefinition aspectDef) {
    try {
      if (type.equalsIgnoreCase("around")) {
        final String aspectName = aspectDef.getName();
        AdviceDefinition adviceDef = createAdviceDefinition(
                name,
                AdviceType.AROUND,
                bindTo,
                null,
                aspectName,
                aspectDef.getClassName(),
                method,
                aspectDef
        );
        aspectDef.addAroundAdviceDefinition(adviceDef);

      } else if (type.equalsIgnoreCase("before")) {
        final String aspectName = aspectDef.getName();
        AdviceDefinition adviceDef = createAdviceDefinition(
                name,
                AdviceType.BEFORE,
                bindTo,
                null,
                aspectName,
                aspectDef.getClassName(),
                method,
                aspectDef
        );
        aspectDef.addBeforeAdviceDefinition(adviceDef);

      } else if (type.startsWith("after")) {
        String specialArgumentType = null;
        AdviceType adviceType = AdviceType.AFTER;
        if (type.startsWith("after returning(")) {
          adviceType = AdviceType.AFTER_RETURNING;
          int start = type.indexOf('(');
          int end = type.indexOf(')');
          specialArgumentType = type.substring(start + 1, end).trim();
        } else if (type.startsWith("after throwing(")) {
          adviceType = AdviceType.AFTER_THROWING;
          int start = type.indexOf('(');
          int end = type.indexOf(')');
          specialArgumentType = type.substring(start + 1, end).trim();
        } else if (type.startsWith("after returning")) {
          adviceType = AdviceType.AFTER_RETURNING;
        } else if (type.startsWith("after throwing")) {
          adviceType = AdviceType.AFTER_THROWING;
        } else if (type.startsWith("after")) {
          adviceType = AdviceType.AFTER_FINALLY;
        } else if (type.startsWith("after finally")) {
          adviceType = AdviceType.AFTER_FINALLY;
        }
        if (specialArgumentType != null && specialArgumentType.indexOf(' ') > 0) {
          throw new DefinitionException(
                  "argument to after (returning/throwing) can only be a type (parameter name binding should be done using args(..))"
          );
        }
        final String aspectName = aspectDef.getName();
        AdviceDefinition adviceDef = createAdviceDefinition(
                name,
                adviceType,
                bindTo,
                specialArgumentType,
                aspectName,
                aspectDef.getClassName(),
                method,
                aspectDef
        );

        aspectDef.addAfterAdviceDefinition(adviceDef);
      } else {
        throw new DefinitionException("Unkonw type for advice : " + type);
      }
    } catch (DefinitionException e) {
      System.err.println(
              "WARNING: unable to register advice " + aspectDef.getName() + "." + name +
                      " at pointcut [" + bindTo + "] due to: " + e.getMessage()
      );
      // TODO ALEX - better handling of reg issue (f.e. skip the whole aspect, in DocumentParser, based on DefinitionE
    }
  }

  public static MethodInfo createMethodInfoForAdviceFQN(final String name,
                                                        final AspectDefinition aspectDef,
                                                        final ClassInfo aspectClassInfo) {
    List adviceMethodList = ClassInfoHelper.createMethodList(aspectClassInfo);
    MethodInfo method = null;
    for (Iterator it3 = adviceMethodList.iterator(); it3.hasNext();) {
      MethodInfo methodCurrent = (MethodInfo) it3.next();
      if (aspectDef.isAspectWerkzAspect()) {
        if (matchMethodAsAdvice(methodCurrent, name)) {
          method = methodCurrent;
          break;
        }
      } else {
        // TODO support matchMethodAsAdvice(..) for all aspect models? if so use stuff below
        //                        AspectModel aspectModel = AspectModelManager.getModelFor(aspectDef.getAspectModel());
        //                        if (aspectModel.matchMethodAsAdvice(methodCurrent, name)) {
        //                            method = methodCurrent;
        //                            break;
        //                        }
        if (methodCurrent.getName().equals(name)) {
          method = methodCurrent;
          break;
        }
      }
    }
    if (method == null) {
      throw new DefinitionException(
              "could not find advice method [" + name + "] in [" + aspectClassInfo.getName() +
                      "] (are you using a compiler extension that you have not registered?)" +
                      " (are you using XML defined advice, with StaticJoinPoint bindings without specifying the full" +
                      "source like signature?)"
      );
    }
    return method;
  }

  /**
   * Check if a method from an aspect class match a given advice signature.
   * <br/>
   * If the signature is just a method name, then we have a match even if JoinPoint is sole method parameter.
   * Else we match both method name and parameters type, with abbreviation support (java.lang.* and JoinPoint)
   *
   * @param method
   * @param adviceSignature
   * @return boolean
   */
  private static boolean matchMethodAsAdvice(MethodInfo method, String adviceSignature) {
    // grab components from adviceSignature
    //TODO catch AOOBE for better syntax error reporting
    String[] signatureElements = Strings.extractMethodSignature(adviceSignature);

    // check method name
    if (!method.getName().equals(signatureElements[0])) {
      return false;
    }
    // check number of args
    if (method.getParameterTypes().length * 2 != signatureElements.length - 1) {
      // we still match if method has "JoinPoint" has sole parameter
      // and adviceSignature has none
      if (signatureElements.length == 1 &&
              method.getParameterTypes().length == 1 &&
              (method.getParameterTypes()[0].getName().equals(TransformationConstants.JOIN_POINT_JAVA_CLASS_NAME)
                      || method.getParameterTypes()[0].getName().equals(TransformationConstants.STATIC_JOIN_POINT_JAVA_CLASS_NAME)))
      {
        return true;
      } else {
        return false;
      }
    }
    int argIndex = 0;
    for (int i = 1; i < signatureElements.length; i++) {
      String paramType = signatureElements[i++];
      String methodParamType = method.getParameterTypes()[argIndex++].getName();
      // handle shortcuts for java.lang.* and JoinPoint, StaticJoinPoint and Rtti
      String paramTypeResolved = (String) Pattern.ABBREVIATIONS.get(paramType);
      if (methodParamType.equals(paramType) || methodParamType.equals(paramTypeResolved)) {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }
}