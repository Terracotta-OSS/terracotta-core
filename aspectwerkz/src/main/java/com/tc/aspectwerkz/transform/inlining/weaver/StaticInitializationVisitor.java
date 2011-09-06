/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

import com.tc.aspectwerkz.joinpoint.management.JoinPointType;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.TransformationUtil;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;

import java.util.Set;

/**
 * Adds a "proxy method" to the <tt>&lt;clinit&gt;</tt> that matches an
 * <tt>staticinitialization</tt> pointcut as well as prefixing the "original method"
 * (see {@link com.tc.aspectwerkz.transform.TransformationUtil#getPrefixedOriginalClinitName(String)}).
 * <br/>
 *
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public class StaticInitializationVisitor extends ClassAdapter implements TransformationConstants {

  private final InstrumentationContext m_ctx;
  private String m_declaringTypeName;
  private final Set m_addedMethods;

  /**
   * Creates a new class adapter.
   *
   * @param cv
   * @param ctx
   * @param addedMethods already added methods by AW
   */
  public StaticInitializationVisitor(final ClassVisitor cv,
                                     final InstrumentationContext ctx,
                                     final Set addedMethods) {
    super(cv);
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
    if (!CLINIT_METHOD_NAME.equals(name)) {
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    String prefixedOriginalName = TransformationUtil.getPrefixedOriginalClinitName(m_declaringTypeName);
    if (m_addedMethods.contains(AlreadyAddedMethodAdapter.getMethodKey(prefixedOriginalName, CLINIT_METHOD_SIGNATURE)))
    {
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    m_ctx.markAsAdvised();

    // create the proxy for the original method
    createProxyMethod(access, name, desc, signature, exceptions);

    // prefix the original method
    return cv.visitMethod(access + ACC_PUBLIC, prefixedOriginalName, desc, signature, exceptions);
  }

  /**
   * Creates the "proxy method", e.g. the method that has the same name and
   * signature as the original method but a completely other implementation.
   *
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   */
  private void createProxyMethod(final int access,
                                 final String name,
                                 final String desc,
                                 final String signature,
                                 final String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

    //caller instance is null
    mv.visitInsn(ACONST_NULL);

    int joinPointHash = AsmHelper.calculateMethodHash(name, desc);
    String joinPointClassName = TransformationUtil
            .getJoinPointClassName(m_declaringTypeName,
                    name,
                    desc,
                    m_declaringTypeName,
                    JoinPointType.STATIC_INITIALIZATION_INT,
                    joinPointHash);

    mv.visitMethodInsn(INVOKESTATIC,
            joinPointClassName,
            INVOKE_METHOD_NAME,
            TransformationUtil.getInvokeSignatureForCodeJoinPoints(access,
                    desc,
                    m_declaringTypeName,
                    m_declaringTypeName));

    AsmHelper.addReturnStatement(mv, Type.VOID_TYPE);
    mv.visitMaxs(0, 0);

    // emit the joinpoint
    m_ctx.addEmittedJoinPoint(
            new EmittedJoinPoint(JoinPointType.STATIC_INITIALIZATION_INT,
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
  }
}
