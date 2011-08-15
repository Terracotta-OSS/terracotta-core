/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DuplicateMethodAdapter extends ClassAdapter implements Opcodes {

  public static final String MANAGED_PREFIX   = "_managed_";
  public static final String UNMANAGED_PREFIX = ByteCodeUtil.TC_METHOD_PREFIX + "unmanaged_";

  private final Set          dontDupe;
  private String             ownerSlashes;
  private String             superClass;

  public DuplicateMethodAdapter(ClassVisitor cv) {
    this(cv, Collections.EMPTY_SET);
  }

  public DuplicateMethodAdapter(ClassVisitor cv, Set dontDupe) {
    super(cv);
    this.dontDupe = new HashSet(dontDupe);
    this.dontDupe.add("readObject(Ljava/io/ObjectInputStream;)V");
    this.dontDupe.add("writeObject(Ljava/io/ObjectOutputStream;)V");
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.ownerSlashes = name;
    this.superClass = superName;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if (name.startsWith(MANAGED_PREFIX) || name.startsWith(UNMANAGED_PREFIX)) {
      // make formatter sane
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    if ("<init>".equals(name) || "<clinit>".equals(name)) {
      // don't need any special indirection on initializers
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    if (Modifier.isStatic(access) || Modifier.isNative(access) || Modifier.isAbstract(access)) {
      // make formatter sane
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    if (dontDupe.contains(name + desc)) { return super.visitMethod(access, name, desc, signature, exceptions); }

    createSwitchMethod(access, name, desc, signature, exceptions);

    MethodVisitor managed = new RewriteSelfTypeCalls(super.visitMethod(access, MANAGED_PREFIX + name, desc, signature,
                                                                       exceptions), new String[] { ownerSlashes,
        superClass }, MANAGED_PREFIX);
    MethodVisitor unmanaged = new RewriteSelfTypeCalls(super.visitMethod(access, UNMANAGED_PREFIX + name, desc,
                                                                         signature, exceptions), new String[] {
        ownerSlashes, superClass }, UNMANAGED_PREFIX);

    return (MethodVisitor) Proxy
        .newProxyInstance(getClass().getClassLoader(), new Class[] { MethodVisitor.class },
                          new MulticastMethodVisitor(new MethodVisitor[] { managed, unmanaged }));
  }

  private void createSwitchMethod(int access, String name, String desc, String signature, String[] exceptions) {
    Type returnType = Type.getReturnType(desc);
    boolean isVoid = returnType.equals(Type.VOID_TYPE);
    MethodVisitor mv = super.visitMethod(access & (~ACC_SYNCHRONIZED), name, desc, signature, exceptions);
    Label notManaged = new Label();
    Label end = new Label();
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, ClassAdapterBase.MANAGED_METHOD, "()Lcom/tc/object/TCObject;");
    mv.visitJumpInsn(IFNULL, notManaged);
    ByteCodeUtil.prepareStackForMethodCall(access, desc, mv);
    mv.visitMethodInsn(INVOKESPECIAL, ownerSlashes, MANAGED_PREFIX + name, desc);
    if (!isVoid) {
      mv.visitInsn(Type.getReturnType(desc).getOpcode(IRETURN));
    } else {
      mv.visitJumpInsn(GOTO, end);
    }
    mv.visitLabel(notManaged);
    ByteCodeUtil.prepareStackForMethodCall(access, desc, mv);
    mv.visitMethodInsn(INVOKESPECIAL, ownerSlashes, UNMANAGED_PREFIX + name, desc);
    if (!isVoid) {
      mv.visitInsn(Type.getReturnType(desc).getOpcode(IRETURN));
    } else {
      mv.visitLabel(end);
      mv.visitInsn(RETURN);
    }

    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private class RewriteSelfTypeCalls extends MethodAdapter implements Opcodes {

    private final String[] types;
    private final String   prefix;

    public RewriteSelfTypeCalls(MethodVisitor mv, String[] types, String prefix) {
      super(mv);
      this.types = types;
      this.prefix = prefix;
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ("<init>".equals(name)) {
        super.visitMethodInsn(opcode, owner, name, desc);
        return;
      }

      if (dontDupe.contains(name + desc)) {
        super.visitMethodInsn(opcode, owner, name, desc);
        return;
      }

      if (opcode != INVOKESTATIC) {
        boolean rewrite = false;
        for (int i = 0; i < types.length; i++) {
          if (types[i].equals(owner)) {
            rewrite = true;
            break;
          }
        }

        if (rewrite) {
          name = prefix + name;
        }
      }

      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }

  private static class MulticastMethodVisitor implements InvocationHandler {

    private final MethodVisitor[] targets;

    MulticastMethodVisitor(MethodVisitor targets[]) {
      this.targets = targets;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Object rv = null;
      for (int i = 0; i < targets.length; i++) {
        rv = method.invoke(targets[i], args);
      }
      return rv;
    }

  }

}
