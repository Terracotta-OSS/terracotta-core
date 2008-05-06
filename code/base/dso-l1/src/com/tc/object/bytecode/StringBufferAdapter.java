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
import com.tc.util.runtime.VmVersion;

public class StringBufferAdapter extends ClassAdapter implements Opcodes {

  private final VmVersion version;

  public StringBufferAdapter(ClassVisitor cv, VmVersion version) {
    super(cv);
    this.version = version;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("toString".equals(name) && "()Ljava/lang/String;".equals(desc) && version.isJDK14()) {
      rewriteToString1_4(mv);
      return null;
    }

    return mv;
  }

  private void rewriteToString1_4(MethodVisitor mv) {
    // This code is from the String(StringBuffer) constructor. It's easier to do the autolocking here, instead of in
    // String -- otherwise the code is exactly the same
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(12, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitVarInsn(ASTORE, 1);
    mv.visitInsn(MONITORENTER);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(13, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "setShared", "()V");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(14, l2);
    mv.visitTypeInsn(NEW, "java/lang/String");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/StringBuffer", "count", "I");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/StringBuffer", "value", "[C");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "(II[C)V");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(MONITOREXIT);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitInsn(ARETURN);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(12, l4);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(MONITOREXIT);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitInsn(ATHROW);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitTryCatchBlock(l1, l3, l4, null);
    mv.visitTryCatchBlock(l4, l5, l4, null);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  public static class FixUp extends ClassAdapter {

    private static final String MANAGED_APPEND = DuplicateMethodAdapter.MANAGED_PREFIX + "append";
    private final VmVersion       version;

    public FixUp(ClassVisitor cv, VmVersion version) {
      super(cv);
      this.version = version;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);

      if (MANAGED_APPEND.equals(name) && "([CII)Ljava/lang/StringBuffer;".equals(desc) && version.isJDK14()) {
        // comment to make formatter sane
        return new CloneCharArrayFixup(visitor);
      }

      return visitor;
    }

    // Because the char[] argument to StringBuffer.append([CII) is potentially a threadlocal (see Long.appendTo(long,
    // StringBuffer) in JDK1.4), we need to make a defensive copy before calling anymore code that can modify it before
    // it gets appended into this StringBuffer. Specifically, our code to get the DSO monitor was corrupting the char[]
    // from the ThreadLocal
    private static class CloneCharArrayFixup extends MethodAdapter {
      public CloneCharArrayFixup(MethodVisitor mv) {
        super(mv);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "clone", "()Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, "[C");
        mv.visitVarInsn(ASTORE, 1);
      }
    }
  }

}
