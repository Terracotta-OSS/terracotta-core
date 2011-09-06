/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.AdviceAdapter;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.tools.BootJarTool;

/**
 * Adds NamedClassLoader interface to a class with a hard coded name (known at instrumentation time).
 * The hard coded name can be overrided by specifying a property name in the constructor and
 * giving a value to the property when starting the VM
 */
public class StandardClassLoaderAdapter extends ClassAdapter implements Opcodes {

  private static final String LOADER_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "loaderName";

  private final String        loaderName;
  private final String        sysProp;
  private boolean             hasStaticInit;
  private String              className;

  public StandardClassLoaderAdapter(ClassVisitor cv, String loaderName, String sysProp) {
    super(cv);
    this.loaderName = loaderName;
    this.sysProp = sysProp;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.className = name;
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { ByteCodeUtil.NAMEDCLASSLOADER_CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitEnd() {
    addNamedClassLoaderMethods();

    super.visitField(ACC_STATIC | ACC_SYNTHETIC | ACC_FINAL | ACC_PRIVATE, LOADER_NAME, "Ljava/lang/String;", null,
                     null);

    if (!hasStaticInit) {
      MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
      mv.visitCode();
      setupLoaderName(mv);
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    super.visitEnd();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("<clinit>".equals(name)) {
      hasStaticInit = true;
      return new StaticInitAdapter(mv, access, name, desc);
    }

    return mv;
  }

  private void addNamedClassLoaderMethods() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_getClassLoaderName",
                                         "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitFieldInsn(GETSTATIC, className, LOADER_NAME, "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_setClassLoaderName", "(Ljava/lang/String;)V",
                           null, null);
    mv.visitCode();

    mv.visitTypeInsn(NEW, "java/lang/AssertionError");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Classloader names cannot be set programmatically. Please use -D" +
                    BootJarTool.SYSTEM_CLASSLOADER_NAME_PROPERTY + " to specify a custom name to the Standard Class Loader");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V");
    mv.visitInsn(ATHROW);

    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void setupLoaderName(MethodVisitor mv) {
    mv.visitLdcInsn(sysProp);
    mv.visitLdcInsn(loaderName);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty",
                       "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    mv.visitFieldInsn(PUTSTATIC, className, LOADER_NAME, "Ljava/lang/String;");
  }

  private class StaticInitAdapter extends AdviceAdapter {

    public StaticInitAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(mv, access, name, desc);
    }

    protected void onMethodEnter() {
      //
    }

    protected void onMethodExit(int opcode) {
      if (RETURN == opcode) {
        setupLoaderName(this);
      }
    }
  }

}
