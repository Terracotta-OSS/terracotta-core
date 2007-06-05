/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.hook.impl.JavaLangArrayHelpers;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tc.util.runtime.Vm.Version;

public class JavaLangStringAdapter extends ClassAdapter implements Opcodes {

  private final Version vmVersion;
  private final boolean portableStringBuffer;

  public JavaLangStringAdapter(ClassVisitor cv, Version vmVersion, boolean portableStringBuffer) {
    super(cv);
    this.vmVersion = vmVersion;
    this.portableStringBuffer = portableStringBuffer;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("getBytes".equals(name) && "(II[BI)V".equals(desc)) {
      return rewriteGetBytes(mv);
    } else if ("<init>".equals(name) && "(Ljava/lang/StringBuffer;)V".equals(desc)) {
      if (vmVersion.isJDK14() && portableStringBuffer) { return rewriteStringBufferConstructor(mv); }
    } else if ("getChars".equals(name) && "(II[CI)V".equals(desc)) {
      // make formatter sane
      return new GetCharsAdapter(mv);
    } else if ("getChars".equals(name) && "([CI)V".equals(desc)) {
      // This method is in the 1.5 Sun impl of String
      return new GetCharsAdapter(mv);
    }

    return new RewriteGetCharsCallsAdapter(mv);
  }

  public void visitEnd() {
    addFastGetChars();
    super.visitEnd();
  }

  private void addFastGetChars() {
    // Called by the unmanaged paths of StringBuffer, StringBuilder, etc. Also called it strategic places where the
    // target char[] is known (or assumed) to be non-shared
    MethodVisitor mv = super.visitMethod(ACC_SYNTHETIC | ACC_PUBLIC, "getCharsFast", "(II[CI)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInsn(IADD);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInsn(ISUB);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // Called from (Abstract)StringBuilder.insert|repace()
    mv = super.visitMethod(ACC_SYNTHETIC, "getCharsFast", "([CI)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private MethodVisitor rewriteStringBufferConstructor(MethodVisitor mv) {
    // move the sync into StringBuffer.toString() where it belongs
    Assert.assertTrue(Vm.isJDK14());
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "value", "[C");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "count", "I");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "offset", "I");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    return null;
  }

  private MethodVisitor rewriteGetBytes(MethodVisitor mv) {
    mv.visitCode();
    mv.visitVarInsn(ILOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitMethodInsn(INVOKESTATIC, JavaLangArrayHelpers.CLASS, "javaLangStringGetBytes", "(II[BIII[C)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    return null;
  }

  private static class RewriteGetCharsCallsAdapter extends MethodAdapter {

    public RewriteGetCharsCallsAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((INVOKEVIRTUAL == opcode) && ("java/lang/String".equals(owner) && "getChars".equals(name))) {
        super.visitMethodInsn(opcode, owner, "getCharsFast", desc);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

  }

  private static class GetCharsAdapter extends MethodAdapter {

    public GetCharsAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((opcode == INVOKESTATIC) && "java/lang/System".equals(owner) && "arraycopy".equals(name)) {
        super.visitMethodInsn(INVOKESTATIC, JavaLangArrayHelpers.CLASS, "charArrayCopy", "([CI[CII)V");
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

  }

}
