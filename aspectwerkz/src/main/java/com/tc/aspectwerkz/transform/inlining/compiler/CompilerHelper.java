/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.compiler;

import java.util.Map;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.Set;

import com.tc.aspectwerkz.joinpoint.management.JoinPointType;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionInfo;

/**
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class CompilerHelper {

  /**
   * Stores the compilation infos - mapped to the last compiled join point class based on this compilation info.
   */
  private static final Map COMPILATION_INFO_REPOSITORY = new WeakHashMap();

  /**
   * Compiles and loades a join point class, one specific class for each distinct join point.
   *
   * @param model  the model for the compilation
   * @param loader the class loader that the compiled join point should live in
   * @return the compiled join point class
   */
  public static Class compileJoinPointAndAttachToClassLoader(final CompilationInfo.Model model,
                                                             final ClassLoader loader) {
    return attachToClassLoader(model.getJoinPointClassName(), loader, compileJoinPoint(model));
  }

  /**
   * Loads a join point class, one specific class for each distinct join point.
   *
   * @param joinpointClassName
   * @param loader             the class loader that the compiled join point should live in
   * @param bytecode           of the joinpoint
   * @return the compiled join point class
   */
  public static Class attachToClassLoader(final String joinpointClassName,
                                          final ClassLoader loader,
                                          final byte[] bytecode) {
    return AsmHelper.defineClass(loader, bytecode, joinpointClassName);
  }

  /**
   * Adds or updates a compilation info. The class key is always the first compiled join point class.
   *
   * @param clazz
   * @param compilationInfo
   */
  public static void addCompilationInfo(final Class clazz, final CompilationInfo compilationInfo) {
    COMPILATION_INFO_REPOSITORY.put(clazz, compilationInfo);
  }

  /**
   * Compiles a join point class, one specific class for each distinct join point.
   *
   * @param model the model for the compilation
   * @return the compiled join point bytecode
   */
  public static byte[] compileJoinPoint(final CompilationInfo.Model model) {
    switch (model.getEmittedJoinPoint().getJoinPointType()) {
      case JoinPointType.METHOD_EXECUTION_INT:
        return new MethodExecutionJoinPointCompiler(model).compile();
      case JoinPointType.METHOD_CALL_INT:
        return new MethodCallJoinPointCompiler(model).compile();
      case JoinPointType.CONSTRUCTOR_EXECUTION_INT:
        return new ConstructorExecutionJoinPointCompiler(model).compile();
      case JoinPointType.CONSTRUCTOR_CALL_INT:
        return new ConstructorCallJoinPointCompiler(model).compile();
      case JoinPointType.FIELD_SET_INT:
        return new FieldSetJoinPointCompiler(model).compile();
      case JoinPointType.FIELD_GET_INT:
        return new FieldGetJoinPointCompiler(model).compile();
      case JoinPointType.HANDLER_INT:
        return new HandlerJoinPointCompiler(model).compile();
      case JoinPointType.STATIC_INITIALIZATION_INT:
        return new StaticInitializationJoinPointCompiler(model).compile();
      default:
        throw new UnsupportedOperationException(
                "join point type is not supported: " + model.getEmittedJoinPoint().getJoinPointType()
        );
    }
  }

  /**
   * Redefines the originally compiled join point.
   *
   * @param compilationInfo the model for the compilation
   * @return the compiled join point bytecode
   */
  public static byte[] redefineJoinPoint(final CompilationInfo compilationInfo) {
    switch (compilationInfo.getInitialModel().getEmittedJoinPoint().getJoinPointType()) {
      case JoinPointType.METHOD_EXECUTION_INT:
        return new MethodExecutionJoinPointRedefiner(compilationInfo).compile();
      case JoinPointType.METHOD_CALL_INT:
        return new MethodCallJoinPointRedefiner(compilationInfo).compile();
      case JoinPointType.CONSTRUCTOR_EXECUTION_INT:
        return new ConstructorExecutionJoinPointRedefiner(compilationInfo).compile();
      case JoinPointType.CONSTRUCTOR_CALL_INT:
        return new ConstructorCallJoinPointRedefiner(compilationInfo).compile();
      case JoinPointType.FIELD_SET_INT:
        return new FieldSetJoinPointRedefiner(compilationInfo).compile();
      case JoinPointType.FIELD_GET_INT:
        return new FieldGetJoinPointRedefiner(compilationInfo).compile();
      case JoinPointType.HANDLER_INT:
        return new HandlerJoinPointRedefiner(compilationInfo).compile();
      default:
        throw new UnsupportedOperationException(
                "join point type is not supported: " +
                        compilationInfo.getInitialModel().getEmittedJoinPoint().getJoinPointType()
        );
    }
  }

  /**
   * Returns a list with all the join point compilers that matches a specific pointcut expression.
   * <p/>
   * To be used for redefinition of the join point compilers only. This since the compilers must have been created
   * in advance to exist in the repository (which is done when the target class is loaded).
   *
   * @param expression the pointcut expression
   * @return a set with the matching emitted join point
   */
  public static Set getJoinPointsMatching(final ExpressionInfo expression) {
    final Set matchingJoinPointInfos = new HashSet();
    for (Iterator it = COMPILATION_INFO_REPOSITORY.entrySet().iterator(); it.hasNext();) {
      final Map.Entry entry = (Map.Entry) it.next();

      final Class clazz = (Class) entry.getKey();
      final CompilationInfo compilationInfo = (CompilationInfo) entry.getValue();
      final EmittedJoinPoint joinPoint = (EmittedJoinPoint) compilationInfo.
              getInitialModel().getEmittedJoinPoint();
      final ClassLoader loader = clazz.getClassLoader();

      final ClassInfo calleeClassInfo = AsmClassInfo.getClassInfo(joinPoint.getCalleeClassName(), loader);
      final ClassInfo callerClassInfo = AsmClassInfo.getClassInfo(joinPoint.getCallerClassName(), loader);
      final MethodInfo callerMethodInfo = getCallerMethodInfo(callerClassInfo, joinPoint);

      ExpressionContext ctx = null;
      switch (joinPoint.getJoinPointType()) {
        case JoinPointType.METHOD_EXECUTION_INT:
          ctx = new ExpressionContext(
                  PointcutType.EXECUTION,
                  calleeClassInfo.getMethod(joinPoint.getJoinPointHash()),
                  callerMethodInfo
          );
          break;
        case JoinPointType.METHOD_CALL_INT:
          ctx = new ExpressionContext(
                  PointcutType.CALL,
                  calleeClassInfo.getMethod(joinPoint.getJoinPointHash()),
                  callerMethodInfo
          );
          break;
        case JoinPointType.CONSTRUCTOR_EXECUTION_INT:
          ctx = new ExpressionContext(
                  PointcutType.EXECUTION,
                  calleeClassInfo.getConstructor(joinPoint.getJoinPointHash()),
                  callerMethodInfo
          );
          break;
        case JoinPointType.CONSTRUCTOR_CALL_INT:
          ctx = new ExpressionContext(
                  PointcutType.CALL,
                  calleeClassInfo.getConstructor(joinPoint.getJoinPointHash()),
                  callerMethodInfo
          );
          break;
        case JoinPointType.FIELD_SET_INT:
          ctx = new ExpressionContext(
                  PointcutType.SET,
                  calleeClassInfo.getField(joinPoint.getJoinPointHash()),
                  callerMethodInfo
          );
          break;
        case JoinPointType.FIELD_GET_INT:
          ctx = new ExpressionContext(
                  PointcutType.GET,
                  calleeClassInfo.getField(joinPoint.getJoinPointHash()),
                  callerMethodInfo
          );
          break;
        case JoinPointType.HANDLER_INT:
          ctx = new ExpressionContext(
                  PointcutType.HANDLER,
                  AsmClassInfo.getClassInfo(joinPoint.getCalleeClassName(), loader),
                  callerMethodInfo
          );
          break;
        case JoinPointType.STATIC_INITIALIZATION_INT:
          ctx = new ExpressionContext(
                  PointcutType.STATIC_INITIALIZATION,
                  calleeClassInfo.staticInitializer(),
                  calleeClassInfo
          );
      }
      if (expression.getExpression().match(ctx)) {
        matchingJoinPointInfos.add(new MatchingJoinPointInfo(clazz, compilationInfo, ctx));
      }
    }
    return matchingJoinPointInfos;
  }

  /**
   * Returns the emitted join point structure for a specific JIT generated join point class.
   *
   * @param clazz the join point class
   * @return the emitted join point structure
   */
  public static EmittedJoinPoint getEmittedJoinPoint(final Class clazz) {
    return (EmittedJoinPoint) COMPILATION_INFO_REPOSITORY.get(clazz);
  }

  /**
   * Grabs the caller method info.
   *
   * @param callerClassInfo
   * @param emittedJoinPoint
   * @return
   */
  private static MethodInfo getCallerMethodInfo(final ClassInfo callerClassInfo,
                                                final EmittedJoinPoint emittedJoinPoint) {
    MethodInfo callerMethodInfo = null;
    MethodInfo[] callerMethods = callerClassInfo.getMethods();
    for (int i = 0; i < callerMethods.length; i++) {
      MethodInfo method = callerMethods[i];
      if (method.getName().equals(emittedJoinPoint.getCallerMethodName()) &&
              method.getSignature().equals(emittedJoinPoint.getCallerMethodDesc())) {
        callerMethodInfo = method;
        break;
      }
    }
    return callerMethodInfo;
  }
}
