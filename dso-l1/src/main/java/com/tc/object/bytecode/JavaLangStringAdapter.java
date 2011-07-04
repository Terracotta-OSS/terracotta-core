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
import com.tc.object.bytecode.hook.impl.JavaLangArrayHelpers;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tc.util.runtime.VmVersion;

public class JavaLangStringAdapter extends ClassAdapter implements Opcodes {

  private static final String GET_VALUE_METHOD      = ByteCodeUtil.fieldGetterMethod("value");
  private static final String INTERN_FIELD_NAME     = ByteCodeUtil.TC_FIELD_PREFIX + "interned";
  private static final String COMPRESSED_FIELD_NAME = ByteCodeUtil.TC_FIELD_PREFIX + "compressed";

  private final VmVersion     vmVersion;
  private final boolean       portableStringBuffer;
  private final boolean       isAzul;
  private final boolean       isIBM;

  public JavaLangStringAdapter(ClassVisitor cv, VmVersion vmVersion, boolean portableStringBuffer, boolean isAzul,
                               boolean isIBM) {
    super(cv);
    this.vmVersion = vmVersion;
    this.portableStringBuffer = portableStringBuffer;
    this.isAzul = isAzul;
    this.isIBM = isIBM;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { "com/tc/object/bytecode/JavaLangStringTC" });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("getBytes".equals(name) && "(II[BI)V".equals(desc)) {
      mv = rewriteGetBytes(mv);
    } else if ("<init>".equals(name) && "(Ljava/lang/StringBuffer;)V".equals(desc)) {
      if (vmVersion.isJDK14() && portableStringBuffer) {
        mv = rewriteStringBufferConstructor(mv);
      }
    } else if ("getChars".equals(name) && "(II[CI)V".equals(desc)) {
      // make formatter sane
      mv = new GetCharsAdapter(mv);
    } else if ("getChars".equals(name) && "([CI)V".equals(desc)) {
      // This method is in the 1.5 Sun impl of String
      mv = new GetCharsAdapter(mv);
    } else {
      mv = new RewriteGetCharsCallsAdapter(mv);
    }

    if (mv != null) {
      mv = new DecompressCharsAdapter(mv);
    }

    return mv;
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if ("value".equals(name)) {
      // Remove final modifier and add volatile on char[] value. We will need to modify
      // this field when decompressing a string value.
      return super.visitField(ACC_PRIVATE + ACC_VOLATILE, "value", "[C", null, null);
    } else {
      return super.visitField(access, name, desc, signature, value);
    }
  }

  public void visitEnd() {
    addCompressionField();
    addCompressedConstructor();

    addGetValueMethod();
    addFastGetChars();
    addStringTCMethods();
    addStringInternTCNature();
    super.visitEnd();
  }

  private void addStringTCMethods() {

    // public boolean __tc_isCompressed()
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.TC_METHOD_PREFIX + "isCompressed", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", COMPRESSED_FIELD_NAME, "Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

    // public void __tc_decompress
    mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.TC_METHOD_PREFIX + "decompress", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
    mv.visitInsn(POP);
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addStringInternTCNature() {
    // private boolean $__tc_interned;
    super.visitField(ACC_PRIVATE + ACC_VOLATILE + ACC_TRANSIENT, INTERN_FIELD_NAME, "Z", null, null);

    // public String __tc_intern(String) - TC version of String.intern()
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.TC_METHOD_PREFIX + "intern", "()Ljava/lang/String;",
                                         null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "__tc_decompress", "()V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ICONST_1);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", INTERN_FIELD_NAME, "Z");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();

    // public boolean __tc_isInterned() - implementation of JavaLangStringTC Interface
    mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.TC_METHOD_PREFIX + "isInterned", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", INTERN_FIELD_NAME, "Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addCompressionField() {
    // private volatile boolean $__tc_compressed = false;
    super.visitField(ACC_PRIVATE + ACC_VOLATILE + ACC_TRANSIENT, COMPRESSED_FIELD_NAME, "Z", null, null);
  }

  private void addCompressedConstructor() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "<init>", "(Z[CII)V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    if (isIBM) { // IBM named theirs "hashCode" while Sun, etc use "hash"
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "hashCode", "I");
    } else {
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "hash", "I");
    }
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "value", "[C");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 4);
    if (isIBM) { // IBM named theirs "hashCode" while Sun,etc use "hash"
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "hashCode", "I");
    } else {
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "hash", "I");
    }
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "count", "I");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", COMPRESSED_FIELD_NAME, "Z");
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", INTERN_FIELD_NAME, "Z");
    Label l7 = new Label();
    mv.visitLabel(l7);
    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ICONST_0);
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "offset", "I");
    }
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitInsn(RETURN);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLocalVariable("this", "Ljava/lang/String;", null, l0, l9, 0);
    mv.visitLocalVariable("compressed", "Z", null, l0, l9, 1);
    mv.visitLocalVariable("compressedData", "[C", null, l0, l9, 2);
    mv.visitLocalVariable("uncompressedLength", "I", null, l0, l9, 3);
    if (isIBM) {
      mv.visitLocalVariable("hashCode", "I", null, l0, l9, 4);
    } else {
      mv.visitLocalVariable("hash", "I", null, l0, l9, 4);
    }
    mv.visitMaxs(2, 5);
    mv.visitEnd();
  }

  /**
   * private char[] __tc_getvalue() { 
   *    if ($__tc_compressed){ 
   *      byte[] uncompressed = StringCompressionUtil.unpackAndDecompress(value); 
   *      if (uncompressed != null) { 
   *        try { 
   *            value =  StringCoding.decode("UTF-8", uncompressed, 0, uncompressed.length); 
   *        } catch (UnsupportedEncodingException e) {
   *            //NOTE: Java 1.4 AssertionError does not have a constructor taking a (Throwable)
   *            throw new AssertionError(e.getMessage()); 
   *        } 
   *      $__tc_compressed=false; 
   *      } 
   *    } 
   *    return value; 
   * }
   */
  private void addGetValueMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, GET_VALUE_METHOD, "()[C", null, null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "java/io/UnsupportedEncodingException");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(14, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", COMPRESSED_FIELD_NAME, "Z");
    Label l4 = new Label();
    mv.visitJumpInsn(IFEQ, l4);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(15, l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/compression/StringCompressionUtil", "unpackAndDecompress", "([C)[B");
    mv.visitVarInsn(ASTORE, 1);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(16, l6);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitJumpInsn(IFNULL, l4);
    mv.visitLabel(l0);
    mv.visitLineNumber(18, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitLdcInsn("UTF-8");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/StringCoding", "decode", "(Ljava/lang/String;[BII)[C");
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "value", "[C");
    mv.visitLabel(l1);
    Label l7 = new Label();
    mv.visitJumpInsn(GOTO, l7);
    mv.visitLabel(l2);
    mv.visitLineNumber(19, l2);
    mv.visitVarInsn(ASTORE, 2);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(20, l8);
    mv.visitTypeInsn(NEW, "java/lang/AssertionError");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/UnsupportedEncodingException", "getMessage", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l7);
    mv.visitLineNumber(22, l7);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", COMPRESSED_FIELD_NAME, "Z");
    mv.visitLabel(l4);
    mv.visitLineNumber(25, l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitInsn(ARETURN);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLocalVariable("this", "Ljava/lang/String;", null, l3, l9, 0);
    mv.visitLocalVariable("uncompressed", "[B", null, l6, l4, 1);
    mv.visitLocalVariable("e", "Ljava/io/UnsupportedEncodingException;", null, l8, l7, 2);
    mv.visitMaxs(5, 3);
    mv.visitEnd();
  }

  private void addFastGetChars() {
    // Called by the unmanaged paths of StringBuffer, StringBuilder, etc. Also called it strategic places where the
    // target char[] is known (or assumed) to be non-shared
    MethodVisitor mv = visitMethod(ACC_SYNTHETIC | ACC_PUBLIC, "getCharsFast", "(II[CI)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    }
    mv.visitVarInsn(ILOAD, 1);
    if (!isAzul) {
      mv.visitInsn(IADD);
    }
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInsn(ISUB);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // Called from (Abstract)StringBuilder.insert|replace()
    mv = visitMethod(ACC_SYNTHETIC, "getCharsFast", "([CI)V", null, null);
    mv.visitCode();
    if (isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
      mv.visitInsn(ARRAYLENGTH);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
      mv.visitInsn(RETURN);
    } else {
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
    }
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
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "value", "[C");
    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "count", "I");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "offset", "I");
    }
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    return null;
  }

  private MethodVisitor rewriteGetBytes(MethodVisitor mv) {
    mv.visitCode();
    mv.visitVarInsn(ILOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);

    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    }

    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
    mv.visitMethodInsn(INVOKESTATIC, JavaLangArrayHelpers.CLASS, "javaLangStringGetBytes", isAzul ? "(II[BI[C)V"
        : "(II[BIII[C)V");
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

  private static class DecompressCharsAdapter extends MethodAdapter {
    public DecompressCharsAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (opcode == GETFIELD && "java/lang/String".equals(owner) && "value".equals(name)) {
        String gDesc = "()" + desc;
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, gDesc);
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }
  }

}
