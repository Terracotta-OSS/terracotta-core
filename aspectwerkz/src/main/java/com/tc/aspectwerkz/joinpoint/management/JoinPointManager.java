/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.management;

import com.tc.asm.Type;

import com.tc.aspectwerkz.aspect.AdviceInfo;
import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.definition.AdviceDefinition;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.definition.Virtual;
import com.tc.aspectwerkz.joinpoint.JoinPoint;
import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ReflectionInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.AspectModelManager;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilationInfo;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilerHelper;
import com.tc.aspectwerkz.transform.inlining.spi.AspectModel;
import com.tc.aspectwerkz.util.ContextClassLoader;
import com.tc.aspectwerkz.util.Strings;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.ArgsIndexVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Manages the join point compilation, loading and instantiation for the target classes. This implementation relies on
 * the SystemDefinitionContainer.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class JoinPointManager {

  /**
   * Used to eagerly load the join point compilation. Ensures that the specific joinPoint class for the given target
   * class and joinPoint info is generated. This call is added to the weaved class as a "clinit" block.
   *
   * @param joinPointType
   * @param callerClass
   * @param callerMethodName
   * @param callerMethodDesc
   * @param callerMethodModifiers
   * @param calleeClassName
   * @param calleeMemberName
   * @param calleeMemberDesc
   * @param calleeMemberModifiers
   * @param joinPointHash
   * @param joinPointClassName
   * @param systemDef optional system definition that will override the existing one
   */
  public static void loadJoinPoint(final int joinPointType, final Class callerClass, final String callerMethodName,
      final String callerMethodDesc, final int callerMethodModifiers, final String calleeClassName,
      final String calleeMemberName, final String calleeMemberDesc, final int calleeMemberModifiers,
      final int joinPointHash, final String joinPointClassName, final SystemDefinition systemDef) {
    Class calleeClass = null;
    try {
      if (calleeClassName != null) {
        calleeClass = Class.forName(calleeClassName.replace('/', '.'), false, callerClass.getClassLoader());
      }
    } catch (ClassNotFoundException calleeNotFound) {
      throw new RuntimeException("callee class [" + calleeClassName + "] can not be found in class loader ["
          + callerClass.getClassLoader() + "]");
    }

    // check if the JP is already loaded
    // this can occurs if user packaged its JIT classes, or if we are using multiweaving
    final ClassLoader classLoader = callerClass.getClassLoader();
    boolean generateJoinPoint = false;
    try {
      if (calleeClass == null) { throw new RuntimeException("callee class [" + calleeClassName + "] is NULL"); }
      ContextClassLoader.forName(classLoader, joinPointClassName.replace('/', '.'));
    } catch (ClassNotFoundException e) {
      generateJoinPoint = true;
    }
    if (!generateJoinPoint) { return; }

    final CompiledJoinPoint compiledJoinPoint =
        compileJoinPoint(joinPointType, callerClass, callerMethodName, callerMethodDesc, callerMethodModifiers,
            calleeClassName, calleeMemberName, calleeMemberDesc, calleeMemberModifiers, joinPointHash,
            joinPointClassName, calleeClass, classLoader, systemDef);

    Class jpClass = CompilerHelper.attachToClassLoader(joinPointClassName, classLoader, compiledJoinPoint.bytecode);
    CompilerHelper.addCompilationInfo(jpClass, compiledJoinPoint.compilationInfo);
  }

  /**
   * Ensures that the specific joinPoint class for the given target class and joinPoint info is generated. This call is
   * added to the weaved class as a "clinit" block
   *
   * @param joinPointType
   * @param callerClass
   * @param callerMethodName
   * @param callerMethodDesc
   * @param callerMethodModifiers
   * @param calleeClassName
   * @param calleeMemberName
   * @param calleeMemberDesc
   * @param calleeMemberModifiers
   * @param joinPointHash
   * @param joinPointClassName
   */
  public static void loadJoinPoint(final int joinPointType, final Class callerClass, final String callerMethodName,
      final String callerMethodDesc, final int callerMethodModifiers, final String calleeClassName,
      final String calleeMemberName, final String calleeMemberDesc, final int calleeMemberModifiers,
      final int joinPointHash, final String joinPointClassName) {
    loadJoinPoint(joinPointType, callerClass, callerMethodName, callerMethodDesc, callerMethodModifiers,
        calleeClassName, calleeMemberName, calleeMemberDesc, calleeMemberModifiers, joinPointHash, joinPointClassName,
        null);
  }

  /**
   * Compile a new joinpoint
   *
   * @param joinPointType
   * @param callerClass
   * @param callerMethodName
   * @param callerMethodDesc
   * @param callerMethodModifiers
   * @param calleeClassName
   * @param calleeMemberName
   * @param calleeMemberDesc
   * @param calleeMemberModifiers
   * @param joinPointHash
   * @param joinPointClassName
   * @param calleeClass
   * @param loader the loader that hosts the definitions, and from where caller, callee and aspect are visible. At
   *        runtime it is exactly callerClass.getClassLoader() but in offline mode and genjp, it can happen to be
   *        different when weaved class also exists in the compilation classpath.
   * @param systemDef (can be null)
   * @return the compiled join point
   */
  public static CompiledJoinPoint compileJoinPoint(final int joinPointType, final Class callerClass,
      final String callerMethodName, final String callerMethodDesc, final int callerMethodModifiers,
      final String calleeClassName, final String calleeMemberName, final String calleeMemberDesc,
      final int calleeMemberModifiers, final int joinPointHash, final String joinPointClassName,
      final Class calleeClass, final ClassLoader loader, final SystemDefinition systemDef) {

    ClassInfo calleeClassInfo = JavaClassInfo.getClassInfo(calleeClass);

    // create the callee info
    final ReflectionInfo reflectionInfo;
    final PointcutType pointcutType;
    switch (joinPointType) {
      case JoinPointType.STATIC_INITIALIZATION_INT:
        reflectionInfo = calleeClassInfo.staticInitializer();
        pointcutType = PointcutType.STATIC_INITIALIZATION;
        break;
      case JoinPointType.METHOD_EXECUTION_INT:
        reflectionInfo = calleeClassInfo.getMethod(joinPointHash);
        pointcutType = PointcutType.EXECUTION;
        break;
      case JoinPointType.METHOD_CALL_INT:
        reflectionInfo = calleeClassInfo.getMethod(joinPointHash);
        pointcutType = PointcutType.CALL;
        break;
      case JoinPointType.FIELD_GET_INT:
        reflectionInfo = calleeClassInfo.getField(joinPointHash);
        pointcutType = PointcutType.GET;
        break;
      case JoinPointType.FIELD_SET_INT:
        reflectionInfo = calleeClassInfo.getField(joinPointHash);
        pointcutType = PointcutType.SET;
        break;
      case JoinPointType.CONSTRUCTOR_EXECUTION_INT:
        reflectionInfo = calleeClassInfo.getConstructor(joinPointHash);
        pointcutType = PointcutType.EXECUTION;
        break;
      case JoinPointType.CONSTRUCTOR_CALL_INT:
        reflectionInfo = calleeClassInfo.getConstructor(joinPointHash);
        pointcutType = PointcutType.CALL;
        break;
      case JoinPointType.HANDLER_INT:
        reflectionInfo = calleeClassInfo;
        pointcutType = PointcutType.HANDLER;
        break;
      default:
        throw new RuntimeException("Joinpoint type not supported: " + joinPointType);
    }

    // create the caller info
    final ClassInfo callerClassInfo = JavaClassInfo.getClassInfo(callerClass);
    final ReflectionInfo withinInfo;
    if (TransformationConstants.CLINIT_METHOD_NAME.equals(callerMethodName)) {
      withinInfo = callerClassInfo.staticInitializer();
    } else if (TransformationConstants.INIT_METHOD_NAME.equals(callerMethodName)) {
      withinInfo = callerClassInfo.getConstructor(AsmHelper.calculateConstructorHash(callerMethodDesc));
    } else {
      withinInfo = callerClassInfo.getMethod(AsmHelper.calculateMethodHash(callerMethodName, callerMethodDesc));
    }

    // getDefault the compilation model
    final ExpressionContext ctx = new ExpressionContext(pointcutType, reflectionInfo, withinInfo);
    final AdviceInfoContainer adviceContainer = getAdviceInfoContainerForJoinPoint(ctx, loader, systemDef);
    final EmittedJoinPoint emittedJoinPoint =
        new EmittedJoinPoint(joinPointType, callerClass.getName(), callerMethodName, callerMethodDesc,
            callerMethodModifiers, calleeClass.getName(), calleeMemberName, calleeMemberDesc, calleeMemberModifiers,
            joinPointHash, joinPointClassName, EmittedJoinPoint.NO_LINE_NUMBER);
    final CompilationInfo.Model compilationModel =
        new CompilationInfo.Model(emittedJoinPoint, adviceContainer, callerClassInfo);

    return new CompiledJoinPoint(compilationModel);
  }

  /**
   * A compiled joinpoint is tied to a compilation model at a given time
   */
  public static class CompiledJoinPoint {
    public byte[]          bytecode;
    public CompilationInfo compilationInfo;

    public CompiledJoinPoint(CompilationInfo.Model model) {
      bytecode = CompilerHelper.compileJoinPoint(model);
      compilationInfo = new CompilationInfo(model);
    }
  }

  /**
   * Retrieves the advice info wrapped up in a struct.
   *
   * @param expressionContext
   * @param loader
   * @param systemDef (can be null)
   * @return the advice info
   */
  public static AdviceInfoContainer getAdviceInfoContainerForJoinPoint(final ExpressionContext expressionContext,
      final ClassLoader loader, final SystemDefinition systemDef) {
    final List beforeAdvices = new ArrayList();
    final List aroundAdvices = new ArrayList();
    final List afterFinallyAdvices = new ArrayList();
    final List afterReturningAdvices = new ArrayList();
    final List afterThrowingAdvices = new ArrayList();

    final Set systemDefinitions;
    if (systemDef == null) {
      systemDefinitions = SystemDefinitionContainer.getDefinitionsFor(loader);
    } else {
      systemDefinitions = new HashSet();
      systemDefinitions.add(systemDef);
    }
    // FIXME these two lines are needed to weave in other aspects on the classpath etc. - now causes verify error
//    for (Iterator it = SystemDefinitionContainer.getDefinitionsFor(loader).iterator(); it.hasNext();) {
//      definitions.add(it.next());
//    }

    for (Iterator iterator = systemDefinitions.iterator(); iterator.hasNext();) {
      SystemDefinition systemDefinition = (SystemDefinition) iterator.next();
      Collection aspects = systemDefinition.getAspectDefinitions();
      for (Iterator iterator1 = aspects.iterator(); iterator1.hasNext();) {
        AspectDefinition aspectDefinition = (AspectDefinition) iterator1.next();
        if (aspectDefinition.getName().equals(Virtual.class.getName())) {
          continue;
        }

        // TODO - do we care about non bounded pointcut ?
        for (Iterator iterator2 = aspectDefinition.getAdviceDefinitions().iterator(); iterator2.hasNext();) {
          AdviceDefinition adviceDefinition = (AdviceDefinition) iterator2.next();
          final ExpressionInfo expressionInfo = adviceDefinition.getExpressionInfo();
          if (expressionInfo == null) {
            continue;
          }
          try {
            if (expressionInfo.getExpression().match(expressionContext)) {
              // compute the target method to advice method arguments map, and grab information about this
              // and target bindings
              expressionContext.resetRuntimeState();
              ArgsIndexVisitor.updateContextForRuntimeInformation(expressionInfo, expressionContext, loader);
              // Note that the expressionContext dynamic information updated here should only be used
              // in the scope of this code block, since at the next iteration, the data will be
              // updated for another advice binding
              // [hence see setMethodArgumentIndexes below]

              // create a lightweight representation of the bounded advices to pass to the compiler
              final MethodInfo adviceMethodInfo = adviceDefinition.getMethodInfo();
              final AdviceInfo adviceInfo =
                  new AdviceInfo(aspectDefinition.getQualifiedName(), aspectDefinition.getClassName(), aspectDefinition
                      .getDeploymentModel(), adviceMethodInfo.getName(), AsmHelper.getMethodDescriptor(adviceMethodInfo),
                      AsmHelper.getArgumentTypes(adviceMethodInfo), adviceDefinition.getType(), adviceDefinition
                          .getSpecialArgumentType(), adviceDefinition.getName(),
                      expressionContext.m_targetWithRuntimeCheck, expressionInfo, expressionContext, adviceDefinition);

              setMethodArgumentIndexes(expressionInfo, expressionContext, adviceInfo, loader);

              if (AdviceType.BEFORE.equals(adviceDefinition.getType())) {
                beforeAdvices.add(adviceInfo);
              } else if (AdviceType.AROUND.equals(adviceDefinition.getType())) {
                aroundAdvices.add(adviceInfo);
              } else if (AdviceType.AFTER_FINALLY.equals(adviceDefinition.getType())) {
                afterFinallyAdvices.add(adviceInfo);
              } else if (AdviceType.AFTER_RETURNING.equals(adviceDefinition.getType())) {
                afterReturningAdvices.add(adviceInfo);
              } else if (AdviceType.AFTER_THROWING.equals(adviceDefinition.getType())) {
                afterThrowingAdvices.add(adviceInfo);
              } else if (AdviceType.AFTER.equals(adviceDefinition.getType())) {
                afterFinallyAdvices.add(adviceInfo);// special case for "after only"
              }
            }
          } catch (NullPointerException e) {
            System.out.println("-------------------------> NPE: Expression - " + expressionInfo.getExpressionString());
            System.out.println("-------------------------> NPE: Context - " + expressionContext.getDebugString());
            throw e;
          }
        }
      }
    }

    final AdviceInfoContainer adviceInfoContainer =
        new AdviceInfoContainer(aroundAdvices, beforeAdvices, afterFinallyAdvices, afterReturningAdvices,
            afterThrowingAdvices);
    return adviceInfoContainer;
  }

  /**
   * Get the parameter names from a "method declaration" signature like pc(type a, type2 b) => 0:a, 1:b
   *
   * @param adviceName
   * @return the parameter names
   */
  public static String[] getParameterNames(final String adviceName) {
    int paren = adviceName.indexOf('(');
    List paramNames = new ArrayList();
    if (paren > 0) {
      String params = adviceName.substring(paren + 1, adviceName.lastIndexOf(')')).trim();
      String[] javaParameters = Strings.splitString(params, ",");
      for (int i = 0; i < javaParameters.length; i++) {
        String javaParameter = Strings.replaceSubString(javaParameters[i], "  ", " ").trim();
        String[] paramInfo = Strings.splitString(javaParameter, " ");
        // handles XML def where name is optional
        if (paramInfo.length == 2) {
          paramNames.add(paramInfo[1]);
        } else {
          paramNames.add("anonymous_" + i);
        }
      }
    }
    String[] paramNamesArray = new String[paramNames.size()];
    int index = 0;
    for (Iterator it = paramNames.iterator(); it.hasNext(); index++) {
      paramNamesArray[index] = (String) it.next();
    }
    return paramNamesArray;
  }

  /**
   * Sets the advice argument indexes map. <p/> Each advice arg is mapped to wether a system entity like
   * StaticJoinPoint, Rtti, this, target etc thru a specific index (see AdviceInfo), or to one of the advised member
   * arguments (thru args(..) binding).
   *
   * @param expressionInfo
   * @param ctx
   * @param adviceInfo
   * @param loader
   */
  private static void setMethodArgumentIndexes(final ExpressionInfo expressionInfo, final ExpressionContext ctx,
      final AdviceInfo adviceInfo, final ClassLoader loader) {
    // grab the parameters names
    String[] adviceArgNames = getParameterNames(adviceInfo.getName());

    // map them from the ctx info
    int[] adviceToTargetArgs = new int[adviceInfo.getMethodParameterTypes().length];
    for (int k = 0; k < adviceArgNames.length; k++) {
      String adviceArgName = adviceArgNames[k];
      int exprArgIndex = expressionInfo.getArgumentIndex(adviceArgName);
      if (exprArgIndex >= 0 && ctx.m_exprIndexToTargetIndex.containsKey(adviceArgName)) {
        adviceToTargetArgs[k] = ((Integer) ctx.m_exprIndexToTargetIndex.get(adviceArgName)).intValue();
      } else {
        // does not appears to be an argument of the advised target
        // It can be StaticJP / JP / This binding / Target binding
        final Type type = adviceInfo.getMethodParameterTypes()[k];
        if (isJoinPoint(type)) {
          adviceToTargetArgs[k] = AdviceInfo.JOINPOINT_ARG;
        } else if (isStaticJoinPoint(type)) {
          adviceToTargetArgs[k] = AdviceInfo.STATIC_JOINPOINT_ARG;
        } else if (isTarget(adviceArgName, ctx)) {
          adviceToTargetArgs[k] = AdviceInfo.TARGET_ARG;
        } else if (isThis(adviceArgName, ctx)) {
          adviceToTargetArgs[k] = AdviceInfo.THIS_ARG;
        } else if (isSpecialArgument(adviceArgName, expressionInfo)) {
          adviceToTargetArgs[k] = AdviceInfo.SPECIAL_ARGUMENT;
        } else if (isCustomJointPoint(type, loader)) {
          adviceToTargetArgs[k] = AdviceInfo.CUSTOM_JOIN_POINT_ARG;
        } else {
          throw new Error("Unbound advice parameter at index " + k + " in " + adviceInfo.getMethodName()
              + adviceInfo.getMethodSignature() + " named " + adviceArgName);
        }
      }
    }

    // support for old style advices in XML whose name does not contain the call signature
    if (adviceArgNames.length == 0) {
      AspectDefinition aspectDef = adviceInfo.getAdviceDefinition().getAspectDefinition();
      Type[] adviceArgTypes = adviceInfo.getMethodParameterTypes();
      for (int i = 0; i < adviceArgTypes.length; i++) {

        if (aspectDef.isAspectWerkzAspect()) {
          if (isJoinPoint(adviceArgTypes[i])) {
            adviceToTargetArgs[i] = AdviceInfo.JOINPOINT_ARG;
          } else if (isStaticJoinPoint(adviceArgTypes[i])) {
            adviceToTargetArgs[i] = AdviceInfo.STATIC_JOINPOINT_ARG;
          } else {
            throw new Error("Unbound unnamed advice parameter at index " + i + " in " + adviceInfo.getMethodSignature());
          }
        } else {
          // handles other models
          // custom proceed won't be recognized here but will be in the first block
          // logic (mandatory arg type declaration).
          // TODO refactor models or def to not use getAroundClosureClassInfo
          final AspectModel aspectModel = AspectModelManager.getModelFor(aspectDef.getAspectModel());
          final String superClassName = aspectModel.getAroundClosureClassInfo().getSuperClassName();
          final String[] interfaces = aspectModel.getAroundClosureClassInfo().getInterfaceNames();
          final String[] classNames = new String[interfaces.length + 1];
          classNames[0] = superClassName;
          for (int j = 1; j < interfaces.length + 1; j++) {
            classNames[j] = interfaces[j - 1];
          }

          final Type argType = adviceArgTypes[i];
          if (isValidAroundClosureType(argType, classNames)) {
            adviceToTargetArgs[i] = AdviceInfo.VALID_NON_AW_AROUND_CLOSURE_TYPE;
          } else if (isSpecialArgumentType(argType, adviceInfo)) {
            adviceToTargetArgs[i] = AdviceInfo.SPECIAL_ARGUMENT;
          }
        }
      }
    }

    adviceInfo.setMethodToArgIndexes(adviceToTargetArgs);
  }

  private static boolean isSpecialArgumentType(final Type argType, final AdviceInfo adviceInfo) {
    final String specialArgumentTypeDesc = adviceInfo.getSpecialArgumentTypeDesc();
    return specialArgumentTypeDesc != null && specialArgumentTypeDesc.equals(argType.getDescriptor());
  }

  private static boolean isValidAroundClosureType(final Type argType, final String[] closureTypeNames) {
    for (int i = 0; i < closureTypeNames.length; i++) {
      final String closureTypeName = closureTypeNames[i];
      if (closureTypeName != null && closureTypeName.equals(argType.getInternalName())) { return true; }
    }
    return false;
  }

  private static boolean isJoinPoint(final Type type) {
    return Type.getType(JoinPoint.class).getDescriptor().equals(type.getDescriptor());
  }

  private static boolean isStaticJoinPoint(final Type type) {
    return Type.getType(StaticJoinPoint.class).getDescriptor().equals(type.getDescriptor());
  }

  private static boolean isTarget(final String adviceArgName, final ExpressionContext ctx) {
    return adviceArgName.equals(ctx.m_targetBoundedName);
  }

  private static boolean isThis(final String adviceArgName, final ExpressionContext ctx) {
    return adviceArgName.equals(ctx.m_thisBoundedName);
  }

  private static boolean isSpecialArgument(final String adviceArgName, final ExpressionInfo expressionInfo) {
    return adviceArgName.equals(expressionInfo.getSpecialArgumentName());
  }

  private static boolean isCustomJointPoint(final Type type, final ClassLoader loader) {
    ClassInfo classInfo = AsmClassInfo.getClassInfo(type.getClassName(), loader);
    return ClassInfoHelper.implementsInterface(classInfo, ExpressionInfo.JOINPOINT_CLASS_NAME)
        || ClassInfoHelper.implementsInterface(classInfo, ExpressionInfo.STATIC_JOINPOINT_CLASS_NAME);
  }
}
