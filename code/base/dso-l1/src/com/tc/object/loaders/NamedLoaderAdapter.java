/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;

/**
 * Adds the NamedClassLoader interface (and required impl) to Tomcat's internal loader implementations
 */
public class NamedLoaderAdapter extends ClassAdapter implements Opcodes {

  private static final String LOADER_NAME_FIELD = ByteCodeUtil.TC_FIELD_PREFIX + "loaderName";
  private String              owner;

  public NamedLoaderAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { NamedClassLoader.CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
    this.owner = name;
  }

  public void visitEnd() {
    super.visitField(ACC_SYNTHETIC | ACC_VOLATILE | ACC_TRANSIENT | ACC_PRIVATE, LOADER_NAME_FIELD,
                     "Ljava/lang/String;", null, null);

    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "__tc_setClassLoaderName",
                                         "(Ljava/lang/String;)V", null, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "__tc_getClassLoaderName", "()Ljava/lang/String;", null, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    super.visitEnd();
  }
}
