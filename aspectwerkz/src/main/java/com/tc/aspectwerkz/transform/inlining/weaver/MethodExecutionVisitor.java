/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;

import com.tc.asm.*;

import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.intercept.AdvisableImpl;
import com.tc.aspectwerkz.joinpoint.management.JoinPointType;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.TransformationUtil;
import com.tc.aspectwerkz.transform.inlining.AsmCopyAdapter;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.expression.ExpressionContext;

/**
 * Adds a "proxy method" to the methods that matches an <tt>execution</tt> pointcut as well as prefixing the "original
 * method".
 * <br/>
 * The proxy method calls the JPClass.invoke static method. The signature of the invoke method depends if the
 * target method is static or not as follow:
 * <pre>
 *      invoke(callee, args.., caller) // non static
 *      invoke(args.., caller) // static
 * </pre>
 * (The reason why is that it simplifies call pointcut stack management)
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class MethodExecutionVisitor extends ClassAdapter implements TransformationConstants {

  private final ClassInfo m_classInfo;
  private final InstrumentationContext m_ctx;
  private String m_declaringTypeName;
  private final Set m_addedMethods;

  /**
   * Creates a new class adapter.
   *
   * @param cv
   * @param classInfo
   * @param ctx
   * @param addedMethods
   */
  public MethodExecutionVisitor(final ClassVisitor cv,
                                final ClassInfo classInfo,
                                final InstrumentationContext ctx,
                                final Set addedMethods) {
    super(cv);
    m_classInfo = classInfo;
    m_ctx = (InstrumentationContext) ctx;
    m_addedMethods = addedMethods;
  }

  /**
   * Visits the class.
   *
   * @param access
   * @param name
   * @param signature
   * @param superName
   * @param interfaces
   */
  public void visit(final int version,
                    final int access,
                    final String name,
                    final String signature,
                    final String superName,
                    final String[] interfaces) {
    m_declaringTypeName = name;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  /**
   * Visits the methods.
   *
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   * @return
   */
  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {

    if (INIT_METHOD_NAME.equals(name) ||
            CLINIT_METHOD_NAME.equals(name) ||
            name.startsWith(ASPECTWERKZ_PREFIX) ||
            name.startsWith(SYNTHETIC_MEMBER_PREFIX) ||
            name.startsWith(WRAPPER_METHOD_PREFIX) ||
            (AdvisableImpl.ADD_ADVICE_METHOD_NAME.equals(name) && AdvisableImpl.ADD_ADVICE_METHOD_DESC.equals(desc)) ||
            (AdvisableImpl.REMOVE_ADVICE_METHOD_NAME.equals(name) && AdvisableImpl.REMOVE_ADVICE_METHOD_DESC.equals(desc)))
    {
      return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    int hash = AsmHelper.calculateMethodHash(name, desc);
    MethodInfo methodInfo = m_classInfo.getMethod(hash);
    if (methodInfo == null) {
      System.err.println(
              "AW::WARNING " +
                      "metadata structure could not be build for method ["
                      + m_classInfo.getName().replace('/', '.')
                      + '.' + name + ':' + desc + ']'
      );
      // bail out
      return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    ExpressionContext ctx = new ExpressionContext(PointcutType.EXECUTION, methodInfo, methodInfo);

    if (methodFilter(m_ctx.getDefinitions(), ctx, methodInfo)) {
      return cv.visitMethod(access, name, desc, signature, exceptions);
    } else {
      String prefixedOriginalName = TransformationUtil.getPrefixedOriginalMethodName(name, m_declaringTypeName);
      if (m_addedMethods.contains(AlreadyAddedMethodAdapter.getMethodKey(prefixedOriginalName, desc))) {
        return cv.visitMethod(access, name, desc, signature, exceptions);
      }

      m_ctx.markAsAdvised();

      // create the proxy for the original method
      final MethodVisitor proxyMethod = createProxyMethod(access, name, desc, signature, exceptions, methodInfo);

      int modifiers = ACC_SYNTHETIC;
      if (Modifier.isStatic(access)) {
        modifiers |= ACC_STATIC;
      }
      // prefix the original method and make sure we copy method annotations to the proxyMethod
      // while keeping the body for the prefixed method
      return new MethodAdapter(cv.visitMethod(modifiers, prefixedOriginalName, desc, signature, exceptions)) {
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
          return new AsmCopyAdapter.CopyAnnotationAdapter(
                  super.visitAnnotation(desc, visible),
                  proxyMethod.visitAnnotation(desc, visible)
          );
        }

        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
          return new AsmCopyAdapter.CopyAnnotationAdapter(
                  super.visitParameterAnnotation(parameter, desc, visible),
                  proxyMethod.visitParameterAnnotation(parameter, desc, visible)
          );
        }

        public void visitAttribute(Attribute attr) {
          super.visitAttribute(attr);
          proxyMethod.visitAttribute(attr);
        }
      };
    }
  }

  /**
   * Creates the "proxy method", e.g. the method that has the same name and signature as the original method but a
   * completely other implementation.
   *
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   * @param methodInfo
   * @return the method visitor
   */
  private MethodVisitor createProxyMethod(final int access,
                                          final String name,
                                          final String desc,
                                          final String signature,
                                          final String[] exceptions,
                                          final MethodInfo methodInfo) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

    // load "this" ie callee if target method is not static
    if (!Modifier.isStatic(access)) {
      mv.visitVarInsn(ALOAD, 0);
    }
    // load args
    AsmHelper.loadArgumentTypes(mv, Type.getArgumentTypes(desc), Modifier.isStatic(access));
    // load "this" ie caller or null if method is static
    if (Modifier.isStatic(access)) {
      mv.visitInsn(ACONST_NULL);
    } else {
      mv.visitVarInsn(ALOAD, 0);
    }

    int joinPointHash = AsmHelper.calculateMethodHash(name, desc);
    String joinPointClassName = TransformationUtil.getJoinPointClassName(
            m_declaringTypeName,
            name,
            desc,
            m_declaringTypeName,
            JoinPointType.METHOD_EXECUTION_INT,
            joinPointHash
    );

    // TODO: should we provide some sort of option to do JITgen when weaving instead of when loading ?
    // use case: offline full packaging and alike

    mv.visitMethodInsn(
            INVOKESTATIC,
            joinPointClassName,
            INVOKE_METHOD_NAME,
            TransformationUtil.getInvokeSignatureForCodeJoinPoints(
                    access, desc, m_declaringTypeName, m_declaringTypeName
            )
    );

    AsmHelper.addReturnStatement(mv, Type.getReturnType(desc));
    mv.visitMaxs(0, 0);

    // emit the joinpoint
    m_ctx.addEmittedJoinPoint(
            new EmittedJoinPoint(
                    JoinPointType.METHOD_EXECUTION_INT,
                    m_declaringTypeName,
                    name,
                    desc,
                    access,
                    m_declaringTypeName,
                    name,
                    desc,
                    access,
                    joinPointHash,
                    joinPointClassName,
                    EmittedJoinPoint.NO_LINE_NUMBER
            )
    );

    return mv;
  }

  /**
   * Filters out the methods that are not eligible for transformation.
   *
   * @param definitions
   * @param ctx
   * @param methodInfo
   * @return boolean true if the method should be filtered out
   */
  public static boolean methodFilter(final Set definitions,
                                     final ExpressionContext ctx,
                                     final MethodInfo methodInfo) {
    if (Modifier.isAbstract(methodInfo.getModifiers())
            || Modifier.isNative(methodInfo.getModifiers())
            || methodInfo.getName().equals(INIT_METHOD_NAME)
            || methodInfo.getName().equals(CLINIT_METHOD_NAME)
            || methodInfo.getName().startsWith(ORIGINAL_METHOD_PREFIX)) {
      return true;
    }
    for (Iterator it = definitions.iterator(); it.hasNext();) {
      if (((SystemDefinition) it.next()).hasPointcut(ctx)) {
        return false;
      } else {
        continue;
      }
    }
    return true;
  }
}
