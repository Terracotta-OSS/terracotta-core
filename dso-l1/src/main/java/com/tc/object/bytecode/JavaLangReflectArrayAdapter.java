/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.exception.TCRuntimeException;
import com.tc.util.runtime.Vm;

import java.util.HashSet;
import java.util.Set;

public class JavaLangReflectArrayAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  private final static Set nonNativeMethods         = new HashSet(2);
  private final static Set excludeMethods           = new HashSet(9);
  private final static Set includedPrivateMethods   = new HashSet(1);

  static {
    nonNativeMethods.add("newInstance");
    nonNativeMethods.add("<init>");
    // the IBM JDK just delegates to their own setImpl native version
    if (Vm.isIBM()) {
      nonNativeMethods.add("set");
      includedPrivateMethods.add("setImpl");
    }

    excludeMethods.add("getLength");
    excludeMethods.add("getByte");
    excludeMethods.add("getBoolean");
    excludeMethods.add("getChar");
    excludeMethods.add("getDouble");
    excludeMethods.add("getFloat");
    excludeMethods.add("getInt");
    excludeMethods.add("getLong");
    excludeMethods.add("getShort");
  }

  public JavaLangReflectArrayAdapter() {
    super(null);
  }
  
  private JavaLangReflectArrayAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new JavaLangReflectArrayAdapter(visitor, loader);
  }
  
  private boolean isNative(int access) {
    return (access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE;
  }

  private boolean isPrivate(int access) {
    return (access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
  }

  public MethodVisitor visitMethod(int access, String name, String description, String signature, String[] exceptions) {
    if (!isNative(access)) {
      if (!nonNativeMethods.contains(name)) {
        throw new TCRuntimeException("Unexpected non-native method: " + name + description);
      } else {
        return super.visitMethod(access, name, description, signature, exceptions);
      }
    }

    if (isPrivate(access) && !includedPrivateMethods.contains(name)) {
      return super.visitMethod(access, name, description, signature, exceptions);
    } else if (isNative(access) && !excludeMethods.contains(name)) {
      MethodVisitor mv = super.visitMethod(access ^ Opcodes.ACC_NATIVE, name, description, signature, exceptions);
      addArrayUtilMethodCode(mv, name, description);
      return null;
    } else {
      return super.visitMethod(access, name, description, signature, exceptions);
    }
  }

  private void addArrayUtilMethodCode(MethodVisitor mv, String methodName, String description) {
    mv.visitCode();

    Type[] params = Type.getArgumentTypes(description);
    Type returnType = Type.getReturnType(description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i);
    }
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, methodName, description);
    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

}
