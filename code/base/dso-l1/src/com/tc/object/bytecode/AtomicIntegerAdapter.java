/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.util.runtime.Vm;

/**
 * NOTE: This adapter is only used for IBM JDK
 */
public class AtomicIntegerAdapter extends ClassAdapter implements Opcodes {

  public static final String VALUE_FIELD_NAME = "java.util.concurrent.atomic.AtomicInteger.value";

  public AtomicIntegerAdapter(ClassVisitor cv) {
    super(cv);
    Vm.assertIsIbm();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("set".equals(name) && "(I)V".equals(desc)) {
      return new SetMethodVisitor(mv);
    } else if ("get".equals(name) && "()I".equals(desc)) { return new GetMethodVisitor(mv); }

    return mv;
  }

  private static class SetMethodVisitor extends AbstractVolatileFieldMethodVisitor {
    private int int_var_store;

    private SetMethodVisitor(MethodVisitor mv) {
      super(mv);
    }

    public void modifyVolatileValue(int tcobjectVarStore, Label labelCommitVolatile) {
      // if the TCObject instance is not null, obtain it again so
      // that it can be used to signal the new value for the local 'value'
      // variable
      mv.visitVarInsn(ALOAD, tcobjectVarStore);
      mv.visitLdcInsn("java.util.concurrent.atomic.AtomicInteger");
      mv.visitLdcInsn(VALUE_FIELD_NAME);
      mv.visitVarInsn(ILOAD, int_var_store);
      mv.visitInsn(ICONST_M1);
      mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "intFieldChanged",
                         "(Ljava/lang/String;Ljava/lang/String;II)V");
      mv.visitJumpInsn(GOTO, labelCommitVolatile);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (PUTFIELD == opcode && owner.equals("java/util/concurrent/atomic/AtomicInteger") && name.equals("value")
          && desc.equals("I")) {

        int_var_store = getMaxLocalVarStore() + 1;

        // make a copy of the int that's currently on the stack,
        // ready to be assigned to the 'value' field
        mv.visitInsn(DUP);

        // store the int value in the appropriate local variable slot
        mv.visitVarInsn(ISTORE, int_var_store);

        doVolatileBeginCommitLogic(VALUE_FIELD_NAME, int_var_store + 1);

        // setup stack for original putfield operation
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, int_var_store);
      }

      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }

  private static class GetMethodVisitor extends AbstractVolatileFieldMethodVisitor {

    private GetMethodVisitor(MethodVisitor mv) {
      super(mv);
    }

    public void modifyVolatileValue(int tcobjectVarStore, Label labelCommitVolatile) {
      // do nothing since this is a getter
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (GETFIELD == opcode && owner.equals("java/util/concurrent/atomic/AtomicInteger") && name.equals("value")
          && desc.equals("I")) {

        doVolatileBeginCommitLogic(VALUE_FIELD_NAME, getMaxLocalVarStore() + 1);
      }

      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }
}