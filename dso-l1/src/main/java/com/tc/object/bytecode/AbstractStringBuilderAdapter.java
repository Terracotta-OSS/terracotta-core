/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.hook.impl.JavaLangArrayHelpers;

public class AbstractStringBuilderAdapter extends ClassAdapter {
  private String stringBuilderInternalName;
  
  public AbstractStringBuilderAdapter(ClassVisitor cv, String stringBuilderClassName) {
    super(cv);
    
    this.stringBuilderInternalName = stringBuilderClassName.replace('.', '/');
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ((DuplicateMethodAdapter.MANAGED_PREFIX + "append").equals(name)) {
      if (("(I)L" + stringBuilderInternalName + ";").equals(desc)) {
        return new AppendAdapter(mv, "I");
      } else if (("(J)L" + stringBuilderInternalName + ";").equals(desc)) { return new AppendAdapter(mv, "J"); }
    }

    return mv;
  }

  private class AppendAdapter extends MethodAdapter implements Opcodes {

    private final int spaceNeededSlot;
    private final int countSlot;
    private boolean   hasVisitedOnce = false;

    public AppendAdapter(MethodVisitor mv, String type) {
      super(mv);

      if (!"J".equals(type) && !"I".equals(type)) { throw new AssertionError("bad type: " + type); }

      this.spaceNeededSlot = type.equals("I") ? 2 : 3;
      this.countSlot = type.equals("I") ? 3 : 4;
    }

    public void visitInsn(int opcode) {
      if (opcode == ARETURN) {

        // This is a hack to fix append(int) and append(long) where the argument is either Integer.MIN_VALUE
        // or Long.MIN_VALUE. In either of those cases, we do not want to call
        // JavaLangArrayHelpers.javaLangAbstractStringBuilderAppend.
        if (!hasVisitedOnce) {
          hasVisitedOnce = true;
          super.visitInsn(opcode);
          return;
        }

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, stringBuilderInternalName, ByteCodeUtil.fieldGetterMethod("value"),
                           "()[C");
        mv.visitVarInsn(ILOAD, spaceNeededSlot);
        mv.visitVarInsn(ILOAD, countSlot);
        mv.visitMethodInsn(INVOKESTATIC, JavaLangArrayHelpers.CLASS, "javaLangAbstractStringBuilderAppend", "([CII)V");

        super.visitInsn(opcode);
        return;
      }

      super.visitInsn(opcode);
    }

  }

}
