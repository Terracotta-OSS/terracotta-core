/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.object.SerializationUtil;

public class JavaUtilConcurrentLinkedBlockingQueueAdapter implements Opcodes {

  public static class PutAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new PutMethodAdapter(access, description, visitOriginal(classVisitor), SerializationUtil.QUEUE_PUT_SIGNATURE);
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

  private static class PutMethodAdapter extends LocalVariablesSorter implements Opcodes {
    private final String invokeMethodSignature;
    private final Label continueLabel;
    private boolean visitAddCoded = false;
    private int newVar;

    public PutMethodAdapter(final int access, final String desc, final MethodVisitor mv, String invokeMethodSignature) {
      super(access, desc, mv);
      this.invokeMethodSignature = invokeMethodSignature;
      this.continueLabel = new Label();
      //      this.newVar = newLocal(Type.INT_TYPE);
      this.newVar = 5;
    }

    /**
     * Changing the while (count.get() == capacity) condition to while (count.get() >= capacity) due to the non-blocking
     * version of __tc_put().
     */
    public void visitJumpInsn(int opcode, Label label) {
      if (IF_ICMPEQ == opcode) {
        opcode = IF_ICMPGE;
      } else if (IF_ICMPNE == opcode) { // for jdk higher than 1.5_08
        opcode = IF_ICMPLT;
      }
      super.visitJumpInsn(opcode, label);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if ("insert".equals(name) && "(Ljava/lang/Object;)V".equals(desc)) {
        addLogicalInvokeMethodCall();
      } else if (INVOKESPECIAL == opcode && "signalNotEmpty".equals(name) && "()V".equals(desc)) {
        super.visitInsn(RETURN);
      }
    }
    
    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        super.visitVarInsn(ILOAD, newVar);
        Label a = new Label();
        super.visitJumpInsn(IFNE, a);
        ByteCodeUtil.pushThis(mv);
        super.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "signalNotEmpty", "()V");
        super.visitLabel(a);
      }
      super.visitInsn(opcode);
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      
      addCheckedManagedCode(mv, notManaged);
      super.visitVarInsn(ALOAD, 4);
      super.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
      super.visitVarInsn(ISTORE, 2);
      ByteCodeUtil.pushThis(mv);
      super.visitVarInsn(ALOAD, 3);
      super.visitLdcInsn(invokeMethodSignature);
      super.visitInsn(ICONST_1);
      super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      super.visitInsn(DUP);
      super.visitInsn(ICONST_0);
      super.visitVarInsn(ALOAD, 1);
      super.visitInsn(AASTORE);
      super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      super.visitVarInsn(ALOAD, 4);
      super.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
      super.visitVarInsn(ISTORE, newVar);
      super.visitJumpInsn(GOTO, continueLabel);
      super.visitLabel(notManaged);
      visitAddCoded = true;
    }
    
    public void visitVarInsn(int opcode, int var) {
      super.visitVarInsn(opcode, var);
      if (ISTORE == opcode && 2 == var && visitAddCoded) {
        super.visitLabel(continueLabel);
        visitAddCoded = false;
      }
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
      } else if (IFNE == opcode) { // for jdk higher than 1.5_08
        opcode = IFGT;
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
