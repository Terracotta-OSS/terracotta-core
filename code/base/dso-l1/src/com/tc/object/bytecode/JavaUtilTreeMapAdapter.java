/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.lang.reflect.Modifier;

public class JavaUtilTreeMapAdapter extends ClassAdapter {

  public JavaUtilTreeMapAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("writeObject".equals(name) && Modifier.isPrivate(access)) { return new WriteObjectAdapter(mv); }

    return mv;
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
