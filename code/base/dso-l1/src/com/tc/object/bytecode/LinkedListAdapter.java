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
import com.tc.asm.Type;
import com.tc.object.SerializationUtil;
import com.tc.util.runtime.Vm;

public class LinkedListAdapter {

  private static final String RENAMED_REMOVE = ByteCodeUtil.TC_METHOD_PREFIX + "remove";
  private static final String RENAMED_SIG    = "(Ljava/util/LinkedList$Entry;Z)";

  public static class RemoveMethodCreator implements MethodCreator, Opcodes {

    public void createMethods(ClassVisitor cv) {
      boolean hasReturnValue = Vm.isJDK15Compliant();
      String retType = hasReturnValue ? "Ljava/lang/Object;" : "V";

      MethodVisitor mv = cv.visitMethod(ACC_PRIVATE | ACC_SYNTHETIC, RENAMED_REMOVE, RENAMED_SIG + retType, null, null);

      mv.visitCode();
      if (!hasReturnValue) {
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETFIELD, "java/util/LinkedList$Entry", "element", "Ljava/lang/Object;");
        mv.visitVarInsn(ASTORE, 4);
      }
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedList", "remove", "(Ljava/util/LinkedList$Entry;)" + retType);
      if (hasReturnValue) {
        mv.visitVarInsn(ASTORE, 3);
      }
      mv.visitVarInsn(ILOAD, 2);
      Label notShared = new Label();
      mv.visitJumpInsn(IFEQ, notShared);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn(SerializationUtil.REMOVE_SIGNATURE);
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, hasReturnValue ? 3 : 4);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notShared);
      if (hasReturnValue) {
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ARETURN);
      } else {
        mv.visitInsn(RETURN);
      }
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

  }

  public static class RemoveAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = visitOriginal(classVisitor);
      return new Adapter(mv);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
      }

      public void visitCode() {
        super.visitCode();

        ByteCodeUtil.pushThis(this);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
        super.visitVarInsn(ISTORE, 4);

        super.visitVarInsn(ILOAD, 4);
        Label notShared = new Label();
        super.visitJumpInsn(IFEQ, notShared);
        ByteCodeUtil.pushThis(this);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "checkWriteAccess",
                              "(Ljava/lang/Object;)V");
        super.visitLabel(notShared);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (opcode == INVOKESPECIAL && "remove".equals(name) && "java/util/LinkedList".equals(owner)
            && desc.startsWith("(Ljava/util/LinkedList$Entry;)")) {

          name = RENAMED_REMOVE;
          desc = "(Ljava/util/LinkedList$Entry;Z)" + Type.getReturnType(desc);
          super.visitVarInsn(ILOAD, 4);
        }

        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

  }

  public static class ListIteratorAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = visitOriginal(classVisitor);
      return new Adapter(mv);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
        mv.visitTypeInsn(NEW, "com/tc/util/ListIteratorWrapper");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
      }

      public void visitInsn(int opcode) {
        if (ARETURN == opcode) {
          mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/ListIteratorWrapper", "<init>",
                             "(Ljava/util/List;Ljava/util/ListIterator;)V");
        }
        super.visitInsn(opcode);
      }

    }
  }
}
