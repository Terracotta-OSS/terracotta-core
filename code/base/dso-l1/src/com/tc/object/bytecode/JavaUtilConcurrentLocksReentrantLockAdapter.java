/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.exception.TCNotSupportedMethodException;
import com.tc.object.lockmanager.api.LockLevel;

import java.util.HashSet;
import java.util.Set;

public class JavaUtilConcurrentLocksReentrantLockAdapter extends ClassAdapter implements Opcodes {
  private static final String LOCK_SIGNATURE                      = "lock()V";
  private static final String UNLOCK_SIGNATURE                    = "unlock()V";
  private static final String TO_STRING_SIGNATURE                 = "toString()Ljava/lang/String;";
  private static final String IS_LOCKED_SIGNATURE                 = "isLocked()Z";
  private static final String GET_OWNER_SIGNATURE                 = "getOwner()Ljava/lang/Thread;";
  private static final String GET_QUEUED_THREADS_SIGNATURE        = "getQueuedThreads()Ljava/util/Collection;";
  private static final String GET_WAITING_THREADS_SIGNATURE       = "getWaitingThreads(Ljava/util/concurrent/locks/Condition;)Ljava/util/Collection;";
  private static final String HAS_QUEUED_THREAD_SIGNATURE         = "hasQueuedThread(Ljava/lang/Thread;)Z";
  private static final String GET_HOLD_COUNT_SIGNATURE            = "getHoldCount()I";
  private static final String GET_QUEUE_LENGTH                    = "getQueueLength()I";
  private static final String HAS_QUEUED_THREADS_SIGNATURE        = "hasQueuedThreads()Z";
  private static final String IS_HELD_BY_CURRENT_THREAD_SIGNATURE = "isHeldByCurrentThread()Z";
  private static final String LOCK_INTERRUPTIBLY_SIGNATURE        = "lockInterruptibly()V";
  private static final String TRY_LOCK_SIGNATURE                  = "tryLock()Z";
  private static final String TRY_LOCK_TIMEOUT_SIGNATURE          = "tryLock(JLjava/util/concurrent/TimeUnit;)Z";
  private static final String NEW_CONDITION_SIGNATURE             = "newCondition()Ljava/util/concurrent/locks/Condition;";
  private static final String GET_WAIT_QUEUE_LENGTH_SIGNATURE     = "getWaitQueueLength(Ljava/util/concurrent/locks/Condition;)I";
  private static final String HAS_WAITERS_SIGNATURE               = "hasWaiters(Ljava/util/concurrent/locks/Condition;)Z";

  private static final Set    nonSupportedMethods                 = new HashSet();

  static {
    nonSupportedMethods.add(GET_OWNER_SIGNATURE);
    nonSupportedMethods.add(GET_QUEUED_THREADS_SIGNATURE);
    nonSupportedMethods.add(GET_WAITING_THREADS_SIGNATURE);
    nonSupportedMethods.add(HAS_QUEUED_THREAD_SIGNATURE);
  }

  public JavaUtilConcurrentLocksReentrantLockAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visitEnd() {
    super.visitEnd();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String method = name + desc;
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if (method.equals(LOCK_SIGNATURE)) {
      return new UtilsMethodAdapter(access, name, desc, mv);
    } else if (method.equals(NEW_CONDITION_SIGNATURE)) {
      return new NewConditionMethodAdapter(mv);
    } else if (method.equals(UNLOCK_SIGNATURE)) {
      return new UtilsMethodAdapter(access, name, desc, mv);
    } else if (method.equals(TO_STRING_SIGNATURE)) {
      return new ToStringMethodAdapter(access, desc, mv);
    } else if (method.equals(IS_LOCKED_SIGNATURE)) {
      return new IsLockedMethodAdapter(access, desc, mv);
    } else if (method.equals(GET_HOLD_COUNT_SIGNATURE)) {
      return new GetHoldCountMethodAdapter(access, desc, mv);
    } else if (method.equals(GET_QUEUE_LENGTH)) {
      return new GetQueueLengthMethodAdapter(access, desc, mv);
    } else if (method.equals(GET_WAIT_QUEUE_LENGTH_SIGNATURE)) {
      return new GetWaitQueueLengthMethodAdapter(access, name, desc, mv);
    } else if (method.equals(HAS_WAITERS_SIGNATURE)) {
      return new HasWaitersMethodAdapter(access, name, desc, mv);
    } else if (method.equals(HAS_QUEUED_THREADS_SIGNATURE)) {
      return new HasQueuedThreadsMethodAdapter(access, desc, mv);
    } else if (method.equals(IS_HELD_BY_CURRENT_THREAD_SIGNATURE)) {
      return new IsHeldByCurrentThreadMethodAdapter(access, desc, mv);
    } else if (method.equals(LOCK_INTERRUPTIBLY_SIGNATURE)) {
      return new UtilsMethodAdapter(access, name, desc, mv);
    } else if (method.equals(TRY_LOCK_SIGNATURE)) {
      return new UtilsMethodAdapter(access, name, desc, mv);
    } else if (method.equals(TRY_LOCK_TIMEOUT_SIGNATURE)) {
      return new TryLockTimeoutMethodAdapter(access, desc, mv);
    } else if (nonSupportedMethods.contains(method)) {
      return new NonSupportedMethodAdapter(access, desc, mv);
    } else {
      return mv;
    }
  }

  private static class NewConditionMethodAdapter extends MethodAdapter implements Opcodes {
    public NewConditionMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitCode() {
      super.visitCode();
      mv.visitTypeInsn(NEW, "com/tcclient/util/ConditionObjectWrapper");
      mv.visitInsn(DUP);
    }

    public void visitInsn(int opcode) {
      if (ARETURN == opcode) {
        ByteCodeUtil.pushThis(mv);
        mv.visitMethodInsn(INVOKESPECIAL, "com/tcclient/util/ConditionObjectWrapper", "<init>",
        "(Ljava/util/concurrent/locks/Condition;Ljava/util/concurrent/locks/ReentrantLock;)V");
      }
      super.visitInsn(opcode);
    }
  }

  private static abstract class ReentrantLockMethodAdapter extends LocalVariablesSorter implements Opcodes {
    Label l1 = new Label();
    Label l2 = new Label();

    public ReentrantLockMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    public void visitCode() {
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      mv.visitJumpInsn(IFEQ, l1);
      managedCode();
      mv.visitJumpInsn(GOTO, l2);
      mv.visitLabel(l1);
      super.visitCode();
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode || ARETURN == opcode || IRETURN == opcode) {
        mv.visitLabel(l2);
      }
      super.visitInsn(opcode);
    }

    protected abstract void managedCode();
  }
  
  private static class UtilsMethodAdapter extends ReentrantLockMethodAdapter {
    private final String methodName;
    private final String methodDesc;

    public UtilsMethodAdapter(int access, String methodName, String desc, MethodVisitor mv) {
      super(access, desc, mv);
      this.methodName = methodName;
      this.methodDesc = desc;
    }

    protected void managedCode() {
      Type returnType = Type.getReturnType(methodDesc);

      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESTATIC, "com/tcclient/util/ReentrantLockUtils", methodName,
                         "(Ljava/util/concurrent/locks/ReentrantLock;)" + returnType.getDescriptor());
    }
  }

  private static class ToStringMethodAdapter extends ReentrantLockMethodAdapter {
    public ToStringMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    protected void managedCode() {
      mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                         "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESTATIC, "com/tcclient/util/ReentrantLockUtils", "toString",
                         "(Ljava/util/concurrent/locks/ReentrantLock;)Ljava/lang/String;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                         "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
    }
  }

  private static class IsLockedMethodAdapter extends ReentrantLockMethodAdapter {
    public IsLockedMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    protected void managedCode() {
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isLocked", "(Ljava/lang/Object;)Z");
    }
  }

  private static class GetHoldCountMethodAdapter extends ReentrantLockMethodAdapter {
    public GetHoldCountMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    protected void managedCode() {
      ByteCodeUtil.pushThis(mv);
      mv.visitIntInsn(BIPUSH, LockLevel.WRITE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "heldCount", "(Ljava/lang/Object;I)I");
    }
  }

  private static class GetQueueLengthMethodAdapter extends ReentrantLockMethodAdapter {
    public GetQueueLengthMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    protected void managedCode() {
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "queueLength", "(Ljava/lang/Object;)I");
    }
  }

  private static class GetWaitQueueLengthMethodAdapter extends ReentrantLockMethodAdapter {
    private final String methodName;
    private final String methodDesc;

    public GetWaitQueueLengthMethodAdapter(int access, String name, String desc, MethodVisitor mv) {
      super(access, desc, mv);
      this.methodName = name;
      this.methodDesc = desc;
    }

    protected void managedCode() {
      Type[] params = Type.getArgumentTypes(methodDesc);
      int varIndex = 0;
      for (int i = 0; i < params.length; i++) {
        mv.visitVarInsn(params[i].getOpcode(ILOAD), varIndex + 1);
        varIndex += params[i].getSize();
      }
      mv.visitMethodInsn(INVOKESTATIC, "com/tcclient/util/ReentrantLockUtils", methodName, methodDesc);
    }
  }
  
  private static class HasWaitersMethodAdapter extends ReentrantLockMethodAdapter {
    private final String methodName;
    private final String methodDesc;

    public HasWaitersMethodAdapter(int access, String name, String desc, MethodVisitor mv) {
      super(access, desc, mv);
      this.methodName = name;
      this.methodDesc = desc;
    }

    protected void managedCode() {
      Type[] params = Type.getArgumentTypes(methodDesc);
      int varIndex = 0;
      for (int i = 0; i < params.length; i++) {
        mv.visitVarInsn(params[i].getOpcode(ILOAD), varIndex + 1);
        varIndex += params[i].getSize();
      }
      mv.visitMethodInsn(INVOKESTATIC, "com/tcclient/util/ReentrantLockUtils", methodName, methodDesc);
    }
  }

  private static class HasQueuedThreadsMethodAdapter extends ReentrantLockMethodAdapter {
    public HasQueuedThreadsMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    protected void managedCode() {
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "queueLength", "(Ljava/lang/Object;)I");
      Label falseLabel = new Label();
      Label trueLabel = new Label();
      mv.visitJumpInsn(IFLE, falseLabel);
      mv.visitInsn(ICONST_1);
      mv.visitJumpInsn(GOTO, trueLabel);
      mv.visitLabel(falseLabel);
      mv.visitInsn(ICONST_0);
      mv.visitLabel(trueLabel);
    }
  }

  private static class IsHeldByCurrentThreadMethodAdapter extends ReentrantLockMethodAdapter {
    public IsHeldByCurrentThreadMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    protected void managedCode() {
      ByteCodeUtil.pushThis(mv);
      mv.visitIntInsn(BIPUSH, LockLevel.WRITE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isHeldByCurrentThread", "(Ljava/lang/Object;I)Z");
    }
  }

  private static class TryLockTimeoutMethodAdapter extends ReentrantLockMethodAdapter {
    private final String methodDesc;

    public TryLockTimeoutMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
      this.methodDesc = desc;
    }

    protected void managedCode() {
      Type[] params = Type.getArgumentTypes(methodDesc);
      int varIndex = 0;
      for (int i = 0; i < params.length; i++) {
        mv.visitVarInsn(params[i].getOpcode(ILOAD), varIndex + 1);
        varIndex += params[i].getSize();
      }
      ByteCodeUtil.pushThis(mv);
      mv
          .visitMethodInsn(INVOKESTATIC, "com/tcclient/util/ReentrantLockUtils", "tryLock",
          "(JLjava/util/concurrent/TimeUnit;Ljava/util/concurrent/locks/ReentrantLock;)Z");
    }
  }

  private static class NonSupportedMethodAdapter extends ReentrantLockMethodAdapter {
    public NonSupportedMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    protected void managedCode() {
      mv.visitTypeInsn(NEW, TCNotSupportedMethodException.CLASS_SLASH);
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, TCNotSupportedMethodException.CLASS_SLASH, "<init>", "()V");
      mv.visitInsn(ATHROW);
    }
  }
}