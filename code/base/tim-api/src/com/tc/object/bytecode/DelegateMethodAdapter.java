/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
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
  private final boolean crossLoader;

  private String        delegateFieldType;
  private String        thisClassname;
  private boolean       helperInjected;

  // This is the real constructor for the actual adapter
  private DelegateMethodAdapter(ClassVisitor cv, Class superClass, String delegateFieldName, boolean crossLoader) {
    super(cv);
    this.delegateFieldName = delegateFieldName;
    this.crossLoader = crossLoader;
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
    this.crossLoader = false;
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    final Class delegateClass;
    try {
      delegateClass = Class.forName(delegateType, false, loader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to load class " + delegateType);
    }

    return new DelegateMethodAdapter(visitor, delegateClass, delegateFieldName,
                                     delegateClass.getClassLoader() != loader);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.thisClassname = name;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if (delegateFieldName.equals(name)) {
      if (delegateFieldType != null) { throw new AssertionError("field type already set: " + delegateFieldType); }
      delegateFieldType = desc;
    }
    return super.visitField(access, name, desc, signature, value);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String sig = name + Type.getMethodDescriptor(Type.getReturnType(desc), Type.getArgumentTypes(desc));

    if (!Modifier.isPublic(access) && !Modifier.isPrivate(access) && !Modifier.isStatic(access) && crossLoader) {
      // This scenario will just produce a VerifyError instead if you don't throw an Assertion
      throw new AssertionError("cross loader protected/package-private method present in original class: "
                               + thisClassname + ", " + sig);
    }

    overrideMethods.remove(sig);
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  @Override
  public void visitEnd() {
    for (Iterator iter = overrideMethods.values().iterator(); iter.hasNext();) {
      Method method = (Method) iter.next();

      Class[] exceptionTypes = method.getExceptionTypes();
      String[] exceptions = new String[exceptionTypes.length];
      for (int i = 0; i < exceptions.length; i++) {
        exceptions[i] = exceptionTypes[i].getName().replace('.', '/');
      }

      Type[] argumentTypes = Type.getArgumentTypes(method);

      boolean normalBody = true;

      int access = method.getModifiers();
      access = access & ~ACC_ABSTRACT;

      if (!Modifier.isPublic(access) && !Modifier.isPrivate(access) && crossLoader) {
        if (Modifier.isProtected(access)) {
          normalBody = false;
          access = ACC_PUBLIC;
        } else {
          throw new AssertionError("Package private method cannot be delegated cross-loader: " + method);
        }
      }

      String desc = Type.getMethodDescriptor(method);

      LocalVariablesSorter mv = new LocalVariablesSorter(access, desc, super.visitMethod(access, method.getName(),
                                                                                         desc, null, exceptions));
      mv.visitCode();

      if (normalBody) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, thisClassname, delegateFieldName, delegateFieldType);

        int slot = 1;
        for (Type arg : argumentTypes) {
          mv.visitVarInsn(arg.getOpcode(ILOAD), slot);
          slot += arg.getSize();
        }

        mv.visitMethodInsn(delegateIsInterface ? INVOKEINTERFACE : INVOKEVIRTUAL, delegateType, method.getName(), Type
            .getMethodDescriptor(method));

        Type returnType = Type.getReturnType(method);
        if (returnType == Type.VOID_TYPE) {
          mv.visitInsn(RETURN);
        } else {
          mv.visitInsn(returnType.getOpcode(IRETURN));
        }
      } else {
        injectHelper();
        reflectiveBody(mv, method, argumentTypes);
      }

      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
    super.visitEnd();
  }

  private void reflectiveBody(LocalVariablesSorter mv, Method m, Type[] argumentTypes) {
    int indexSlot = mv.newLocal(Type.INT_TYPE);
    int argTypesSlot = mv.newLocal(Type.getType(String[].class));
    int argsSlot = mv.newLocal(Type.getType(Object[].class));
    int numParams = m.getParameterTypes().length;

    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, indexSlot);

    mv.visitLdcInsn(new Integer(numParams));
    mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
    mv.visitVarInsn(ASTORE, argTypesSlot);

    mv.visitLdcInsn(new Integer(numParams));
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitVarInsn(ASTORE, argsSlot);

    for (Class argType : m.getParameterTypes()) {
      mv.visitVarInsn(ALOAD, argTypesSlot);
      mv.visitVarInsn(ILOAD, indexSlot);
      mv.visitIincInsn(indexSlot, 1);
      mv.visitLdcInsn(argType.getName());
      mv.visitInsn(AASTORE);
    }

    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, indexSlot);

    int paramSlot = 1;
    for (int i = 0; i < numParams; i++) {
      mv.visitVarInsn(ALOAD, argsSlot);
      mv.visitVarInsn(ILOAD, indexSlot);
      mv.visitIincInsn(indexSlot, 1);
      mv.visitVarInsn(argumentTypes[i].getOpcode(ILOAD), paramSlot);
      paramSlot += argumentTypes[i].getSize();

      switch (argumentTypes[i].getSort()) {
        case Type.BOOLEAN: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
          break;
        }
        case Type.BYTE: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
          break;
        }
        case Type.CHAR: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
          break;
        }
        case Type.DOUBLE: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
          break;
        }
        case Type.FLOAT: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
          break;
        }
        case Type.INT: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
          break;
        }
        case Type.LONG: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
          break;
        }
        case Type.SHORT: {
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
          break;
        }
      }

      mv.visitInsn(AASTORE);
    }

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, thisClassname, delegateFieldName, delegateFieldType);
    mv.visitLdcInsn(m.getName());
    mv.visitVarInsn(ALOAD, argTypesSlot);
    mv.visitVarInsn(ALOAD, argsSlot);
    mv
        .visitMethodInsn(INVOKESTATIC, thisClassname, ByteCodeUtil.TC_METHOD_PREFIX + "reflectiveInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");

    Type returnType = Type.getReturnType(m);
    if (returnType == Type.VOID_TYPE) {
      mv.visitInsn(RETURN);
    } else {
      switch (returnType.getSort()) {
        case Type.BOOLEAN: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
          break;
        }
        case Type.BYTE: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
          break;
        }
        case Type.CHAR: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
          break;
        }
        case Type.DOUBLE: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
          break;
        }
        case Type.FLOAT: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
          break;
        }
        case Type.INT: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
          break;
        }
        case Type.LONG: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
          break;
        }
        case Type.SHORT: {
          mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
          break;
        }
        default: {
          mv.visitTypeInsn(CHECKCAST, returnType.getInternalName());
          break;
        }
      }
      mv.visitInsn(returnType.getOpcode(IRETURN));
    }
  }

  private void injectHelper() {
    if (helperInjected) return;
    helperInjected = true;

    inject("reflectiveInvoke");
  }

  private void inject(String methodName) {
    MethodNode mn = getMethodNode(methodName);
    mn.name = ByteCodeUtil.TC_METHOD_PREFIX + methodName;
    mn.access = ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC;
    mn.accept(this);
  }

  private MethodNode getMethodNode(String methodName) {
    try {
      String res = getClass().getName().replace('.', '/').concat(".class");
      ClassReader reader = new ClassReader(getClass().getClassLoader().getResourceAsStream(res));

      ClassNode classNode = new ClassNode();
      reader.accept(classNode, 0);

      for (Iterator iter = classNode.methods.iterator(); iter.hasNext();) {
        MethodNode mn = (MethodNode) iter.next();
        if (mn.name.equals(methodName)) { return mn; }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    throw new NoSuchMethodError(methodName);
  }

  static Object reflectiveInvoke(Object o, String name, String[] argTypes, Object[] args) {
    Method method = null;
    Class c = o.getClass();
    while (c != null) {
      for (Method m : c.getDeclaredMethods()) {
        if (m.getName().equals(name)) {
          Class<?>[] parameterTypes = m.getParameterTypes();
          if (parameterTypes.length != argTypes.length) {
            continue;
          }

          boolean match = true;
          for (int i = 0; i < argTypes.length; i++) {
            if (!argTypes[i].equals(parameterTypes[i].getName())) {
              match = false;
              break;
            }
          }

          if (match) {
            method = m;
          }
        }
      }

      c = c.getSuperclass();
    }

    if (method == null) { throw new RuntimeException("No such method " + name + Arrays.asList(argTypes)); }

    method.setAccessible(true);
    try {
      return method.invoke(o, args);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Map getOverrideMethods(Class c) {
    Map rv = new HashMap();

    while (c != null && c != Object.class) {
      Method[] methods = c.isInterface() ? c.getMethods() : c.getDeclaredMethods();
      for (Method m : methods) {
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
