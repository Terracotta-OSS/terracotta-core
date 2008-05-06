/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect.container;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Label;

/**
 * Abstract class for lazy model that instantiate the aspect as soon as aspectOf is called
 * (f.e. perThread, perClass)
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public abstract class LazyPerXFactoryCompiler extends AbstractAspectFactoryCompiler {

  public LazyPerXFactoryCompiler(String uuid, String aspectClassName, String aspectQualifiedName, String containerClassName, String rawParameters, ClassLoader loader) {
    super(uuid, aspectClassName, aspectQualifiedName, containerClassName, rawParameters, loader);
  }

  protected abstract String getXSignature();

  protected void createAspectOf() {
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            FACTORY_ASPECTS_FIELD_NAME,
            MAP_CLASS_SIGNATURE,
            null,
            null
    );

    m_clinit.visitTypeInsn(NEW, "java/util/WeakHashMap");
    m_clinit.visitInsn(DUP);
    m_clinit.visitMethodInsn(INVOKESPECIAL, "java/util/WeakHashMap", INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    m_clinit.visitFieldInsn(PUTSTATIC, m_aspectFactoryClassName, FACTORY_ASPECTS_FIELD_NAME, MAP_CLASS_SIGNATURE);

    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            FACTORY_ASPECTOF_METHOD_NAME,
            "(" + getXSignature() + ")" + m_aspectClassSignature,
            null,
            null
    );

    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_ASPECTS_FIELD_NAME, MAP_CLASS_SIGNATURE);
    cv.visitVarInsn(ALOAD, 0);//Class
    cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, "getDefault", "(Ljava/lang/Object;)Ljava/lang/Object;");
    cv.visitVarInsn(ASTORE, 1);
    cv.visitVarInsn(ALOAD, 1);
    Label ifBound = new Label();
    cv.visitJumpInsn(IFNONNULL, ifBound);
    if (m_hasAspectContainer) {
      cv.visitFieldInsn(
              GETSTATIC, m_aspectFactoryClassName, FACTORY_CONTAINER_FIELD_NAME, ASPECT_CONTAINER_CLASS_SIGNATURE
      );
      cv.visitMethodInsn(
              INVOKEINTERFACE,
              ASPECT_CONTAINER_CLASS_NAME,
              ASPECT_CONTAINER_ASPECTOF_METHOD_NAME,
              "(" + getXSignature() + ")Ljava/lang/Object;"
      );
      cv.visitTypeInsn(CHECKCAST, m_aspectClassName);
    } else {
      cv.visitTypeInsn(NEW, m_aspectClassName);
      cv.visitInsn(DUP);
      cv.visitMethodInsn(INVOKESPECIAL, m_aspectClassName, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    }
    cv.visitVarInsn(ASTORE, 2);
    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_ASPECTS_FIELD_NAME, MAP_CLASS_SIGNATURE);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitVarInsn(ALOAD, 2);
    cv.visitMethodInsn(
            INVOKEINTERFACE, MAP_CLASS_NAME, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    );
    cv.visitVarInsn(ALOAD, 2);
    cv.visitInsn(ARETURN);

    cv.visitLabel(ifBound);
    cv.visitVarInsn(ALOAD, 1);
    cv.visitTypeInsn(CHECKCAST, m_aspectClassName);
    cv.visitInsn(ARETURN);
    cv.visitMaxs(0, 0);
  }

  protected void createHasAspect() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_STATIC + ACC_PUBLIC + ACC_FINAL,
            FACTORY_HASASPECT_METHOD_NAME,
            "(" + getXSignature() + ")Z",
            null,
            null
    );

    cv.visitFieldInsn(GETSTATIC, m_aspectFactoryClassName, FACTORY_ASPECTS_FIELD_NAME, MAP_CLASS_SIGNATURE);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, "containsKey", "(Ljava/lang/Object;)Z");
    cv.visitInsn(IRETURN);
    cv.visitMaxs(0, 0);
  }

  protected void createOtherArtifacts() {
    ;
  }

  static class PerClassAspectFactoryCompiler extends LazyPerXFactoryCompiler {
    public PerClassAspectFactoryCompiler(String uuid, String aspectClassName, String aspectQualifiedName, String containerClassName, String rawParameters, ClassLoader loader) {
      super(uuid, aspectClassName, aspectQualifiedName, containerClassName, rawParameters, loader);
    }

    protected String getXSignature() {
      return "Ljava/lang/Class;";
    }
  }

  static class PerThreadAspectFactoryCompiler extends LazyPerXFactoryCompiler {
    public PerThreadAspectFactoryCompiler(String uuid, String aspectClassName, String aspectQualifiedName, String containerClassName, String rawParameters, ClassLoader loader) {
      super(uuid, aspectClassName, aspectQualifiedName, containerClassName, rawParameters, loader);
    }

    protected String getXSignature() {
      return "Ljava/lang/Thread;";
    }
  }

}
