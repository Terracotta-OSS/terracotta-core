/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.config.ClassReplacementMapping;

public class RenameClassesAdapter extends ClassAdapter implements Opcodes {
  private final ClassReplacementMapping mapping;

  public RenameClassesAdapter(ClassVisitor cv, ClassReplacementMapping mapping) {
    super(cv);
    
    this.mapping = mapping;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    cv.visit(version, ACC_PUBLIC, mapping.getOriginalClassNameSlashes(name), signature, superName, interfaces);
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    return cv.visitField(access, name, mapping.getOriginalAsmType(desc), mapping.ensureOriginalAsmTypes(signature), value);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, mapping.ensureOriginalAsmTypes(desc), mapping.ensureOriginalAsmTypes(signature), exceptions);
    if (mv != null && (access & ACC_ABSTRACT) == 0) {
      mv = new MethodRenamer(mv);
    }
    return mv;
  }

  class MethodRenamer extends MethodAdapter {
    public MethodRenamer(final MethodVisitor mv) {
      super(mv);
    }

    public void visitTypeInsn(int i, String s) {
      mv.visitTypeInsn(i, mapping.getOriginalClassNameSlashes(s));
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      mv.visitFieldInsn(opcode, mapping.getOriginalClassNameSlashes(owner), name, mapping.ensureOriginalAsmTypes(desc));
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      mv.visitMethodInsn(opcode, mapping.getOriginalClassNameSlashes(owner), name, mapping.ensureOriginalAsmTypes(desc));
    }
    
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      mv.visitLocalVariable(name, mapping.getOriginalAsmType(desc), mapping.ensureOriginalAsmTypes(signature), start, end, index);
    }
  }
}