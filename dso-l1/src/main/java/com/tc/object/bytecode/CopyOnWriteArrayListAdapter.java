/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.util.runtime.Vm;

public class CopyOnWriteArrayListAdapter {
  public static final String CONSTRUCTOR1_SIGNATURE = "__INIT__()V";
  public static final String CONSTRUCTOR2_SIGNATURE = "__INIT__(Ljava/util/Collection;)V";
  public static final String CONSTRUCTOR3_SIGNATURE = "__INIT__([Ljava/lang/Object;)V";
  public static final String RESET_LOCK_SIGNATURE   = "resetLock()V";

  public static class Jdk16LockAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new LockAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class LockAdapter extends MethodAdapter implements Opcodes {

    public LockAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (Vm.isJDK16Compliant()) {
        if (opcode == PUTFIELD && "lock".equals(name)) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitTypeInsn(NEW, "com/tc/util/concurrent/locks/CopyOnWriteArrayListLock");
          mv.visitInsn(DUP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/concurrent/locks/CopyOnWriteArrayListLock", "<init>",
                             "(Ljava/util/concurrent/CopyOnWriteArrayList;)V");
        }
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }

  public static class AddAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new AddMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class AddMethodAdapter extends MethodAdapter implements Opcodes {

    public AddMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("add(Ljava/lang/Object;)Z");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class AddAtAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new AddAtMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class AddAtMethodAdapter extends MethodAdapter implements Opcodes {

    public AddAtMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("add(ILjava/lang/Object;)V");
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class SetAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new SetMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class SetMethodAdapter extends MethodAdapter implements Opcodes {

    public SetMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("set(ILjava/lang/Object;)Ljava/lang/Object;");
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class AddAllAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new AddAllMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class AddAllMethodAdapter extends MethodAdapter implements Opcodes {

    public AddAllMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("addAll(Ljava/util/Collection;)Z");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class AddAllAtAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new AddAllAtMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class AddAllAtMethodAdapter extends MethodAdapter implements Opcodes {

    public AddAllAtMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("addAll(ILjava/util/Collection;)Z");
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class AddIfAbsentAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new AddIfAbsentMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class AddIfAbsentMethodAdapter extends MethodAdapter implements Opcodes {

    public AddIfAbsentMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("add(Ljava/lang/Object;)Z");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }

  }

  public static class AddAllAbsentAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new AddAllAbsentMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class AddAllAbsentMethodAdapter extends MethodAdapter implements Opcodes {

    public AddAllAbsentMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          mv.visitVarInsn(ILOAD, 6);
          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          mv.visitVarInsn(ASTORE, 9);

          mv.visitVarInsn(ALOAD, 5);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ALOAD, 9);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ILOAD, 6);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy",
                             "(Ljava/lang/Object;ILjava/lang/Object;II)V");

          mv.visitVarInsn(ALOAD, 0);
          mv.visitLdcInsn("addAll(Ljava/util/Collection;)Z");

          mv.visitInsn(ICONST_1);
          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          mv.visitInsn(DUP);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ALOAD, 9);
          mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;");
          mv.visitInsn(AASTORE);

          mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                             "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          mv.visitVarInsn(ILOAD, 7);
          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          mv.visitVarInsn(ASTORE, 9);

          mv.visitVarInsn(ALOAD, 3);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ALOAD, 9);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ILOAD, 7);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy",
                             "(Ljava/lang/Object;ILjava/lang/Object;II)V");

          mv.visitVarInsn(ALOAD, 0);
          mv.visitLdcInsn("addAll(Ljava/util/Collection;)Z");

          mv.visitInsn(ICONST_1);
          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          mv.visitInsn(DUP);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ALOAD, 9);
          mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;");
          mv.visitInsn(AASTORE);

          mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                             "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
        }
      }
    }
  }

  public static class RemoveAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new RemoveMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class RemoveMethodAdapter extends MethodAdapter implements Opcodes {

    public RemoveMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitLdcInsn("remove(Ljava/lang/Object;)Z");
          mv.visitInsn(ICONST_1);
          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          mv.visitInsn(DUP);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
          mv.visitVarInsn(ILOAD, 5);
          mv.visitInsn(AALOAD);
          mv.visitInsn(AASTORE);

          mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                             "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
        }
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitLdcInsn("remove(Ljava/lang/Object;)Z");
          mv.visitInsn(ICONST_1);
          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          mv.visitInsn(DUP);
          mv.visitInsn(ICONST_0);
          mv.visitVarInsn(ALOAD, 3);
          mv.visitVarInsn(ILOAD, 7);
          mv.visitInsn(AALOAD);
          mv.visitInsn(AASTORE);
          mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                             "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
        }
      }
      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }

  public static class RemoveAtAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new RemoveAtMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class RemoveAtMethodAdapter extends MethodAdapter implements Opcodes {

    public RemoveAtMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("remove(I)Ljava/lang/Object;");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class RemoveRangeAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new RemoveRangeMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class RemoveRangeMethodAdapter extends MethodAdapter implements Opcodes {

    public RemoveRangeMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("removeRange(II)V");
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class ClearAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new ClearMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class ClearMethodAdapter extends MethodAdapter implements Opcodes {

    public ClearMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (Vm.isJDK15()) {
        if (opcode == PUTFIELD && "array".equals(name)) {
          doVisit();
        }
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (Vm.isJDK16Compliant()) {
        if ("setArray".equals(name) && "([Ljava/lang/Object;)V".equals(desc)) {
          doVisit();
        }
      }
    }

    private void doVisit() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("clear()V");
      mv.visitInsn(ICONST_0);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }

  public static class RemoveAllAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = cv
          .visitMethod(this.access, this.methodName, this.description, this.signature, this.exceptions);
      if (Vm.isJDK15()) {
        adaptRemoveAllJdk15(mv);
      } else if (Vm.isJDK16Compliant()) {
        adaptRemoveAllJdk16Compliant(mv);
      }
      return null;
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private void adaptRemoveAllJdk16Compliant(MethodVisitor mv) {
      mv.visitCode();
      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      mv.visitTryCatchBlock(l0, l1, l2, null);
      Label l3 = new Label();
      mv.visitLabel(l3);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "lock",
                        "Ljava/util/concurrent/locks/ReentrantLock;");
      mv.visitVarInsn(ASTORE, 2);
      Label l4 = new Label();
      mv.visitLabel(l4);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lock", "()V");
      mv.visitLabel(l0);

      mv.visitTypeInsn(NEW, "java/util/ArrayList");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
      mv.visitVarInsn(ASTORE, 3);
      Label l5 = new Label();
      mv.visitLabel(l5);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CopyOnWriteArrayList", "getArray",
                         "()[Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, 4);
      Label l6 = new Label();
      mv.visitLabel(l6);

      mv.visitVarInsn(ALOAD, 4);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitVarInsn(ISTORE, 5);
      Label l7 = new Label();
      mv.visitLabel(l7);

      mv.visitVarInsn(ILOAD, 5);
      Label l8 = new Label();
      mv.visitJumpInsn(IFEQ, l8);
      Label l9 = new Label();
      mv.visitLabel(l9);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 6);
      Label l10 = new Label();
      mv.visitLabel(l10);

      mv.visitVarInsn(ILOAD, 5);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 7);
      Label l11 = new Label();
      mv.visitLabel(l11);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 8);
      Label l12 = new Label();
      mv.visitLabel(l12);
      Label l13 = new Label();
      mv.visitJumpInsn(GOTO, l13);
      Label l14 = new Label();
      mv.visitLabel(l14);

      mv.visitFrame(Opcodes.F_FULL, 9, new Object[] { "java/util/concurrent/CopyOnWriteArrayList",
          "java/util/Collection", "java/util/concurrent/locks/ReentrantLock", "java/util/List", "[Ljava/lang/Object;",
          Opcodes.INTEGER, Opcodes.INTEGER, "[Ljava/lang/Object;", Opcodes.INTEGER }, 0, new Object[] {});
      mv.visitVarInsn(ALOAD, 4);
      mv.visitVarInsn(ILOAD, 8);
      mv.visitInsn(AALOAD);
      mv.visitVarInsn(ASTORE, 9);
      Label l15 = new Label();
      mv.visitLabel(l15);

      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 9);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "contains", "(Ljava/lang/Object;)Z");
      Label l16 = new Label();
      mv.visitJumpInsn(IFNE, l16);
      Label l17 = new Label();
      mv.visitLabel(l17);

      mv.visitVarInsn(ALOAD, 7);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitIincInsn(6, 1);
      mv.visitVarInsn(ALOAD, 9);
      mv.visitInsn(AASTORE);
      Label l18 = new Label();
      mv.visitJumpInsn(GOTO, l18);
      mv.visitLabel(l16);

      mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "java/lang/Object" }, 0, null);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitVarInsn(ALOAD, 9);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
      mv.visitInsn(POP);
      mv.visitLabel(l18);

      mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
      mv.visitIincInsn(8, 1);
      mv.visitLabel(l13);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ILOAD, 8);
      mv.visitVarInsn(ILOAD, 5);
      mv.visitJumpInsn(IF_ICMPLT, l14);
      Label l19 = new Label();
      mv.visitLabel(l19);

      mv.visitVarInsn(ILOAD, 6);
      mv.visitVarInsn(ILOAD, 5);
      mv.visitJumpInsn(IF_ICMPEQ, l8);
      Label l20 = new Label();
      mv.visitLabel(l20);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOf", "([Ljava/lang/Object;I)[Ljava/lang/Object;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CopyOnWriteArrayList", "setArray",
                         "([Ljava/lang/Object;)V");
      Label l21 = new Label();
      mv.visitLabel(l21);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("removeAll(Ljava/util/Collection;)Z");
      mv.visitVarInsn(ALOAD, 3);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(l1);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
      Label l22 = new Label();
      mv.visitLabel(l22);

      mv.visitInsn(ICONST_1);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l8);

      mv.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
      Label l23 = new Label();
      mv.visitLabel(l23);

      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l2);

      mv.visitFrame(Opcodes.F_FULL, 3, new Object[] { "java/util/concurrent/CopyOnWriteArrayList",
          "java/util/Collection", "java/util/concurrent/locks/ReentrantLock" }, 1,
                    new Object[] { "java/lang/Throwable" });
      mv.visitVarInsn(ASTORE, 10);
      Label l24 = new Label();
      mv.visitLabel(l24);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
      Label l25 = new Label();
      mv.visitLabel(l25);

      mv.visitVarInsn(ALOAD, 10);
      mv.visitInsn(ATHROW);
      Label l26 = new Label();
      mv.visitLabel(l26);
      mv.visitLocalVariable("this", "Ljava/util/concurrent/CopyOnWriteArrayList;",
                            "Ljava/util/concurrent/CopyOnWriteArrayList<TE;>;", l3, l26, 0);
      mv.visitLocalVariable("c", "Ljava/util/Collection;", "Ljava/util/Collection<*>;", l3, l26, 1);
      mv.visitLocalVariable("lock", "Ljava/util/concurrent/locks/ReentrantLock;", null, l4, l26, 2);
      mv.visitLocalVariable("removedElements", "Ljava/util/List;", null, l5, l2, 3);
      mv.visitLocalVariable("elements", "[Ljava/lang/Object;", null, l6, l2, 4);
      mv.visitLocalVariable("len", "I", null, l7, l2, 5);
      mv.visitLocalVariable("newlen", "I", null, l10, l8, 6);
      mv.visitLocalVariable("temp", "[Ljava/lang/Object;", null, l11, l8, 7);
      mv.visitLocalVariable("i", "I", null, l12, l19, 8);
      mv.visitLocalVariable("element", "Ljava/lang/Object;", null, l15, l18, 9);
      mv.visitMaxs(3, 11);
      mv.visitEnd();
    }

    private void adaptRemoveAllJdk15(MethodVisitor mv) {
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);

      mv.visitTypeInsn(NEW, "java/util/ArrayList");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
      mv.visitVarInsn(ASTORE, 2);
      Label l1 = new Label();
      mv.visitLabel(l1);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, 3);
      Label l2 = new Label();
      mv.visitLabel(l2);

      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitVarInsn(ISTORE, 4);
      Label l3 = new Label();
      mv.visitLabel(l3);

      mv.visitVarInsn(ILOAD, 4);
      Label l4 = new Label();
      mv.visitJumpInsn(IFNE, l4);
      Label l5 = new Label();
      mv.visitLabel(l5);

      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l4);

      mv.visitVarInsn(ILOAD, 4);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 5);
      Label l6 = new Label();
      mv.visitLabel(l6);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 6);
      Label l7 = new Label();
      mv.visitLabel(l7);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 7);
      Label l8 = new Label();
      mv.visitLabel(l8);
      Label l9 = new Label();
      mv.visitJumpInsn(GOTO, l9);
      Label l10 = new Label();
      mv.visitLabel(l10);

      mv.visitVarInsn(ALOAD, 3);
      mv.visitVarInsn(ILOAD, 7);
      mv.visitInsn(AALOAD);
      mv.visitVarInsn(ASTORE, 8);
      Label l11 = new Label();
      mv.visitLabel(l11);

      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "contains", "(Ljava/lang/Object;)Z");
      Label l12 = new Label();
      mv.visitJumpInsn(IFNE, l12);
      Label l13 = new Label();
      mv.visitLabel(l13);

      mv.visitVarInsn(ALOAD, 5);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitIincInsn(6, 1);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitInsn(AASTORE);
      Label l14 = new Label();
      mv.visitJumpInsn(GOTO, l14);
      mv.visitLabel(l12);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
      mv.visitInsn(POP);
      mv.visitLabel(l14);

      mv.visitIincInsn(7, 1);
      mv.visitLabel(l9);
      mv.visitVarInsn(ILOAD, 7);
      mv.visitVarInsn(ILOAD, 4);
      mv.visitJumpInsn(IF_ICMPLT, l10);
      Label l15 = new Label();
      mv.visitLabel(l15);

      mv.visitVarInsn(ILOAD, 6);
      mv.visitVarInsn(ILOAD, 4);
      Label l16 = new Label();
      mv.visitJumpInsn(IF_ICMPNE, l16);
      Label l17 = new Label();
      mv.visitLabel(l17);

      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l16);

      mv.visitVarInsn(ILOAD, 6);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 7);
      Label l18 = new Label();
      mv.visitLabel(l18);

      mv.visitVarInsn(ALOAD, 5);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
      Label l19 = new Label();
      mv.visitLabel(l19);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
      Label l20 = new Label();
      mv.visitLabel(l20);

      mv.visitVarInsn(ALOAD, 0);
      Label l21 = new Label();
      mv.visitLabel(l21);

      mv.visitLdcInsn("removeAll(Ljava/util/Collection;)Z");
      Label l22 = new Label();
      mv.visitLabel(l22);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;");
      Label l23 = new Label();
      mv.visitLabel(l23);

      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      Label l24 = new Label();
      mv.visitLabel(l24);

      mv.visitInsn(ICONST_1);
      mv.visitInsn(IRETURN);
      Label l25 = new Label();
      mv.visitLabel(l25);
      mv.visitLocalVariable("this", "Ljava/util/concurrent/CopyOnWriteArrayList;", null, l0, l25, 0);
      mv.visitLocalVariable("c", "Ljava/util/Collection;", null, l0, l25, 1);
      mv.visitLocalVariable("removedObjects", "Ljava/util/List;", null, l1, l25, 2);
      mv.visitLocalVariable("elementData", "[Ljava/lang/Object;", null, l2, l25, 3);
      mv.visitLocalVariable("len", "I", null, l3, l25, 4);
      mv.visitLocalVariable("temp", "[Ljava/lang/Object;", null, l6, l25, 5);
      mv.visitLocalVariable("newlen", "I", null, l7, l25, 6);
      mv.visitLocalVariable("i", "I", null, l8, l15, 7);
      mv.visitLocalVariable("element", "Ljava/lang/Object;", null, l11, l14, 8);
      mv.visitLocalVariable("newArray", "[Ljava/lang/Object;", null, l18, l25, 7);
      mv.visitMaxs(5, 9);
      mv.visitEnd();
    }
  }

  public static class RetainAllAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = cv
          .visitMethod(this.access, this.methodName, this.description, this.signature, this.exceptions);
      if (Vm.isJDK15()) {
        adaptRetainAllJdk15(mv);
      } else if (Vm.isJDK16Compliant()) {
        adaptRetainAllJdk16Compliant(mv);
      }
      return null;
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private void adaptRetainAllJdk16Compliant(MethodVisitor mv) {
      mv.visitCode();
      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      mv.visitTryCatchBlock(l0, l1, l2, null);
      Label l3 = new Label();
      mv.visitLabel(l3);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "lock",
                        "Ljava/util/concurrent/locks/ReentrantLock;");
      mv.visitVarInsn(ASTORE, 2);
      Label l4 = new Label();
      mv.visitLabel(l4);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lock", "()V");
      mv.visitLabel(l0);

      mv.visitTypeInsn(NEW, "java/util/ArrayList");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
      mv.visitVarInsn(ASTORE, 3);
      Label l5 = new Label();
      mv.visitLabel(l5);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CopyOnWriteArrayList", "getArray",
                         "()[Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, 4);
      Label l6 = new Label();
      mv.visitLabel(l6);

      mv.visitVarInsn(ALOAD, 4);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitVarInsn(ISTORE, 5);
      Label l7 = new Label();
      mv.visitLabel(l7);

      mv.visitVarInsn(ILOAD, 5);
      Label l8 = new Label();
      mv.visitJumpInsn(IFEQ, l8);
      Label l9 = new Label();
      mv.visitLabel(l9);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 6);
      Label l10 = new Label();
      mv.visitLabel(l10);

      mv.visitVarInsn(ILOAD, 5);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 7);
      Label l11 = new Label();
      mv.visitLabel(l11);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 8);
      Label l12 = new Label();
      mv.visitLabel(l12);
      Label l13 = new Label();
      mv.visitJumpInsn(GOTO, l13);
      Label l14 = new Label();
      mv.visitLabel(l14);

      mv.visitFrame(Opcodes.F_FULL, 9, new Object[] { "java/util/concurrent/CopyOnWriteArrayList",
          "java/util/Collection", "java/util/concurrent/locks/ReentrantLock", "java/util/List", "[Ljava/lang/Object;",
          Opcodes.INTEGER, Opcodes.INTEGER, "[Ljava/lang/Object;", Opcodes.INTEGER }, 0, new Object[] {});
      mv.visitVarInsn(ALOAD, 4);
      mv.visitVarInsn(ILOAD, 8);
      mv.visitInsn(AALOAD);
      mv.visitVarInsn(ASTORE, 9);
      Label l15 = new Label();
      mv.visitLabel(l15);

      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 9);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "contains", "(Ljava/lang/Object;)Z");
      Label l16 = new Label();
      mv.visitJumpInsn(IFEQ, l16);
      Label l17 = new Label();
      mv.visitLabel(l17);

      mv.visitVarInsn(ALOAD, 7);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitIincInsn(6, 1);
      mv.visitVarInsn(ALOAD, 9);
      mv.visitInsn(AASTORE);
      Label l18 = new Label();
      mv.visitJumpInsn(GOTO, l18);
      mv.visitLabel(l16);

      mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "java/lang/Object" }, 0, null);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitVarInsn(ALOAD, 9);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
      mv.visitInsn(POP);
      mv.visitLabel(l18);

      mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
      mv.visitIincInsn(8, 1);
      mv.visitLabel(l13);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ILOAD, 8);
      mv.visitVarInsn(ILOAD, 5);
      mv.visitJumpInsn(IF_ICMPLT, l14);
      Label l19 = new Label();
      mv.visitLabel(l19);

      mv.visitVarInsn(ILOAD, 6);
      mv.visitVarInsn(ILOAD, 5);
      mv.visitJumpInsn(IF_ICMPEQ, l8);
      Label l20 = new Label();
      mv.visitLabel(l20);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOf", "([Ljava/lang/Object;I)[Ljava/lang/Object;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CopyOnWriteArrayList", "setArray",
                         "([Ljava/lang/Object;)V");
      Label l21 = new Label();
      mv.visitLabel(l21);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("removeAll(Ljava/util/Collection;)Z");
      mv.visitVarInsn(ALOAD, 3);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(l1);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
      Label l22 = new Label();
      mv.visitLabel(l22);

      mv.visitInsn(ICONST_1);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l8);

      mv.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
      Label l23 = new Label();
      mv.visitLabel(l23);

      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l2);

      mv.visitFrame(Opcodes.F_FULL, 3, new Object[] { "java/util/concurrent/CopyOnWriteArrayList",
          "java/util/Collection", "java/util/concurrent/locks/ReentrantLock" }, 1,
                    new Object[] { "java/lang/Throwable" });
      mv.visitVarInsn(ASTORE, 10);
      Label l24 = new Label();
      mv.visitLabel(l24);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
      Label l25 = new Label();
      mv.visitLabel(l25);

      mv.visitVarInsn(ALOAD, 10);
      mv.visitInsn(ATHROW);
      Label l26 = new Label();
      mv.visitLabel(l26);
      mv.visitLocalVariable("this", "Ljava/util/concurrent/CopyOnWriteArrayList;",
                            "Ljava/util/concurrent/CopyOnWriteArrayList<TE;>;", l3, l26, 0);
      mv.visitLocalVariable("c", "Ljava/util/Collection;", "Ljava/util/Collection<*>;", l3, l26, 1);
      mv.visitLocalVariable("lock", "Ljava/util/concurrent/locks/ReentrantLock;", null, l4, l26, 2);
      mv.visitLocalVariable("removedElements", "Ljava/util/List;", null, l5, l2, 3);
      mv.visitLocalVariable("elements", "[Ljava/lang/Object;", null, l6, l2, 4);
      mv.visitLocalVariable("len", "I", null, l7, l2, 5);
      mv.visitLocalVariable("newlen", "I", null, l10, l8, 6);
      mv.visitLocalVariable("temp", "[Ljava/lang/Object;", null, l11, l8, 7);
      mv.visitLocalVariable("i", "I", null, l12, l19, 8);
      mv.visitLocalVariable("element", "Ljava/lang/Object;", null, l15, l18, 9);
      mv.visitMaxs(3, 11);
      mv.visitEnd();

    }

    private void adaptRetainAllJdk15(MethodVisitor mv) {
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);

      mv.visitTypeInsn(NEW, "java/util/ArrayList");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
      mv.visitVarInsn(ASTORE, 2);
      Label l1 = new Label();
      mv.visitLabel(l1);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, 3);
      Label l2 = new Label();
      mv.visitLabel(l2);

      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitVarInsn(ISTORE, 4);
      Label l3 = new Label();
      mv.visitLabel(l3);

      mv.visitVarInsn(ILOAD, 4);
      Label l4 = new Label();
      mv.visitJumpInsn(IFNE, l4);
      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l4);

      mv.visitFrame(Opcodes.F_APPEND, 3, new Object[] { "java/util/List", "[Ljava/lang/Object;", Opcodes.INTEGER }, 0,
                    null);
      mv.visitVarInsn(ILOAD, 4);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 5);
      Label l5 = new Label();
      mv.visitLabel(l5);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 6);
      Label l6 = new Label();
      mv.visitLabel(l6);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 7);
      Label l7 = new Label();
      mv.visitLabel(l7);
      Label l8 = new Label();
      mv.visitJumpInsn(GOTO, l8);
      Label l9 = new Label();
      mv.visitLabel(l9);

      mv.visitFrame(Opcodes.F_APPEND, 3, new Object[] { "[Ljava/lang/Object;", Opcodes.INTEGER, Opcodes.INTEGER }, 0,
                    null);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitVarInsn(ILOAD, 7);
      mv.visitInsn(AALOAD);
      mv.visitVarInsn(ASTORE, 8);
      Label l10 = new Label();
      mv.visitLabel(l10);

      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "contains", "(Ljava/lang/Object;)Z");
      Label l11 = new Label();
      mv.visitJumpInsn(IFEQ, l11);
      Label l12 = new Label();
      mv.visitLabel(l12);

      mv.visitVarInsn(ALOAD, 5);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitIincInsn(6, 1);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitInsn(AASTORE);
      Label l13 = new Label();
      mv.visitJumpInsn(GOTO, l13);
      mv.visitLabel(l11);

      mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "java/lang/Object" }, 0, null);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
      mv.visitInsn(POP);
      mv.visitLabel(l13);

      mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
      mv.visitIincInsn(7, 1);
      mv.visitLabel(l8);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ILOAD, 7);
      mv.visitVarInsn(ILOAD, 4);
      mv.visitJumpInsn(IF_ICMPLT, l9);
      Label l14 = new Label();
      mv.visitLabel(l14);

      mv.visitVarInsn(ILOAD, 6);
      mv.visitVarInsn(ILOAD, 4);
      Label l15 = new Label();
      mv.visitJumpInsn(IF_ICMPNE, l15);
      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l15);

      mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 7);
      Label l16 = new Label();
      mv.visitLabel(l16);

      mv.visitVarInsn(ALOAD, 5);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
      Label l17 = new Label();
      mv.visitLabel(l17);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
      Label l18 = new Label();
      mv.visitLabel(l18);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn("removeAll(Ljava/util/Collection;)Z");
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      Label l19 = new Label();
      mv.visitLabel(l19);

      mv.visitInsn(ICONST_1);
      mv.visitInsn(IRETURN);
      Label l20 = new Label();
      mv.visitLabel(l20);
      mv.visitLocalVariable("this", "Ljava/util/concurrent/CopyOnWriteArrayList;",
                            "Ljava/util/concurrent/CopyOnWriteArrayList<TE;>;", l0, l20, 0);
      mv.visitLocalVariable("c", "Ljava/util/Collection;", "Ljava/util/Collection<*>;", l0, l20, 1);
      mv.visitLocalVariable("removeList", "Ljava/util/List;", null, l1, l20, 2);
      mv.visitLocalVariable("elementData", "[Ljava/lang/Object;", null, l2, l20, 3);
      mv.visitLocalVariable("len", "I", null, l3, l20, 4);
      mv.visitLocalVariable("temp", "[Ljava/lang/Object;", null, l5, l20, 5);
      mv.visitLocalVariable("newlen", "I", null, l6, l20, 6);
      mv.visitLocalVariable("i", "I", null, l7, l14, 7);
      mv.visitLocalVariable("element", "Ljava/lang/Object;", "TE;", l10, l13, 8);
      mv.visitLocalVariable("newArray", "[Ljava/lang/Object;", null, l16, l20, 7);
      mv.visitMaxs(5, 9);
      mv.visitEnd();
    }
  }

  public static class ResetLockAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = cv
          .visitMethod(this.access, this.methodName, this.description, this.signature, this.exceptions);
      if (Vm.isJDK16Compliant()) {
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "java/util/concurrent/CopyOnWriteArrayList", "unsafe", "Lsun/misc/Unsafe;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETSTATIC, "java/util/concurrent/CopyOnWriteArrayList", "lockOffset", "J");

        mv.visitTypeInsn(NEW, "com/tc/util/concurrent/locks/CopyOnWriteArrayListLock");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/concurrent/locks/CopyOnWriteArrayListLock", "<init>",
                           "(Ljava/util/concurrent/CopyOnWriteArrayList;)V");

        mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "putObjectVolatile",
                           "(Ljava/lang/Object;JLjava/lang/Object;)V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(6, 1);
        mv.visitEnd();
        return null;
      }
      return mv;
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }
}
