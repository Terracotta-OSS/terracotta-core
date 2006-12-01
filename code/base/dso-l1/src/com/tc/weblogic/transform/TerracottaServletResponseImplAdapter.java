/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic.transform;

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

public class TerracottaServletResponseImplAdapter extends ClassAdapter implements Opcodes {

  private final Map nativeMethods;
  private String    thisClassname;

  public TerracottaServletResponseImplAdapter(ClassVisitor cv, ClassLoader caller) throws Exception {
    super(cv);
    this.nativeMethods = getNativeMethods(caller);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.thisClassname = name;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String sig = name + Type.getMethodDescriptor(Type.getReturnType(desc), Type.getArgumentTypes(desc));
    nativeMethods.remove(sig);
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    for (Iterator iter = nativeMethods.values().iterator(); iter.hasNext();) {
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
      mv.visitFieldInsn(GETFIELD, thisClassname, "nativeResponse", "Lweblogic/servlet/internal/ServletResponseImpl;");

      int slot = 1;
      for (int i = 0; i < argumentTypes.length; i++) {
        Type arg = argumentTypes[i];
        mv.visitVarInsn(arg.getOpcode(ILOAD), slot);
        slot += arg.getSize();
      }

      mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/ServletResponseImpl", m.getName(), Type
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
  }

  private static Map getNativeMethods(ClassLoader caller) throws Exception {
    Map rv = new HashMap();
    Class c = Class.forName("weblogic.servlet.internal.ServletResponseImpl", false, caller);

    Method[] methods = c.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];

      int access = m.getModifiers();

      if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
        continue;
      }

      if (Modifier.isFinal(access)) { throw new RuntimeException("Final modifier found (should have been removed): "
                                                                 + m.toString()); }

      String sig = m.getName() + Type.getMethodDescriptor(m);
      Object prev = rv.put(sig, m);
      if (prev != null) { throw new AssertionError("replaced mapping for " + sig); }
    }

    return rv;
  }
}
