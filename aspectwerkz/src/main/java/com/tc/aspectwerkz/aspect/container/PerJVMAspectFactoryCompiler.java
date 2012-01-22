/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect.container;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Label;

/**
 * Simplest factory for perJVM aspects
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 * @author Jonas Bon&#233;r
 */
public class PerJVMAspectFactoryCompiler extends AbstractAspectFactoryCompiler {

  public PerJVMAspectFactoryCompiler(String uuid,
                                     String aspectClassName,
                                     String aspectQualifiedName,
                                     String containerClassName,
                                     String rawParameters,
                                     ClassLoader loader) {
    super(uuid, aspectClassName, aspectQualifiedName, containerClassName, rawParameters, loader);
  }

  protected void createAspectOf() {
    m_cw.visitField(
            ACC_PUBLIC + ACC_STATIC,
            FACTORY_SINGLE_ASPECT_FIELD_NAME,
            m_aspectClassSignature,
            null,
            null
    );

    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            FACTORY_ASPECTOF_METHOD_NAME,
            "()" + m_aspectClassSignature,
            null,
            null
    );

    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_SINGLE_ASPECT_FIELD_NAME, m_aspectClassSignature);
    Label ifNonNull = new Label();
    cv.visitJumpInsn(IFNONNULL, ifNonNull);
    if (m_hasAspectContainer) {
      cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_CONTAINER_FIELD_NAME, ASPECT_CONTAINER_CLASS_SIGNATURE);
      cv.visitMethodInsn(INVOKEINTERFACE, ASPECT_CONTAINER_CLASS_NAME, ASPECT_CONTAINER_ASPECTOF_METHOD_NAME, ASPECT_CONTAINER_ASPECTOF_PERJVM_METHOD_SIGNATURE);
      cv.visitTypeInsn(CHECKCAST, m_aspectClassName);
    } else {
      cv.visitTypeInsn(NEW, m_aspectClassName);
      cv.visitInsn(DUP);
      cv.visitMethodInsn(INVOKESPECIAL, m_aspectClassName, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    }
    cv.visitFieldInsn(PUTSTATIC, m_aspectFactoryClassName, FACTORY_SINGLE_ASPECT_FIELD_NAME, m_aspectClassSignature);
    cv.visitLabel(ifNonNull);
    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_SINGLE_ASPECT_FIELD_NAME, m_aspectClassSignature);
    cv.visitInsn(ARETURN);
    cv.visitMaxs(0, 0);
  }

  protected void createHasAspect() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_STATIC + ACC_PUBLIC + ACC_FINAL,
            FACTORY_HASASPECT_METHOD_NAME,
            NO_PARAM_RETURN_BOOLEAN_SIGNATURE,
            null,
            null
    );

    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_SINGLE_ASPECT_FIELD_NAME, m_aspectClassSignature);
    Label ifNonNull = new Label();
    cv.visitJumpInsn(IFNONNULL, ifNonNull);
    cv.visitInsn(ICONST_0);
    cv.visitInsn(IRETURN);
    cv.visitLabel(ifNonNull);
    cv.visitInsn(ICONST_1);
    cv.visitInsn(IRETURN);
    cv.visitMaxs(0, 0);
  }

  protected void createOtherArtifacts() {
    ;
  }
}
