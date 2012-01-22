/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect.container;

import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

import com.tc.aspectwerkz.aspect.management.NoAspectBoundException;

/**
 * Factory compiler for perThis perTarget and perInstance (lazy like) models.
 * All factories rely on HasInstanceLevelAspect interface.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class PerObjectFactoryCompiler extends AbstractAspectFactoryCompiler {

  public PerObjectFactoryCompiler(String uuid, String aspectClassName, String aspectQualifiedName, String containerClassName, String rawParameters, ClassLoader loader) {
    super(uuid, aspectClassName, aspectQualifiedName, containerClassName, rawParameters, loader);
  }

  protected void createAspectOf() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            FACTORY_ASPECTOF_METHOD_NAME,
            "(Ljava/lang/Object;)" + m_aspectClassSignature,
            null,
            null
    );

    // instanceOf check
    cv.visitVarInsn(ALOAD, 0);// object
    cv.visitTypeInsn(INSTANCEOF, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
    Label ifInstanceOf = new Label();
    cv.visitJumpInsn(IFNE, ifInstanceOf);
    cv.visitTypeInsn(NEW, Type.getInternalName(NoAspectBoundException.class));
    cv.visitInsn(DUP);
    cv.visitLdcInsn("Unimplemented interface");
    cv.visitLdcInsn(m_aspectQualifiedName);
    cv.visitMethodInsn(
            INVOKESPECIAL,
            NO_ASPECT_BOUND_EXCEPTION_CLASS_NAME,
            INIT_METHOD_NAME,
            "(Ljava/lang/String;Ljava/lang/String;)V"
    );
    cv.visitInsn(ATHROW);
    cv.visitLabel(ifInstanceOf);

    cv.visitVarInsn(ALOAD, 0);
    cv.visitTypeInsn(CHECKCAST, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_CLASS_FIELD_NAME, CLASS_CLASS_SIGNATURE);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME,
            INSTANCE_LEVEL_GETASPECT_METHOD_NAME,
            INSTANCE_LEVEL_GETASPECT_METHOD_SIGNATURE
    );
    cv.visitVarInsn(ASTORE, 1);
    cv.visitVarInsn(ALOAD, 1);
    Label ifBound = new Label();
    cv.visitJumpInsn(IFNONNULL, ifBound);
    cv.visitTypeInsn(NEW, NO_ASPECT_BOUND_EXCEPTION_CLASS_NAME);
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
    cv.visitMaxs(0, 0);
  }

  protected void createHasAspect() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC,
            FACTORY_HASASPECT_METHOD_NAME,
            "(Ljava/lang/Object;)Z",
            null,
            null
    );

    // instanceOf check
    cv.visitVarInsn(ALOAD, 0);// object
    cv.visitTypeInsn(INSTANCEOF, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
    Label ifInstanceOf = new Label();
    cv.visitJumpInsn(IFNE, ifInstanceOf);
    cv.visitInsn(ICONST_0);
    cv.visitInsn(IRETURN);
    cv.visitLabel(ifInstanceOf);

    cv.visitVarInsn(ALOAD, 0);
    cv.visitTypeInsn(CHECKCAST, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_CLASS_FIELD_NAME, CLASS_CLASS_SIGNATURE);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME,
            INSTANCE_LEVEL_HASASPECT_METHOD_NAME,
            INSTANCE_LEVEL_HASASPECT_METHOD_SIGNATURE
    );
    cv.visitInsn(IRETURN);
    cv.visitMaxs(0, 0);
  }

  protected void createOtherArtifacts() {
    createBindMethod();
  }

  private void createBindMethod() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "bind",
            "(Ljava/lang/Object;)Ljava/lang/Object;",
            null,
            null
    );

    // instanceOf check
    cv.visitVarInsn(ALOAD, 0);// object
    cv.visitTypeInsn(INSTANCEOF, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
    Label ifInstanceOf = new Label();
    cv.visitJumpInsn(IFNE, ifInstanceOf);
    cv.visitTypeInsn(NEW, NO_ASPECT_BOUND_EXCEPTION_CLASS_NAME);
    cv.visitInsn(DUP);
    cv.visitLdcInsn("Unimplemented interface");
    cv.visitLdcInsn(m_aspectQualifiedName);
    cv.visitMethodInsn(
            INVOKESPECIAL,
            NO_ASPECT_BOUND_EXCEPTION_CLASS_NAME,
            INIT_METHOD_NAME,
            "(Ljava/lang/String;Ljava/lang/String;)V"
    );
    cv.visitInsn(ATHROW);
    cv.visitLabel(ifInstanceOf);

    cv.visitVarInsn(ALOAD, 0);//object
    cv.visitTypeInsn(CHECKCAST, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_CLASS_FIELD_NAME, CLASS_CLASS_SIGNATURE);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME,
            INSTANCE_LEVEL_GETASPECT_METHOD_NAME,
            INSTANCE_LEVEL_GETASPECT_METHOD_SIGNATURE
    );
    cv.visitVarInsn(ASTORE, 1);
    cv.visitVarInsn(ALOAD, 1);
    Label ifAlreadyBound = new Label();
    cv.visitJumpInsn(IFNONNULL, ifAlreadyBound);

    // target instance and arg0 for bind call
    cv.visitVarInsn(ALOAD, 0);//object
    cv.visitTypeInsn(CHECKCAST, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_CLASS_FIELD_NAME, CLASS_CLASS_SIGNATURE);

    if (m_hasAspectContainer) {
      cv.visitFieldInsn(
              GETSTATIC, m_aspectFactoryClassName, FACTORY_CONTAINER_FIELD_NAME, ASPECT_CONTAINER_CLASS_SIGNATURE
      );
      cv.visitVarInsn(ALOAD, 0);//associated object
      cv.visitMethodInsn(
              INVOKEINTERFACE, ASPECT_CONTAINER_CLASS_NAME, ASPECT_CONTAINER_ASPECTOF_METHOD_NAME, "(Ljava/lang/Object;)Ljava/lang/Object;"
      );
      cv.visitTypeInsn(CHECKCAST, m_aspectClassName);
    } else {
      cv.visitTypeInsn(NEW, m_aspectClassName);
      cv.visitInsn(DUP);
      cv.visitMethodInsn(INVOKESPECIAL, m_aspectClassName, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    }
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME,
            INSTANCE_LEVEL_BINDASPECT_METHOD_NAME,
            INSTANCE_LEVEL_BINDASPECT_METHOD_SIGNATURE
    );
    cv.visitInsn(ARETURN);

    cv.visitLabel(ifAlreadyBound);
    cv.visitVarInsn(ALOAD, 1);
    cv.visitInsn(ARETURN);

    cv.visitMaxs(0, 0);
  }

  public static class PerInstanceFactoryCompiler extends PerObjectFactoryCompiler {

    public PerInstanceFactoryCompiler(String uuid, String aspectClassName, String aspectQualifiedName, String containerClassName, String rawParameters, ClassLoader loader) {
      super(uuid, aspectClassName, aspectQualifiedName, containerClassName, rawParameters, loader);
    }

    protected void createAspectOf() {
      MethodVisitor cv = m_cw.visitMethod(
              ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
              FACTORY_ASPECTOF_METHOD_NAME,
              "(Ljava/lang/Object;)" + m_aspectClassSignature,
              null,
              null
      );

      // instanceOf check
      cv.visitVarInsn(ALOAD, 0);// object
      cv.visitTypeInsn(INSTANCEOF, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
      Label ifInstanceOf = new Label();
      cv.visitJumpInsn(IFNE, ifInstanceOf);
      cv.visitTypeInsn(NEW, Type.getInternalName(NoAspectBoundException.class));
      cv.visitInsn(DUP);
      cv.visitLdcInsn("Unimplemented interface");
      cv.visitLdcInsn(m_aspectQualifiedName);
      cv.visitMethodInsn(
              INVOKESPECIAL,
              NO_ASPECT_BOUND_EXCEPTION_CLASS_NAME,
              INIT_METHOD_NAME,
              "(Ljava/lang/String;Ljava/lang/String;)V"
      );
      cv.visitInsn(ATHROW);
      cv.visitLabel(ifInstanceOf);

      cv.visitVarInsn(ALOAD, 0);
      cv.visitTypeInsn(CHECKCAST, HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME);
      cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_CLASS_FIELD_NAME, CLASS_CLASS_SIGNATURE);
      cv.visitMethodInsn(
              INVOKEINTERFACE,
              HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME,
              INSTANCE_LEVEL_GETASPECT_METHOD_NAME,
              INSTANCE_LEVEL_GETASPECT_METHOD_SIGNATURE
      );
      cv.visitVarInsn(ASTORE, 1);
      cv.visitVarInsn(ALOAD, 1);
      Label ifBound = new Label();
      cv.visitJumpInsn(IFNONNULL, ifBound);
      // no aspect bound yet - since perInstance is lazy delegate to bind
      cv.visitVarInsn(ALOAD, 0);
      cv.visitMethodInsn(
              INVOKESTATIC,
              m_aspectFactoryClassName,
              "bind",
              "(Ljava/lang/Object;)Ljava/lang/Object;"
      );
      cv.visitVarInsn(ASTORE, 1);
      cv.visitLabel(ifBound);
      cv.visitVarInsn(ALOAD, 1);
      cv.visitTypeInsn(CHECKCAST, m_aspectClassName);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }
  }
}
