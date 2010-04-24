/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.jam.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class ReflectClassBuilderAdapter extends ClassAdapter implements ClassAdapterFactory {

  public ReflectClassBuilderAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ReflectClassBuilderAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new ReflectClassBuilderAdapter(visitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    return new HideTCInstrumentationAdapter(mv);
  }

  private static class HideTCInstrumentationAdapter extends MethodAdapter implements Opcodes {

    public HideTCInstrumentationAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);

      if ((opcode == INVOKEVIRTUAL) && "java/lang/Class".equals(owner) && "getInterfaces".equals(name)) {
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/TCInterfaces", "purgeTCInterfaces",
                              "([Ljava/lang/Class;)[Ljava/lang/Class;");
      }

      if ((opcode == INVOKEVIRTUAL) && "java/lang/Class".equals(owner) && "getDeclaredMethods".equals(name)) {
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/TCInterfaces", "purgeTCMethods",
                              "([Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Method;");
      }

    }
  }

}
