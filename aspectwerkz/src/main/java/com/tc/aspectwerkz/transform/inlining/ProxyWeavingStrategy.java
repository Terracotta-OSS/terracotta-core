/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;

import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.WeavingStrategy;
import com.tc.aspectwerkz.transform.inlining.weaver.AddInterfaceVisitor;
import com.tc.aspectwerkz.transform.inlining.weaver.AddMixinMethodsVisitor;
import com.tc.aspectwerkz.transform.inlining.weaver.AddWrapperVisitor;
import com.tc.aspectwerkz.transform.inlining.weaver.JoinPointInitVisitor;
import com.tc.aspectwerkz.transform.inlining.weaver.MethodExecutionVisitor;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.PointcutType;

/**
 * A weaving strategy implementing a weaving scheme based on statical compilation, and no reflection.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bon&#233;r </a>
 */
public class ProxyWeavingStrategy implements WeavingStrategy {

  /**
   * Performs the weaving of the target class.
   *
   * @param className
   * @param context
   */
  public void transform(String className, final InstrumentationContext context) {
    try {
      final byte[] bytecode = context.getInitialBytecode();
      final ClassLoader loader = context.getLoader();

      ClassInfo classInfo = AsmClassInfo.getClassInfo(className, bytecode, loader);

      final Set definitions = context.getDefinitions();
      final ExpressionContext[] ctxs = new ExpressionContext[]{
              new ExpressionContext(PointcutType.EXECUTION, classInfo, classInfo)
      };

      // has AW aspects?
      final boolean isAdvisable = !classFilter(definitions, ctxs, classInfo);

      if (!isAdvisable) {
        context.setCurrentBytecode(context.getInitialBytecode());
        return;
      }

      // prepare ctor call jp
      Set addedMethods = new HashSet();
      // final ClassReader crLookahead = new ClassReader(bytecode);
      // crLookahead.accept(new AlreadyAddedMethodAdapter(addedMethods), true);

      // ------------------------------------------------
      // -- Phase 1 -- type change (ITDs)
      final ClassReader readerPhase1 = new ClassReader(bytecode);
      final ClassWriter writerPhase1 = new ClassWriter(readerPhase1, ClassWriter.COMPUTE_MAXS);
      ClassVisitor reversedChainPhase1 = writerPhase1;
      reversedChainPhase1 = new AddMixinMethodsVisitor(reversedChainPhase1, classInfo, context, addedMethods);
      reversedChainPhase1 = new AddInterfaceVisitor(reversedChainPhase1, classInfo, context);
      readerPhase1.accept(reversedChainPhase1, ClassReader.SKIP_FRAMES);
      context.setCurrentBytecode(writerPhase1.toByteArray());

      // ------------------------------------------------
      // update the class info with new ITDs
      classInfo = AsmClassInfo.newClassInfo(className, context.getCurrentBytecode(), loader);

      // ------------------------------------------------
      // -- Phase 2 -- advice
      final ClassReader readerPhase2 = new ClassReader(context.getCurrentBytecode());
      final ClassWriter writerPhase2 = new ClassWriter(readerPhase2, ClassWriter.COMPUTE_MAXS);
      ClassVisitor reversedChainPhase2 = writerPhase2;
//            reversedChainPhase2 = new InstanceLevelAspectVisitor(reversedChainPhase2, classInfo, context);
      reversedChainPhase2 = new MethodExecutionVisitor(reversedChainPhase2, classInfo, context, addedMethods);
//            reversedChainPhase2 = new LabelToLineNumberVisitor(reversedChainPhase2, context);
      readerPhase2.accept(reversedChainPhase2, 0);
      context.setCurrentBytecode(writerPhase2.toByteArray());

      // ------------------------------------------------
      // -- AW Finalization -- JP init code and wrapper methods
      if (context.isAdvised()) {
        ClassReader readerPhase3 = new ClassReader(context.getCurrentBytecode());
        final ClassWriter writerPhase3 = new ClassWriter(readerPhase3, ClassWriter.COMPUTE_MAXS);
        ClassVisitor reversedChainPhase3 = writerPhase3;
        reversedChainPhase3 = new AddWrapperVisitor(reversedChainPhase3, context, addedMethods);
        reversedChainPhase3 = new JoinPointInitVisitor(reversedChainPhase3, context);
        readerPhase3.accept(reversedChainPhase3, 0);
        context.setCurrentBytecode(writerPhase3.toByteArray());
      }
    } catch (Throwable t) {
      t.printStackTrace();
      throw new WrappedRuntimeException(t);
    }
  }

  /**
   * Filters out the classes that are not eligible for transformation.
   *
   * @param definitions the definitions
   * @param ctxs        an array with the contexts
   * @param classInfo   the class to filter
   * @return boolean true if the class should be filtered out
   */
  private static boolean classFilter(final Set definitions, final ExpressionContext[] ctxs, final ClassInfo classInfo) {
    if (classInfo.isInterface()) {
      return true;
    }
    for (Iterator defs = definitions.iterator(); defs.hasNext();) {
      if (classFilter((SystemDefinition) defs.next(), ctxs, classInfo)) {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * Filters out the classes that are not eligible for transformation.
   *
   * @param definition the definition
   * @param ctxs       an array with the contexts
   * @param classInfo  the class to filter
   * @return boolean true if the class should be filtered out
   * @TODO: when a class had execution pointcut that were removed it must be unweaved, thus not filtered out How to
   * handle that? cache lookup? or custom class level attribute ?
   */
  private static boolean classFilter(final SystemDefinition definition, final ExpressionContext[] ctxs,
                                     final ClassInfo classInfo) {

    if (classInfo.isInterface()) {
      return true;
    }
    String className = classInfo.getName();
    if (definition.inExcludePackage(className)) {
      return true;
    }
    if (!definition.inIncludePackage(className)) {
      return true;
    }
    if (definition.isAdvised(ctxs)) {
      return false;
    }
    if (definition.hasMixin(ctxs)) {
      return false;
    }
    if (definition.hasIntroducedInterface(ctxs)) {
      return false;
    }
    if (definition.inPreparePackage(className)) {
      return false;
    }
    return true;
  }

//  private static boolean classFilterFor(final Set definitions, final ExpressionContext[] ctxs) {
//    for (Iterator defs = definitions.iterator(); defs.hasNext();) {
//      if (classFilterFor((SystemDefinition) defs.next(), ctxs)) {
//        continue;
//      } else {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  private static boolean classFilterFor(final SystemDefinition definition, final ExpressionContext[] ctxs) {
//    if (definition.isAdvised(ctxs)) {
//      return false;
//    }
//    return true;
//  }
//
//  private static boolean hasPointcut(final Set definitions, final ExpressionContext ctx) {
//    for (Iterator defs = definitions.iterator(); defs.hasNext();) {
//      if (hasPointcut((SystemDefinition) defs.next(), ctx)) {
//        return true;
//      } else {
//        continue;
//      }
//    }
//    return false;
//  }
//
//  private static boolean hasPointcut(final SystemDefinition definition, final ExpressionContext ctx) {
//    if (definition.hasPointcut(ctx)) {
//      return true;
//    }
//    return false;
//  }
}
