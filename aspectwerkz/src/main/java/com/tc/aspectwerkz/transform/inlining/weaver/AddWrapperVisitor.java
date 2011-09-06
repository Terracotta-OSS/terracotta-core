/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import com.tc.asm.Opcodes;
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

import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.lang.reflect.Modifier;

/**
 * Adds field and method and ctor wrappers when there has been at least one joinpoint emitted.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class AddWrapperVisitor extends ClassAdapter implements Opcodes, TransformationConstants {

  private InstrumentationContext m_context;

  private Set m_addedMethods;


  public AddWrapperVisitor(ClassVisitor classVisitor, InstrumentationContext context, Set alreadyAddedMethods) {
    super(classVisitor);
    m_context = (InstrumentationContext) context;
    m_addedMethods = alreadyAddedMethods;
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
  public void visit(final int version, final int access,
                    final String name,
                    final String signature,
                    final String superName,
                    final String[] interfaces) {
    // iterate on the emitted joinpoints
    // we don't need to filter more since the joinpoint type and the weaving phase did that for us
    List jps = m_context.getEmittedJoinPoints();
    for (Iterator iterator = jps.iterator(); iterator.hasNext();) {
      EmittedJoinPoint emittedJoinPoint = (EmittedJoinPoint) iterator.next();
      int jpType = emittedJoinPoint.getJoinPointType();
      if (Modifier.isPublic(emittedJoinPoint.getCalleeMemberModifiers())
              || !name.equals(emittedJoinPoint.getCalleeClassName())) {//TODO ?
        continue;
      }
      switch (jpType) {
        case (JoinPointType.FIELD_GET_INT) :
          createGetFieldWrapperMethod(
                  Modifier.isStatic(emittedJoinPoint.getCalleeMemberModifiers()),
                  name,
                  emittedJoinPoint.getCalleeMemberName(),
                  emittedJoinPoint.getCalleeMemberDesc(),
                  null//TODO generic is that ok ?
          );
          break;
        case (JoinPointType.FIELD_SET_INT) :
          createPutFieldWrapperMethod(
                  Modifier.isStatic(emittedJoinPoint.getCalleeMemberModifiers()),
                  name,
                  emittedJoinPoint.getCalleeMemberName(),
                  emittedJoinPoint.getCalleeMemberDesc(),
                  null//TODO generic is that ok ?
          );
          break;
        case (JoinPointType.METHOD_EXECUTION_INT) :
        case (JoinPointType.METHOD_CALL_INT) :
          createMethodWrapperMethod(
                  emittedJoinPoint.getCalleeMemberModifiers(),
                  name,
                  emittedJoinPoint.getCalleeMemberName(),
                  emittedJoinPoint.getCalleeMemberDesc(),
                  null,//TODO generic is that ok ?
                  EMPTY_STRING_ARRAY//TODO should throw Throwable ??
          );
          break;
        case (JoinPointType.CONSTRUCTOR_CALL_INT) :
        case (JoinPointType.CONSTRUCTOR_EXECUTION_INT) :
          createConstructorWrapperMethod(
                  emittedJoinPoint.getCalleeMemberModifiers(),
                  name,
                  emittedJoinPoint.getCalleeMemberName(),
                  emittedJoinPoint.getCalleeMemberDesc(),
                  null,//TODO generic is that ok ?
                  EMPTY_STRING_ARRAY//TODO should throw Throwable ??
          );
          break;
      }
    }

    super.visit(version, access, name, signature, superName, interfaces);
  }

  /**
   * Creates a public wrapper method that delegates to the GETFIELD instruction of the non-public field.
   *
   * @param isStaticField
   * @param declaringTypeName
   * @param name
   * @param desc
   * @param signature
   */
  private void createGetFieldWrapperMethod(final boolean isStaticField,
                                           final String declaringTypeName,
                                           final String name,
                                           final String desc,
                                           final String signature) {
    String wrapperName = TransformationUtil.getWrapperMethodName(
            name, desc, declaringTypeName, GETFIELD_WRAPPER_METHOD_PREFIX
    );

    StringBuffer wsignature = new StringBuffer();
    wsignature.append('(');
    wsignature.append(')');
    wsignature.append(desc);

    final String wrapperKey = AlreadyAddedMethodAdapter.getMethodKey(wrapperName, wsignature.toString());
    if (m_addedMethods.contains(wrapperKey)) {
      return;
    }
    m_addedMethods.add(wrapperKey);

    int modifiers = ACC_SYNTHETIC;
    if (isStaticField) {
      modifiers |= ACC_STATIC;
    }

    MethodVisitor mv = cv.visitMethod(
            modifiers,
            wrapperName,
            wsignature.toString(),
            signature,
            EMPTY_STRING_ARRAY
    );

    if (isStaticField) {
      mv.visitFieldInsn(GETSTATIC, declaringTypeName, name, desc);
    } else {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, declaringTypeName, name, desc);
    }

    AsmHelper.addReturnStatement(mv, Type.getType(desc));
    mv.visitMaxs(0, 0);
  }

  /**
   * Creates a public wrapper method that delegates to the PUTFIELD instruction of the non-public field.
   * Static method if field is static (PUTSTATIC instr)
   *
   * @param isStaticField
   * @param declaringTypeName
   * @param name
   * @param desc
   * @param signature
   */
  private void createPutFieldWrapperMethod(boolean isStaticField,
                                           final String declaringTypeName,
                                           final String name,
                                           final String desc,
                                           final String signature) {
    String wrapperName = TransformationUtil.getWrapperMethodName(
            name, desc, declaringTypeName, PUTFIELD_WRAPPER_METHOD_PREFIX
    );

    StringBuffer wsignature = new StringBuffer();
    wsignature.append('(');
    wsignature.append(desc);
    wsignature.append(')');
    wsignature.append('V');

    final String wrapperKey = AlreadyAddedMethodAdapter.getMethodKey(wrapperName, wsignature.toString());
    if (m_addedMethods.contains(wrapperKey)) {
      return;
    }
    m_addedMethods.add(wrapperKey);

    int modifiers = ACC_SYNTHETIC;
    if (isStaticField) {
      modifiers |= ACC_STATIC;
    }

    MethodVisitor mv = cv.visitMethod(
            modifiers,
            wrapperName,
            wsignature.toString(),
            signature,
            EMPTY_STRING_ARRAY
    );

    Type fieldType = Type.getType(desc);
    if (isStaticField) {
      AsmHelper.loadArgumentTypes(mv, new Type[]{fieldType}, true);
      mv.visitFieldInsn(PUTSTATIC, declaringTypeName, name, desc);
    } else {
      mv.visitVarInsn(ALOAD, 0);
      AsmHelper.loadArgumentTypes(mv, new Type[]{fieldType}, false);
      mv.visitFieldInsn(PUTFIELD, declaringTypeName, name, desc);
    }

    AsmHelper.addReturnStatement(mv, Type.VOID_TYPE);
    mv.visitMaxs(0, 0);
  }

  /**
   * Creates a public wrapper method that delegates to the non-public target method.
   *
   * @param access
   * @param declaringTypeName
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   */
  private void createMethodWrapperMethod(final int access,
                                         final String declaringTypeName,
                                         final String name,
                                         final String desc,
                                         final String signature,
                                         final String[] exceptions) {
    final String wrapperName = TransformationUtil.getWrapperMethodName(
            name, desc, declaringTypeName, INVOKE_WRAPPER_METHOD_PREFIX
    );

    final String wrapperKey = AlreadyAddedMethodAdapter.getMethodKey(wrapperName, desc);
    if (m_addedMethods.contains(wrapperKey)) {
      return;
    }
    m_addedMethods.add(wrapperKey);

    int modifiers = ACC_SYNTHETIC;
    if (Modifier.isStatic(access)) {
      modifiers |= ACC_STATIC;
    }

    MethodVisitor mv = super.visitMethod(
            modifiers,
            wrapperName,
            desc,
            signature,
            exceptions
    );

    if (Modifier.isStatic(access)) {
      AsmHelper.loadArgumentTypes(mv, Type.getArgumentTypes(desc), Modifier.isStatic(access));
      mv.visitMethodInsn(INVOKESTATIC, declaringTypeName, name, desc);
    } else {
      mv.visitVarInsn(ALOAD, 0);
      AsmHelper.loadArgumentTypes(mv, Type.getArgumentTypes(desc), Modifier.isStatic(access));
      mv.visitMethodInsn(INVOKEVIRTUAL, declaringTypeName, name, desc);
    }

    AsmHelper.addReturnStatement(mv, Type.getReturnType(desc));

    mv.visitMaxs(0, 0);
  }

  /**
   * Creates a public wrapper static method that delegates to the non-public target ctor.
   *
   * @param access
   * @param declaringTypeName
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   */
  private void createConstructorWrapperMethod(final int access,
                                              final String declaringTypeName,
                                              final String name,
                                              final String desc,
                                              final String signature,
                                              final String[] exceptions) {
    final String wrapperName = TransformationUtil.getWrapperMethodName(
            name, desc, declaringTypeName, INVOKE_WRAPPER_METHOD_PREFIX
    );

    final String wrapperKey = AlreadyAddedMethodAdapter.getMethodKey(wrapperName, desc);
    if (m_addedMethods.contains(wrapperKey)) {
      return;
    }
    m_addedMethods.add(wrapperKey);

    int modifiers = ACC_SYNTHETIC;
    modifiers |= ACC_STATIC;

    Type declaringType = Type.getType('L' + declaringTypeName + ';');
    String ctorDesc = Type.getMethodDescriptor(declaringType, Type.getArgumentTypes(desc));

    MethodVisitor mv = super.visitMethod(
            modifiers,
            wrapperName,
            ctorDesc,
            signature,
            exceptions
    );

    mv.visitTypeInsn(NEW, declaringTypeName);
    mv.visitInsn(DUP);
    AsmHelper.loadArgumentTypes(mv, Type.getArgumentTypes(desc), true);
    mv.visitMethodInsn(INVOKESPECIAL, declaringTypeName, name, desc);
    AsmHelper.addReturnStatement(mv, declaringType);

    mv.visitMaxs(0, 0);
  }

}
