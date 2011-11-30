/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Label;

import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.joinpoint.management.JoinPointType;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.TransformationUtil;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.PointcutType;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;

/**
 * Instruments method CALL join points by replacing INVOKEXXX instructions with invocations of the compiled join point.
 * <br/>
 * It calls the JPClass.invoke static method. The signature of the invoke method depends if the
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
public class MethodCallVisitor extends ClassAdapter implements TransformationConstants {

  private final InstrumentationContext m_ctx;
  private final ClassLoader m_loader;
  private final ClassInfo m_callerClassInfo;

  private Label m_lastLabelForLineNumber = EmittedJoinPoint.NO_LINE_NUMBER;

  /**
   * Creates a new instance.
   *
   * @param cv
   * @param loader
   * @param classInfo
   * @param ctx
   */
  public MethodCallVisitor(final ClassVisitor cv,
                           final ClassLoader loader,
                           final ClassInfo classInfo,
                           final InstrumentationContext ctx) {
    super(cv);
    m_loader = loader;
    m_callerClassInfo = classInfo;
    m_ctx = ctx;
  }

  /**
   * Visits the caller methods.
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

    if (name.startsWith(WRAPPER_METHOD_PREFIX) ||
            Modifier.isNative(access) ||
            Modifier.isAbstract(access)) {
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    return mv == null ? null : new ReplaceInvokeInstructionCodeAdapter(
            mv,
            m_loader,
            m_callerClassInfo,
            m_ctx.getClassName(),
            name,
            desc
    );
  }

  /**
   * Replaces 'INVOKEXXX' instructions with a call to the compiled JoinPoint instance.
   *
   * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
   * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
   */
  public class ReplaceInvokeInstructionCodeAdapter extends AfterObjectInitializationCodeAdapter {

    private final ClassLoader m_loader;
    private final ClassInfo m_callerClassInfo;
    private final String m_callerClassName;
    private final String m_callerMethodName;
    private final String m_callerMethodDesc;
    private final MemberInfo m_callerMemberInfo;

    /**
     * Creates a new instance.
     *
     * @param ca
     * @param loader
     * @param callerClassInfo
     * @param callerClassName
     * @param callerMethodName
     * @param callerMethodDesc
     */
    public ReplaceInvokeInstructionCodeAdapter(final MethodVisitor ca,
                                               final ClassLoader loader,
                                               final ClassInfo callerClassInfo,
                                               final String callerClassName,
                                               final String callerMethodName,
                                               final String callerMethodDesc) {
      super(ca, callerMethodName);
      m_loader = loader;
      m_callerClassInfo = callerClassInfo;
      m_callerClassName = callerClassName;
      m_callerMethodName = callerMethodName;
      m_callerMethodDesc = callerMethodDesc;

      if (CLINIT_METHOD_NAME.equals(callerMethodName)) {
        m_callerMemberInfo = m_callerClassInfo.staticInitializer();
      } else if (INIT_METHOD_NAME.equals(callerMethodName)) {
        int hash = AsmHelper.calculateConstructorHash(m_callerMethodDesc);
        m_callerMemberInfo = m_callerClassInfo.getConstructor(hash);
      } else {
        int hash = AsmHelper.calculateMethodHash(m_callerMethodName, m_callerMethodDesc);
        m_callerMemberInfo = m_callerClassInfo.getMethod(hash);
      }
    }

    /**
     * Label
     *
     * @param label
     */
    public void visitLabel(Label label) {
      m_lastLabelForLineNumber = label;
      super.visitLabel(label);
    }

    /**
     * Visits 'INVOKEXXX' instructions.
     *
     * @param opcode
     * @param calleeClassName
     * @param calleeMethodName
     * @param calleeMethodDesc
     */
    public void visitMethodInsn(final int opcode,
                                String calleeClassName,
                                final String calleeMethodName,
                                final String calleeMethodDesc) {

      if (m_callerMemberInfo == null) {
        System.err.println(
                "AW::WARNING " +
                        "metadata structure could not be build for method ["
                        + m_callerClassInfo.getName().replace('/', '.')
                        + '.' + m_callerMethodName + ':' + m_callerMethodDesc + ']'
        );
        super.visitMethodInsn(opcode, calleeClassName, calleeMethodName, calleeMethodDesc);
        return;
      }

      if (INIT_METHOD_NAME.equals(calleeMethodName) ||
              CLINIT_METHOD_NAME.equals(calleeMethodName) ||
              calleeMethodName.startsWith(ASPECTWERKZ_PREFIX)
              || calleeClassName.endsWith(JOIN_POINT_CLASS_SUFFIX)) {
        super.visitMethodInsn(opcode, calleeClassName, calleeMethodName, calleeMethodDesc);
        return;
      }

      // check if we have a super.sameMethod() call
      if (opcode == INVOKESPECIAL
              && !calleeClassName.equals(m_callerClassName)
              && ClassInfoHelper.extendsSuperClass(m_callerClassInfo, calleeClassName.replace('/', '.'))) {
        super.visitMethodInsn(opcode, calleeClassName, calleeMethodName, calleeMethodDesc);
        return;
      }

      // check if object initialization has been reached
      if (!m_isObjectInitialized) {
        super.visitMethodInsn(opcode, calleeClassName, calleeMethodName, calleeMethodDesc);
        return;
      }

      int joinPointHash = AsmHelper.calculateMethodHash(calleeMethodName, calleeMethodDesc);

      ClassInfo classInfo = AsmClassInfo.getClassInfo(calleeClassName, m_loader);
      MethodInfo calleeMethodInfo = classInfo.getMethod(joinPointHash);

      if (calleeMethodInfo == null) {
        System.err.println(
                "AW::WARNING " +
                        "metadata structure could not be build for method ["
                        + classInfo.getName().replace('/', '.')
                        + '.' + calleeMethodName + ':' + calleeMethodDesc
                        + "] when parsing method ["
                        + m_callerClassInfo.getName() + '.' + m_callerMethodName + "(..)]"
        );
        // bail out
        super.visitMethodInsn(opcode, calleeClassName, calleeMethodName, calleeMethodDesc);
        return;
      }

      ExpressionContext ctx = new ExpressionContext(PointcutType.CALL, calleeMethodInfo, m_callerMemberInfo);

      if (methodFilter(m_ctx.getDefinitions(), ctx, calleeMethodInfo)) {
        super.visitMethodInsn(opcode, calleeClassName, calleeMethodName, calleeMethodDesc);
      } else {
        m_ctx.markAsAdvised();

        String joinPointClassName = TransformationUtil.getJoinPointClassName(
                m_callerClassName,
                m_callerMethodName,
                m_callerMethodDesc,
                calleeClassName,
                JoinPointType.METHOD_CALL_INT,
                joinPointHash
        );

        // load the caller instance (this), or null if in a static context
        // note that callee instance [optional] and args are already on the stack
        if (Modifier.isStatic(m_callerMemberInfo.getModifiers())) {
          visitInsn(ACONST_NULL);
        } else {
          visitVarInsn(ALOAD, 0);
        }

        // add the call to the join point
        super.visitMethodInsn(
                INVOKESTATIC,
                joinPointClassName,
                INVOKE_METHOD_NAME,
                TransformationUtil.getInvokeSignatureForCodeJoinPoints(
                        calleeMethodInfo.getModifiers(), calleeMethodDesc,
                        m_callerClassName, calleeClassName
                )
        );

        // emit the joinpoint
        //See AW-253 - we remember if we had an INVOKE INTERFACE opcode
        int modifiers = calleeMethodInfo.getModifiers();
        if (opcode == INVOKEINTERFACE) {
          modifiers = modifiers | MODIFIER_INVOKEINTERFACE;
        }
        m_ctx.addEmittedJoinPoint(
                new EmittedJoinPoint(
                        JoinPointType.METHOD_CALL_INT,
                        m_callerClassName,
                        m_callerMethodName,
                        m_callerMethodDesc,
                        m_callerMemberInfo.getModifiers(),
                        calleeClassName,
                        calleeMethodName,
                        calleeMethodDesc,
                        modifiers,
                        joinPointHash,
                        joinPointClassName,
                        m_lastLabelForLineNumber
                )
        );
      }
    }

    /**
     * Filters out the methods that are not eligible for transformation.
     * Do not filter on abstract callee method - needed for interface declared method call
     * (invokeinterface instr.)
     *
     * @param definitions
     * @param ctx
     * @param calleeMethodInfo
     * @return boolean true if the method should be filtered out
     */
    public boolean methodFilter(final Set definitions,
                                final ExpressionContext ctx,
                                final MethodInfo calleeMethodInfo) {
      if (calleeMethodInfo.getName().equals(INIT_METHOD_NAME) ||
              calleeMethodInfo.getName().equals(CLINIT_METHOD_NAME) ||
              calleeMethodInfo.getName().startsWith(ORIGINAL_METHOD_PREFIX)) {
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
}