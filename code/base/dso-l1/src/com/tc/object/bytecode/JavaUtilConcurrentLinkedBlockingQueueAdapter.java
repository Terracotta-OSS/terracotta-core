/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.SerializationUtil;

public class JavaUtilConcurrentLinkedBlockingQueueAdapter implements Opcodes {

  public static class PutAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new PutMethodAdapter(visitOriginal(classVisitor), SerializationUtil.QUEUE_PUT_SIGNATURE);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  public static class ClearAdapter extends AbstractMethodAdapter {
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new ClearMethodAdapter(visitOriginal(classVisitor), SerializationUtil.CLEAR_SIGNATURE);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  public static class RemoveFirstNAdapter extends AbstractMethodAdapter {
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new RemoveFirstNMethodAdapter(visitOriginal(classVisitor), SerializationUtil.REMOVE_FIRST_N_SIGNATURE);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  public static class TakeAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new TakeMethodAdapter(visitOriginal(classVisitor), SerializationUtil.TAKE_SIGNATURE);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class ClearMethodAdapter extends MethodAdapter implements Opcodes {
    private final String invokeMethodSignature;

    public ClearMethodAdapter(MethodVisitor mv, String invokeMethodSignature) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (PUTFIELD == opcode && "next".equals(name)) {
        addLogicalInvokeMethodCall();
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);

      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(0));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class RemoveFirstNMethodAdapter extends MethodAdapter implements Opcodes {
    private final String invokeMethodSignature;

    public RemoveFirstNMethodAdapter(MethodVisitor mv, String invokeMethodSignature) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (PUTFIELD == opcode && "next".equals(name) && "Ljava/util/concurrent/LinkedBlockingQueue$Node;".equals(desc)) {
        addLogicalInvokeMethodCall();
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);

      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(1));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

      int count = 0;
      mv.visitInsn(DUP);
      mv.visitLdcInsn(new Integer(count++));
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 3);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);

      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class PutMethodAdapter extends MethodAdapter implements Opcodes {
    private final String invokeMethodSignature;

    public PutMethodAdapter(MethodVisitor mv, String invokeMethodSignature) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
    }
    
    /**
     * Changing the while (count.get() == capacity) condition to
     * while (count.get() >= capacity) due to the non-blocking version of put().
     */
    public void visitJumpInsn(int opcode, Label label) {
      if (IF_ICMPEQ == opcode) {
        opcode = IF_ICMPGE;
      }
      super.visitJumpInsn(opcode, label);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if ("insert".equals(name) && "(Ljava/lang/Object;)V".equals(desc)) {
        addLogicalInvokeMethodCall();
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "putLock",
                        "Ljava/util/concurrent/locks/ReentrantLock;");

      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(1));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      int count = 0;
      mv.visitLdcInsn(new Integer(count++));
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction",
                         "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class TakeMethodAdapter extends MethodAdapter implements Opcodes {
    private boolean      visitExtract = false;
    private final String invokeMethodSignature;

    public TakeMethodAdapter(MethodVisitor mv, String invokeMethodSignature) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
    }

    public void visitJumpInsn(int opcode, Label label) {
      if (IFEQ == opcode) {
        opcode = IFLE;
      }
      super.visitJumpInsn(opcode, label);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if ("extract".equals(name) && "()Ljava/lang/Object;".equals(desc)) {
        visitExtract = true;
      }
    }

    public void visitVarInsn(int opcode, int var) {
      if (ASTORE == opcode && visitExtract) {
        super.visitVarInsn(opcode, var);
        addLogicalInvokeMethodCall();
        visitExtract = false;
      } else {
        super.visitVarInsn(opcode, var);
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "takeLock",
                        "Ljava/util/concurrent/locks/ReentrantLock;");
      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(0));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction",
                         "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static void addCheckedManagedCode(MethodVisitor mv, Label notManaged) {
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    mv.visitJumpInsn(IFEQ, notManaged);
  }
}
