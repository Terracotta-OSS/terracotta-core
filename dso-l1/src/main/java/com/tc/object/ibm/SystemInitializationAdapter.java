/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.ibm;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class SystemInitializationAdapter extends ClassAdapter implements ClassAdapterFactory {

  public SystemInitializationAdapter(ClassVisitor cv) {
    super(cv);
  }

  public SystemInitializationAdapter() {
    super(null);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("lastChanceHook".equals(name)) { return new LastChanceHookAdapter(mv); }

    return mv;

  }

  private static class LastChanceHookAdapter extends MethodAdapter implements Opcodes {

    public LastChanceHookAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);

      // The important bit with this particular location is that it happens
      // before the jmx remote agent thread is started
      if ((opcode == INVOKESTATIC) && "getProperties".equals(name) && "java/lang/System".equals(owner)) {
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "systemLoaderInitialized", "()V");
      }
    }
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new SystemInitializationAdapter(visitor);
  }

}
