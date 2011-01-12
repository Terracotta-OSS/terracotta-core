/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tcclient.util.DSOUnsafe;

import java.lang.reflect.Modifier;

public class UnsafeAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  private final static String UNSAFE_CLASS_SLASH   = "sun/misc/Unsafe";
  public final static String TC_UNSAFE_FIELD_NAME = ByteCodeUtil.TC_FIELD_PREFIX + "theUnsafe";

  public UnsafeAdapter() {
    super(null);
  }

  private UnsafeAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new UnsafeAdapter(visitor, loader);
  }

  public final void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    if (UNSAFE_CLASS_SLASH.equals(name)) {
      access = ~Modifier.FINAL & access;
    }

    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    access = access & ~Modifier.FINAL;

    // DSOUnsafe.<init> will get verify error calling super() if cstr() is not accessible
    if ("<init>".equals(name)) {
      access = access & ~Modifier.PRIVATE;
      access |= Modifier.PROTECTED;
    }

    if ("<clinit>".equals(name)) {
      return new UnsafeMethodAdapter(super.visitMethod(access, name, desc, signature, exceptions));
    } else {
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }

  private class UnsafeMethodAdapter extends MethodAdapter implements Opcodes {
    public UnsafeMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String className, String methodName, String desc) {
      if (Opcodes.INVOKESPECIAL == opcode && UNSAFE_CLASS_SLASH.equals(className) && "<init>".equals(methodName)) {
        super.visitMethodInsn(opcode, DSOUnsafe.CLASS_SLASH, methodName, desc);
      } else {
        super.visitMethodInsn(opcode, className, methodName, desc);
      }
    }

    public void visitTypeInsn(int opcode, String desc) {
      if (NEW == opcode && UNSAFE_CLASS_SLASH.equals(desc)) {
        super.visitTypeInsn(NEW, DSOUnsafe.CLASS_SLASH);
      } else {
        super.visitTypeInsn(opcode, desc);
      }
    }
  }
}
