/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class LinkedHashMapClassAdapter extends ChangeClassNameHierarchyAdapter implements Opcodes {

  private String className;

  public LinkedHashMapClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.className = name;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if (this.className.equals("java/util/LinkedHashMap") && name.equals("addEntry")) {
      mv = new AddEntryMethodAdapter(mv);
    }

    return mv;
  }

  private static class AddEntryMethodAdapter extends MethodAdapter implements Opcodes {
    public AddEntryMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    /**
     * Wraps the argument to LinkedHashMap.addEntry(Map.Entry) in a HashMapTC.EntryWrapper prior to
     * a call to LinkedHashMap.removeEldestEntry(...) inside the method.
     * This fixes the ClassCastException thrown when an instrumented class extends java.util.LinkedHashMap
     * and overrides the removeEldestEntry method.
     */
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((opcode == INVOKEVIRTUAL)
          && (owner.equals("java/util/LinkedHashMap") && (name.equals("removeEldestEntry")) && (desc
              .equals("(Ljava/util/Map$Entry;)Z")))) {
          mv.visitInsn(POP);
          mv.visitInsn(POP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitTypeInsn(NEW, "java/util/HashMap$EntryWrapper");
          mv.visitInsn(DUP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitVarInsn(ALOAD, 5);
          mv.visitMethodInsn(INVOKESPECIAL, 
                             "java/util/HashMap$EntryWrapper", 
                             "<init>", 
                             "(Ljava/util/HashMap;Ljava/util/Map$Entry;)V");
      }
      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }
}
