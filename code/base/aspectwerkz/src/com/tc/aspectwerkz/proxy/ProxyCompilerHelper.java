/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.proxy;

import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.joinpoint.management.JoinPointManager;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.WeavingStrategy;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.transform.inlining.ProxyWeavingStrategy;
import com.tc.backport175.bytecode.AnnotationReader;
import com.tc.backport175.bytecode.spi.BytecodeProvider;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
 */
public class ProxyCompilerHelper {

  /**
   * The proxy weaving strategy.
   */
  private final static WeavingStrategy s_weavingStrategy = new ProxyWeavingStrategy();

  /**
   * Weaves and defines the newly generated proxy class.
   *
   * @param bytes
   * @param loader
   * @param proxyClassName
   * @param definition
   * @return
   */
  public static Class weaveAndDefineProxyClass(final byte[] bytes,
                                               final ClassLoader loader,
                                               final String proxyClassName,
                                               final SystemDefinition definition) {

    // register the bytecode provider
    // TODO AV - could be optimized ?(f.e. recompile everytime instead of creating many provider)
    AnnotationReader.setBytecodeProviderFor(proxyClassName, loader, new BytecodeProvider() {
      public byte[] getBytecode(final String className, final ClassLoader l) throws ClassNotFoundException, IOException {
        return bytes;
      }
    });

//    final Set definitions = new HashSet();
//    definitions.add(definition);

    final Set definitions;
    if (definition == null) {
      definitions = SystemDefinitionContainer.getDefinitionsFor(loader);
    } else {
      definitions = new HashSet();
      definitions.add(definition);

      for (Iterator it = SystemDefinitionContainer.getDefinitionsFor(loader).iterator(); it.hasNext();) {
        definitions.add(it.next());
      }
    }

    final InstrumentationContext ctx = new InstrumentationContext(
            proxyClassName,
            bytes,
            loader,
            definitions);
    ctx.markAsProxy();

    s_weavingStrategy.transform(proxyClassName, ctx);

    byte[] transformedBytes = ctx.getCurrentBytecode();

    // load and define the class
    return AsmHelper.defineClass(loader, transformedBytes, proxyClassName);
  }

  /**
   * Eagerly compiles and loads the JIT join point.
   *
   * @param proxyClass
   */
  public static void compileJoinPoint(final Class proxyClass, SystemDefinition definition) {
    try {
      Field field = proxyClass.getDeclaredField(TransformationConstants.EMITTED_JOINPOINTS_FIELD_NAME);
      field.setAccessible(true);

      HashMap emittedJoinPoints = (HashMap) field.get(null);

//      Object[] arr = emittedJoinPoints.getValues();
//      for (int i = 0; i < arr.length; i++) {
//        EmittedJoinPoint emittedJoinPoint = (EmittedJoinPoint) arr[i];
      for (Iterator it = emittedJoinPoints.values().iterator(); it.hasNext();) {
        EmittedJoinPoint emittedJoinPoint = (EmittedJoinPoint) it.next();
        JoinPointManager.loadJoinPoint(
                emittedJoinPoint.getJoinPointType(),
                proxyClass,
                emittedJoinPoint.getCallerMethodName(),
                emittedJoinPoint.getCallerMethodDesc(),
                emittedJoinPoint.getCallerMethodModifiers(),
                emittedJoinPoint.getCalleeClassName(),
                emittedJoinPoint.getCalleeMemberName(),
                emittedJoinPoint.getCalleeMemberDesc(),
                emittedJoinPoint.getCalleeMemberModifiers(),
                emittedJoinPoint.getJoinPointHash(),
                emittedJoinPoint.getJoinPointClassName(),
                definition);
      }
    } catch (NoSuchFieldException e) {
      // ignore
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }
  }
}
