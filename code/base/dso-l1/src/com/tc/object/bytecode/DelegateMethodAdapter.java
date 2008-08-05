/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Use me to make sure a class overrides all methods from parent hierarchy and delegates the call
 */
public class DelegateMethodAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  private final Map     overrideMethods;
  private final String  delegateFieldName;
  private final String  delegateType;
  private final boolean delegateIsInterface;

  private String        delegateFieldType;
  private String        thisClassname;

  // This is the real constructor for the actual adapter
  private DelegateMethodAdapter(ClassVisitor cv, Class superClass, String delegateFieldName) {
    super(cv);
    this.delegateFieldName = delegateFieldName;
    this.overrideMethods = getOverrideMethods(superClass);
    this.delegateType = superClass.getName().replace('.', '/');
    this.delegateIsInterface = superClass.isInterface();
  }

  // This constructor is for creating the factory
  public DelegateMethodAdapter(String delegateType, String delegateField) {
    super(null);
    this.delegateFieldName = delegateField;
    this.delegateType = delegateType;
    this.overrideMethods = null;
    this.delegateIsInterface = false;
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    final Class c;
    try {
      c = Class.forName(delegateType, false, loader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to load class " + delegateType);
    }

    return new DelegateMethodAdapter(visitor, c, delegateFieldName);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.thisClassname = name;
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if (delegateFieldName.equals(name)) {
      if (delegateFieldType != null) { throw new AssertionError("field type already set: " + delegateFieldType); }
      delegateFieldType = desc;
    }
    return super.visitField(access, name, desc, signature, value);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String sig = name + Type.getMethodDescriptor(Type.getReturnType(desc), Type.getArgumentTypes(desc));
    overrideMethods.remove(sig);
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    for (Iterator iter = overrideMethods.values().iterator(); iter.hasNext();) {
      Method m = (Method) iter.next();

      Class[] exceptionTypes = m.getExceptionTypes();
      String[] exceptions = new String[exceptionTypes.length];
      for (int i = 0; i < exceptions.length; i++) {
        exceptions[i] = exceptionTypes[i].getName().replace('.', '/');
      }

      Type[] argumentTypes = Type.getArgumentTypes(m);

      MethodVisitor mv = super.visitMethod(m.getModifiers() & ~ACC_ABSTRACT, m.getName(), Type.getMethodDescriptor(m),
                                           null, exceptions);
      mv.visitCode();

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, thisClassname, delegateFieldName, delegateFieldType);

      int slot = 1;
      for (int i = 0; i < argumentTypes.length; i++) {
        Type arg = argumentTypes[i];
        mv.visitVarInsn(arg.getOpcode(ILOAD), slot);
        slot += arg.getSize();
      }

      mv.visitMethodInsn(delegateIsInterface ? INVOKEINTERFACE : INVOKEVIRTUAL, delegateType, m.getName(), Type
          .getMethodDescriptor(m));

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

  private static Map getOverrideMethods(Class c) {
    Map rv = new HashMap();

    while (c != null && c != Object.class) {
      Method[] methods = c.isInterface() ? c.getMethods() : c.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
        Method m = methods[i];

        int access = m.getModifiers();

        if (m.getName().startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) {
          continue;
        }

        if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
          continue;
        }

        if (Modifier.isFinal(access)) { throw new AssertionError("Final modifier found (must be be removed): "
                                                                 + m.toString()); }

        String sig = m.getName() + Type.getMethodDescriptor(m);
        rv.put(sig, m);
      }
      c = c.getSuperclass();
    }
    return rv;
  }

}
