/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.applicator.TCURL;

import java.net.URL;

public class JavaNetURLAdapter extends ClassAdapter implements Opcodes {

  public final static String TCSET_METHOD_NAME = "__tc_set";
  public final static String TCSET_LOGICAL_METHOD_NAME = "__tc_set_logical";
  
  public JavaNetURLAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    String[] interfacesNew = new String[interfaces.length + 1];
    System.arraycopy(interfaces, 0, interfacesNew, 0, interfaces.length);
    interfacesNew[interfacesNew.length - 1] = TCURL.class.getName().replace('.', '/');
    super.visit(version, access, name, signature, superName, interfacesNew);
  }

  public void visitEnd() {
    createTCSetLogicalMethod();
    super.visitEnd();
  }

  /*
   * Creates a method like this: 
   * 
   *  public void __tc_set_logical(String protocol, String host, int port,
   *                               String authority, String userInfo, String path,
   *                               String query, String ref) {
   *    synchronized (this) {
   *      if (null == handler) {
   *        handler = getURLStreamHandler(protocol);
   *      }
   *      this.__tc_set(protocol, host, port, authority, userInfo, path, query, ref);
   *    }
   *  }
   */
  public void createTCSetLogicalMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, TCSET_LOGICAL_METHOD_NAME,
                                      "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                                      null, null);
    mv.visitCode();
    
    // synchronization
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, null);
    Label l3 = new Label();
    mv.visitTryCatchBlock(l2, l3, l2, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitVarInsn(ASTORE, 9);
    mv.visitInsn(MONITORENTER);
    mv.visitLabel(l0);
    
    // lookup a handler from the protocol if it's null
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, Type.getInternalName(URL.class), "handler", "Ljava/net/URLStreamHandler;");
    Label labelHandlerNotNull = new Label();
    mv.visitJumpInsn(IFNONNULL, labelHandlerNotNull);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(URL.class), "getURLStreamHandler", "(Ljava/lang/String;)Ljava/net/URLStreamHandler;");
    mv.visitFieldInsn(PUTFIELD, Type.getInternalName(URL.class), "handler", "Ljava/net/URLStreamHandler;");
    mv.visitLabel(labelHandlerNotNull);
    
    // use the __tc_set method that is now wrapped by the set method for logical actions
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitVarInsn(ALOAD, 7);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(URL.class), TCSET_METHOD_NAME, "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    // synchronization
    mv.visitVarInsn(ALOAD, 9);
    mv.visitInsn(MONITOREXIT);
    mv.visitLabel(l1);
    
    Label labelReturnSuccessfully = new Label();
    mv.visitJumpInsn(GOTO, labelReturnSuccessfully);
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitInsn(MONITOREXIT);
    mv.visitLabel(l3);
    mv.visitInsn(ATHROW);
    mv.visitLabel(labelReturnSuccessfully);
    mv.visitInsn(RETURN);
    mv.visitMaxs(9, 10);
    mv.visitEnd();
  }
}