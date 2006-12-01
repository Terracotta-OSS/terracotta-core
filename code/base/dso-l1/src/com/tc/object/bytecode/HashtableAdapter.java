/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;


import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tcclient.util.HashtableEntrySetWrapper;

public class HashtableAdapter {

  public static class EntrySetAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = classVisitor.visitMethod(access, methodName, description, signature, exceptions);
      return new Adapter(mv);
    }

    public boolean doesOriginalNeedAdapting() {
      return true;
    }

    public static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if ((INVOKESTATIC == opcode) && ("java/util/Collections".equals(owner)) && ("synchronizedSet".equals(name))) {
          mv.visitInsn(POP);
          mv.visitVarInsn(ASTORE, 1);
          mv.visitTypeInsn(NEW, HashtableEntrySetWrapper.CLASS);
          mv.visitInsn(DUP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitVarInsn(ALOAD, 1);
          mv.visitMethodInsn(INVOKESPECIAL, HashtableEntrySetWrapper.CLASS, "<init>",
                             "(Ljava/util/Map;Ljava/util/Set;)V");
          mv.visitVarInsn(ALOAD, 0);
        }

        super.visitMethodInsn(opcode, owner, name, desc);
      }

    }

  }

  public static class KeySetAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = classVisitor.visitMethod(access, methodName, description, signature, exceptions);
      return new Adapter(mv);
    }

    public boolean doesOriginalNeedAdapting() {
      return true;
    }

    public static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if ((INVOKESTATIC == opcode) && ("java/util/Collections".equals(owner)) && ("synchronizedSet".equals(name))) {
          mv.visitInsn(POP);
          mv.visitVarInsn(ASTORE, 1);
          mv.visitTypeInsn(NEW, "com/tc/util/HashtableKeySetWrapper");
          mv.visitInsn(DUP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitVarInsn(ALOAD, 1);
          mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/HashtableKeySetWrapper", "<init>",
                             "(Ljava/util/Hashtable;Ljava/util/Set;)V");
          mv.visitVarInsn(ALOAD, 0);
        }

        super.visitMethodInsn(opcode, owner, name, desc);
      }

    }

  }

  public static class ValuesAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = classVisitor.visitMethod(access, methodName, description, signature, exceptions);
      return new Adapter(mv);
    }

    public boolean doesOriginalNeedAdapting() {
      return true;
    }

    public static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if ((INVOKESTATIC == opcode) && ("java/util/Collections".equals(owner))
            && ("synchronizedCollection".equals(name))) {
          mv.visitInsn(POP);
          mv.visitVarInsn(ASTORE, 1);
          mv.visitTypeInsn(NEW, "com/tc/util/HashtableValuesWrapper");
          mv.visitInsn(DUP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitVarInsn(ALOAD, 1);
          mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/HashtableValuesWrapper", "<init>",
                             "(Ljava/util/Hashtable;Ljava/util/Collection;)V");
          mv.visitVarInsn(ALOAD, 0);
        }

        super.visitMethodInsn(opcode, owner, name, desc);
      }

    }
  }

  public static class IteratorAdapter extends AbstractMethodAdapter {
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = classVisitor.visitMethod(access, methodName, description, signature, exceptions);
      return new Adapter(access, description, mv);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    public static class Adapter extends LocalVariablesSorter implements Opcodes {

      public Adapter(final int access, final String desc, final MethodVisitor mv) {
        super(access, desc, mv);
      }

      public void visitInsn(final int opcode) {
        if (MONITORENTER == opcode) {
          int newVarIndex = newLocal(1);
          mv.visitInsn(DUP);
          mv.visitInsn(DUP);
          mv.visitVarInsn(ASTORE, newVarIndex);
          mv.visitInsn(ICONST_2);
          mv.visitVarInsn(ALOAD, newVarIndex);

          mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "monitorEnter",
                             "(Ljava/lang/Object;ILcom/tc/object/bytecode/Manager;)V");
        }
        super.visitInsn(opcode);
      }
    }

  }
}
