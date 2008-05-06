/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.lang.reflect.Modifier;

public class JavaUtilTreeMapAdapter extends ClassAdapter implements Opcodes {

  public JavaUtilTreeMapAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("writeObject".equals(name) && Modifier.isPrivate(access)) { return new WriteObjectAdapter(mv); }

    return mv;
  }

  public void visitEnd() {
    addRemoveEntryForKey();
    super.visitEnd();
  }

  private void addRemoveEntryForKey() {
    MethodVisitor mv = super.visitMethod(ACC_SYNTHETIC, "removeEntryForKey",
                                         "(Ljava/lang/Object;)Ljava/util/TreeMap$Entry;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/TreeMap", "getEntry", "(Ljava/lang/Object;)Ljava/util/TreeMap$Entry;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 2);
    Label entryNotNull = new Label();
    mv.visitJumpInsn(IFNONNULL, entryNotNull);
    mv.visitInsn(ACONST_NULL);
    mv.visitInsn(ARETURN);
    mv.visitLabel(entryNotNull);
    mv.visitTypeInsn(NEW, "java/util/TreeMap$Entry");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getKey", "()Ljava/lang/Object;");
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getValue", "()Ljava/lang/Object;");
    mv.visitInsn(ACONST_NULL);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/TreeMap$Entry", "<init>",
                       "(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/TreeMap$Entry;)V");
    mv.visitVarInsn(ASTORE, 3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/TreeMap", "deleteEntry", "(Ljava/util/TreeMap$Entry;)V");
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class WriteObjectAdapter extends MethodAdapter implements Opcodes {

    public WriteObjectAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (opcode == GETFIELD) {
        if ("java/util/TreeMap$Entry".equals(owner)) {
          if ("key".equals(name)) {
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;");
          } else if ("value".equals(name)) {
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;");
          } else {
            throw new AssertionError("unknown field name: " + name);
          }
          return;
        }
      }

      super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitTypeInsn(int opcode, String desc) {
      if (CHECKCAST == opcode) {
        if ("java/util/TreeMap$Entry".equals(desc)) {
          super.visitTypeInsn(opcode, "java/util/Map$Entry");
          return;
        }
      }

      super.visitTypeInsn(opcode, desc);
    }

  }

}
