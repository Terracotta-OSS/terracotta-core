/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Use me to make sure a class overrides all methods from a super class and delegates the call
 */
public class DelegateMethodAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  private final Map    overrideMethods;
  private final String delegateField;
  private final String delegateType;
  private String       thisClassname;
  private String       skipMethodsFromClass;
  private final Map    skipMethods;

  // This is the real constructor for the actual adapter
  private DelegateMethodAdapter(ClassVisitor cv, Class superClass, String delegateField, Class skipFromClass) {
    super(cv);
    this.delegateField = delegateField;
    this.overrideMethods = getOverrideMethods(superClass, false);
    this.delegateType = superClass.getName().replace('.', '/');
    if (skipFromClass != null) {
      skipMethods = getOverrideMethods(skipFromClass, true);
    }
    else skipMethods = null;
  }

  // This constructor is for creating the factory
  public DelegateMethodAdapter(String delegateType, String delegateField) {
    super(null);
    this.delegateField = delegateField;
    this.delegateType = delegateType;
    this.overrideMethods = null;
    this.skipMethods = null;
  }

  public void setSkipMethodsFromClass(final String skipMethodsFromClass) {
    this.skipMethodsFromClass = skipMethodsFromClass;
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    final Class c;
    final Class skipFromClass;
    String name = null;
    try {
      name = delegateType;
      c = Class.forName(delegateType, false, loader);

      name = skipMethodsFromClass;
      skipFromClass = Class.forName(skipMethodsFromClass, false, loader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to load class " + name);
    }

    return new DelegateMethodAdapter(visitor, c, delegateField, skipFromClass);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.thisClassname = name;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String sig = name + Type.getMethodDescriptor(Type.getReturnType(desc), Type.getArgumentTypes(desc));
    overrideMethods.remove(sig);
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    if (skipMethods != null) {
      for (Iterator it = skipMethods.keySet().iterator(); it.hasNext();) {
        overrideMethods.remove(it.next());
      }
    }
    for (Iterator iter = overrideMethods.values().iterator(); iter.hasNext();) {
      Method m = (Method) iter.next();

      Class[] exceptionTypes = m.getExceptionTypes();
      String[] exceptions = new String[exceptionTypes.length];
      for (int i = 0; i < exceptions.length; i++) {
        exceptions[i] = exceptionTypes[i].getName().replace('.', '/');
      }

      Type[] argumentTypes = Type.getArgumentTypes(m);

      MethodVisitor mv = super
          .visitMethod(m.getModifiers(), m.getName(), Type.getMethodDescriptor(m), null, exceptions);
      mv.visitCode();

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, thisClassname, delegateField, "L" + delegateType + ";");

      int slot = 1;
      for (int i = 0; i < argumentTypes.length; i++) {
        Type arg = argumentTypes[i];
        mv.visitVarInsn(arg.getOpcode(ILOAD), slot);
        slot += arg.getSize();
      }

      mv.visitMethodInsn(INVOKEVIRTUAL, delegateType, m.getName(), Type.getMethodDescriptor(m));

      Type returnType = Type.getReturnType(m);
      if (returnType == Type.VOID_TYPE) {
        mv.visitInsn(RETURN);
      } else {
        mv.visitInsn(returnType.getOpcode(IRETURN));
      }

      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
    super.visitEnd();
  }

  private static Map getOverrideMethods(Class c, boolean allowFinalMethods) {
    Map rv = new HashMap();
    Method[] methods = c.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];

      int access = m.getModifiers();

      if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
        continue;
      }

      if (!allowFinalMethods && Modifier.isFinal(access)) { throw new AssertionError("Final modifier found (must be be removed): "
                                                               + m.toString()); }

      String sig = m.getName() + Type.getMethodDescriptor(m);
      Object prev = rv.put(sig, m);
      if (prev != null) { throw new AssertionError("replaced mapping for " + sig); }
    }
    return rv;
  }

}
