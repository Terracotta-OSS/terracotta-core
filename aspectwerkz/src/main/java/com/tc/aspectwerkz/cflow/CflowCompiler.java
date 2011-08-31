/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.cflow;

import com.tc.asm.Opcodes;
import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Label;

import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.compiler.AbstractJoinPointCompiler;

/**
 * Compiler for the JIT cflow Aspect
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class CflowCompiler implements Opcodes, TransformationConstants {

  public final static String JIT_CFLOW_CLASS = "com/tc/aspectwerkz/cflow/Cflow_";
  private final static String ABSTRACT_CFLOW_CLASS = "com/tc/aspectwerkz/cflow/AbstractCflowSystemAspect";
  private final static String INSTANCE_CFLOW_FIELD_NAME = "INSTANCE";
  public static final String IN_CFLOW_METOD_NAME = "inCflow";
  public static final String IN_CFLOW_METOD_SIGNATURE = "()Z";
  public static final String CFLOW_ASPECTOF_METHOD_NAME = "aspectOf";

  /**
   * Checks if a class name (ASM style) is a cflow name
   *
   * @param className
   * @return
   */
  public static boolean isCflowClass(String className) {
    return className.indexOf(JIT_CFLOW_CLASS) >= 0;
  }

  /**
   * The jit cflow aspect class name (with /)
   */
  private final String m_className;

  /**
   * The jit cflow aspect class name (with /)
   */
  private final String m_classSignature;

  private ClassWriter m_cw;

  /**
   * private ctor
   *
   * @param cflowId
   */
  private CflowCompiler(int cflowId) {
    m_className = getCflowAspectClassName(cflowId);
    m_classSignature = "L" + m_className + ";";
  }

  /**
   * compile the jit cflow aspect
   *
   * @return bytecode for the concrete jit cflow aspect
   */
  private byte[] compile() {
    m_cw = AsmHelper.newClassWriter(true);

    // class extends AbstractCflowsystemAspect
    m_cw.visit(
            AsmHelper.JAVA_VERSION,
            ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC,
            m_className,
            null,
            ABSTRACT_CFLOW_CLASS,
            EMPTY_STRING_ARRAY
    );

    // static INSTANCE field
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            INSTANCE_CFLOW_FIELD_NAME,
            m_classSignature,
            null,
            null
    );

    // private ctor
    MethodVisitor ctor = m_cw.visitMethod(
            ACC_PRIVATE,
            INIT_METHOD_NAME,
            NO_PARAM_RETURN_VOID_SIGNATURE,
            null,
            EMPTY_STRING_ARRAY
    );
    // invoke the constructor of abstract
    ctor.visitVarInsn(ALOAD, 0);
    ctor.visitMethodInsn(INVOKESPECIAL, ABSTRACT_CFLOW_CLASS, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    ctor.visitInsn(RETURN);
    ctor.visitMaxs(0, 0);

    // static isInCflow() delegators
    MethodVisitor isInCflow = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC,
            IS_IN_CFLOW_METOD_NAME,
            IS_IN_CFLOW_METOD_SIGNATURE,
            null,
            EMPTY_STRING_ARRAY
    );
    isInCflow.visitFieldInsn(GETSTATIC, m_className, INSTANCE_CFLOW_FIELD_NAME, m_classSignature);
    Label isNull = new Label();
    isInCflow.visitJumpInsn(IFNULL, isNull);
    isInCflow.visitFieldInsn(GETSTATIC, m_className, INSTANCE_CFLOW_FIELD_NAME, m_classSignature);
    isInCflow.visitMethodInsn(INVOKEVIRTUAL, ABSTRACT_CFLOW_CLASS, IN_CFLOW_METOD_NAME, IN_CFLOW_METOD_SIGNATURE);
    isInCflow.visitInsn(IRETURN);
    isInCflow.visitLabel(isNull);
    isInCflow.visitInsn(ICONST_0);
    isInCflow.visitInsn(IRETURN);
    isInCflow.visitMaxs(0, 0);

    // static aspectOf()
    MethodVisitor aspectOf = m_cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC,
            CFLOW_ASPECTOF_METHOD_NAME,
            "()" + m_classSignature,
            null,
            EMPTY_STRING_ARRAY
    );
    aspectOf.visitFieldInsn(GETSTATIC, m_className, INSTANCE_CFLOW_FIELD_NAME, m_classSignature);
    Label isNotNull = new Label();
    aspectOf.visitJumpInsn(IFNONNULL, isNotNull);
    aspectOf.visitTypeInsn(NEW, m_className);
    aspectOf.visitInsn(DUP);
    aspectOf.visitMethodInsn(INVOKESPECIAL, m_className, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    aspectOf.visitFieldInsn(PUTSTATIC, m_className, INSTANCE_CFLOW_FIELD_NAME, m_classSignature);
    aspectOf.visitLabel(isNotNull);
    aspectOf.visitFieldInsn(GETSTATIC, m_className, INSTANCE_CFLOW_FIELD_NAME, m_classSignature);
    aspectOf.visitInsn(ARETURN);
    aspectOf.visitMaxs(0, 0);

    m_cw.visitEnd();

    return m_cw.toByteArray();
  }

  /**
   * The naming strategy for jit cflow aspect
   *
   * @param cflowID
   * @return com.tc.aspectwerkz.cflow.Cflow_cflowID
   */
  public static String getCflowAspectClassName(int cflowID) {
    return JIT_CFLOW_CLASS + cflowID;
  }

  /**
   * If necessary, compile a jit cflow aspect and attach it to the given classloader
   *
   * @param loader
   * @param cflowID
   * @return
   */
  public static Class compileCflowAspectAndAttachToClassLoader(ClassLoader loader, int cflowID) {
    // System.out.println("------------------> compileCflowAspectAndAttachToClassLoader: loader = " + loader);
    //TODO we need a Class.forName check first to avoid unecessary compilation
    // else it will fail in defineClass and fallback on Class.forName ie uneeded compilation
    // -> price to pay between compilation + exception in the worse case vs Class.forName each time
    CompiledCflowAspect cflowAspect = compileCflowAspect(cflowID);

    if (AbstractJoinPointCompiler.DUMP_JP_CLASSES) {
      try {
        AsmHelper.dumpClass("_dump", getCflowAspectClassName(cflowID), cflowAspect.bytecode);
      } catch (Throwable t) {
        ;
      }
    }

    Class cflowAspectClass = AsmHelper.defineClass(
            loader,
            cflowAspect.bytecode,
            getCflowAspectClassName(cflowID)
    );
    return cflowAspectClass;
  }

  /**
   * Compile a jit cflow aspect
   *
   * @param cflowID
   * @return
   */
  public static CompiledCflowAspect compileCflowAspect(int cflowID) {
    CompiledCflowAspect cflowAspect = new CompiledCflowAspect();
    CflowCompiler compiler = new CflowCompiler(cflowID);
    cflowAspect.bytecode = compiler.compile();
    cflowAspect.className = compiler.m_className;
    return cflowAspect;
  }

  /**
   * Information about a compiled Cflow Aspect
   */
  public static class CompiledCflowAspect {
    public byte[] bytecode;
    public String className;// ASM style

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CompiledCflowAspect)) return false;

      final CompiledCflowAspect compiledCflowAspect = (CompiledCflowAspect) o;

      if (!className.equals(compiledCflowAspect.className)) return false;

      return true;
    }

    public int hashCode() {
      return className.hashCode();
    }
  }
}
