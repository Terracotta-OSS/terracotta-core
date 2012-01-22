/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect.container;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Label;
import com.tc.asm.Type;

import com.tc.aspectwerkz.aspect.AspectContainer;
import com.tc.aspectwerkz.aspect.management.NoAspectBoundException;

/**
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class PerCflowXAspectFactoryCompiler extends LazyPerXFactoryCompiler {

  public PerCflowXAspectFactoryCompiler(String uuid, String aspectClassName, String aspectQualifiedName, String containerClassName, String rawParameters, ClassLoader loader) {
    super(uuid, aspectClassName, aspectQualifiedName, containerClassName, rawParameters, loader);
  }

  protected String getXSignature() {
    return "Ljava/lang/Thread;";
  }

  /**
   * Overrides the aspectOf() method to not do lazy aspect instantiation since controlled by bind()/unbind()
   */
  protected void createAspectOf() {
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            "ASPECTS",
            MAP_CLASS_SIGNATURE,
            null,
            null
    );

    m_clinit.visitTypeInsn(NEW, MAP_CLASS_NAME);
    m_clinit.visitInsn(DUP);
    m_clinit.visitMethodInsn(INVOKESPECIAL, "java/util/WeakHashMap", INIT_METHOD_NAME, "()V");
    m_clinit.visitFieldInsn(PUTSTATIC, m_aspectFactoryClassName, "ASPECTS", MAP_CLASS_SIGNATURE);

    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "aspectOf",
            "(" + getXSignature() + ")" + m_aspectClassSignature,
            null,
            null
    );

    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, "ASPECTS", MAP_CLASS_SIGNATURE);
    cv.visitVarInsn(ALOAD, 0);//Thread
    cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, "getDefault", "(Ljava/lang/Object;)Ljava/lang/Object;");
    cv.visitVarInsn(ASTORE, 1);
    cv.visitVarInsn(ALOAD, 1);
    Label ifBound = new Label();
    cv.visitJumpInsn(IFNONNULL, ifBound);
    cv.visitTypeInsn(NEW, Type.getInternalName(NoAspectBoundException.class));
    cv.visitInsn(DUP);
    cv.visitLdcInsn("Not bound");
    cv.visitLdcInsn(m_aspectQualifiedName);
    cv.visitMethodInsn(
            INVOKESPECIAL,
            NO_ASPECT_BOUND_EXCEPTION_CLASS_NAME,
            INIT_METHOD_NAME,
            "(Ljava/lang/String;Ljava/lang/String;)V"
    );
    cv.visitInsn(ATHROW);

    cv.visitLabel(ifBound);
    cv.visitVarInsn(ALOAD, 1);
    cv.visitTypeInsn(CHECKCAST, m_aspectClassName);
    cv.visitInsn(ARETURN);

    // create an implicit Thread.currentThread no arg aspectOf() method
    cv = m_cw.visitMethod(
            ACC_STATIC + ACC_PUBLIC + ACC_FINAL,
            "aspectOf",
            "()" + m_aspectClassSignature,
            null,
            null
    );
    cv.visitMethodInsn(
            INVOKESTATIC, Type.getInternalName(Thread.class), "currentThread", "()Ljava/lang/Thread;"
    );
    cv.visitMethodInsn(
            INVOKESTATIC, m_aspectFactoryClassName, "aspectOf", "(Ljava/lang/Thread;)" + getXSignature()
    );
    cv.visitInsn(ARETURN);
    cv.visitMaxs(0, 0);
  }

  protected void createOtherArtifacts() {
    createBindMethod();
    createUnbindMethod();
  }

  private void createBindMethod() {
    //FIXME AVF do not bind if already present
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "bind",
            "(" + getXSignature() + ")V",
            null,
            null
    );

    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, "ASPECTS", MAP_CLASS_SIGNATURE);
    if (m_hasAspectContainer) {
      cv.visitFieldInsn(
              GETSTATIC, m_aspectFactoryClassName, "CONTAINER", Type.getDescriptor(AspectContainer.class)
      );
      cv.visitMethodInsn(
              INVOKEINTERFACE,
              Type.getInternalName(AspectContainer.class),
              "aspectOf",
              "(" + getXSignature() + ")Ljava/lang/Object;"
      );
      cv.visitTypeInsn(CHECKCAST, m_aspectClassName);
    } else {
      cv.visitTypeInsn(NEW, m_aspectClassName);
      cv.visitInsn(DUP);
      cv.visitMethodInsn(INVOKESPECIAL, m_aspectClassName, INIT_METHOD_NAME, "()V");
    }
    cv.visitVarInsn(ALOAD, 0);//Thread
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            MAP_CLASS_NAME,
            "put",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    );
    cv.visitInsn(POP);
    cv.visitInsn(RETURN);
    cv.visitMaxs(0, 0);
  }

  private void createUnbindMethod() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "unbind",
            "(" + getXSignature() + ")V",
            null,
            null
    );

    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, "ASPECTS", MAP_CLASS_SIGNATURE);
    cv.visitVarInsn(ALOAD, 0);//Thread
    cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, "remove", "(Ljava/lang/Object;)Ljava/lang/Object;");
    cv.visitInsn(POP);
    cv.visitInsn(RETURN);
    cv.visitMaxs(0, 0);
  }
}
