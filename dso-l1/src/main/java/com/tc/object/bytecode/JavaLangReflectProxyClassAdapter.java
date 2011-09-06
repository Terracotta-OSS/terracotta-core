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

/**
 * 
 * This custom adapter instrument java.lang.reflect.Proxy class so that all DSO interfaces
 * added to the Proxy class will be filtered out in the getProxyClass method.
 */
public class JavaLangReflectProxyClassAdapter extends ClassAdapter implements Opcodes {
  private final static String FILTER_INTERFACES_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "filterInterfaces";
  private final static String PROXY_INTERFACES_FIELD_NAME = ByteCodeUtil.TC_FIELD_PREFIX + "proxyInterfaces";
  
  public JavaLangReflectProxyClassAdapter(ClassVisitor cv) {
    super(cv);
  }
  
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("getProxyClass".equals(name) && "(Ljava/lang/ClassLoader;[Ljava/lang/Class;)Ljava/lang/Class;".equals(desc)) {
      mv = new FilterInterfacesMethodAdapter(mv);
    } else if ("<clinit>".equals(name)) {
      mv = new StaticInitializerMethodAdapter(mv);
    }
    
    return mv;
  }  
  
  public void visitEnd() {
    addProxyInterfacesField();
    addFilterInterfacesMethod();
    super.visitEnd();
  }
  
  private void addProxyInterfacesField() {
    cv.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC, PROXY_INTERFACES_FIELD_NAME, "[Ljava/lang/Class;", null, null);
  }
  
  private void addFilterInterfacesMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC, FILTER_INTERFACES_METHOD_NAME, "([Ljava/lang/Class;)[Ljava/lang/Class;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitTypeInsn(NEW, "java/util/ArrayList");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l2 = new Label();
    mv.visitLabel(l2);
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 3);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 4);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 5);
    Label l7 = new Label();
    mv.visitLabel(l7);
    Label l8 = new Label();
    mv.visitJumpInsn(GOTO, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitFieldInsn(GETSTATIC, "java/lang/reflect/Proxy", PROXY_INTERFACES_FIELD_NAME, "[Ljava/lang/Class;");
    mv.visitVarInsn(ILOAD, 5);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
    Label l10 = new Label();
    mv.visitJumpInsn(IFEQ, l10);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitFieldInsn(GETSTATIC, "java/lang/reflect/Proxy", PROXY_INTERFACES_FIELD_NAME, "[Ljava/lang/Class;");
    mv.visitVarInsn(ILOAD, 5);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    mv.visitLdcInsn("com.tc");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
    mv.visitJumpInsn(IFEQ, l10);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ISTORE, 4);
    Label l13 = new Label();
    mv.visitLabel(l13);
    Label l14 = new Label();
    mv.visitJumpInsn(GOTO, l14);
    mv.visitLabel(l10);
    mv.visitIincInsn(5, 1);
    mv.visitLabel(l8);
    mv.visitVarInsn(ILOAD, 5);
    mv.visitFieldInsn(GETSTATIC, "java/lang/reflect/Proxy", PROXY_INTERFACES_FIELD_NAME, "[Ljava/lang/Class;");
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l9);
    mv.visitLabel(l14);
    mv.visitVarInsn(ILOAD, 4);
    Label l15 = new Label();
    mv.visitJumpInsn(IFNE, l15);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
    mv.visitInsn(POP);
    mv.visitLabel(l15);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l4);
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
    mv.visitVarInsn(ASTORE, 2);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitInsn(POP);
    Label l19 = new Label();
    mv.visitLabel(l19);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARETURN);
    Label l20 = new Label();
    mv.visitLabel(l20);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class FilterInterfacesMethodAdapter extends MethodAdapter implements Opcodes {
    public FilterInterfacesMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitCode() {
      super.visitCode();
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/reflect/Proxy", FILTER_INTERFACES_METHOD_NAME, "([Ljava/lang/Class;)[Ljava/lang/Class;");
      mv.visitVarInsn(ASTORE, 1);
    }
  }
  
  private static class StaticInitializerMethodAdapter extends MethodAdapter implements Opcodes {
    public StaticInitializerMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        mv.visitTypeInsn(NEW, "java/lang/reflect/Proxy");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/reflect/Proxy", "<init>", "()V");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;");
        mv.visitFieldInsn(PUTSTATIC, "java/lang/reflect/Proxy", PROXY_INTERFACES_FIELD_NAME, "[Ljava/lang/Class;");
      }
      super.visitInsn(opcode);
    }
  }

}
