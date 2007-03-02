/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.iBatis_2_2_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;


public class IBatisAddDefaultConstructorAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  private final static String SQLMAPCLIENT_CLASS_SLASHES = "com/ibatis/sqlmap/engine/impl/SqlMapClientImpl";
  private final static String SIMPLEDATASOURCE_CLASS_SLASHES = "com/ibatis/common/jdbc/SimpleDataSource";
  private String classNameSlashes;

  public IBatisAddDefaultConstructorAdapter() {
    super(null);
  }
  
  public IBatisAddDefaultConstructorAdapter(ClassVisitor cv) {
    super(cv);
  }
  
  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new IBatisAddDefaultConstructorAdapter(visitor);
  }
  
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.classNameSlashes = name;
  }
  
  public void visitEnd() {
    if (SQLMAPCLIENT_CLASS_SLASHES.equals(classNameSlashes)) {
      addSqlMapClientDefaultConstructor();
    } else if (SIMPLEDATASOURCE_CLASS_SLASHES.equals(classNameSlashes)) {
      addSimpleDataSourceConstructor();
    }
    super.visitEnd();
  }
  
  private void addSimpleDataSourceConstructor() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(115, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(78, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitTypeInsn(NEW, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "POOL_LOCK", "Ljava/lang/Object;");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(79, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitTypeInsn(NEW, "java/util/ArrayList");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "idleConnections", "Ljava/util/List;");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(80, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitTypeInsn(NEW, "java/util/ArrayList");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "activeConnections", "Ljava/util/List;");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(81, l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "requestCount", "J");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(82, l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "accumulatedRequestTime", "J");
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(83, l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "accumulatedCheckoutTime", "J");
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(84, l7);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "claimedOverdueConnectionCount", "J");
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(85, l8);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "accumulatedCheckoutTimeOfOverdueConnections", "J");
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLineNumber(86, l9);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "accumulatedWaitTime", "J");
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(87, l10);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "hadToWaitCount", "J");
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitLineNumber(88, l11);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(LCONST_0);
    mv.visitFieldInsn(PUTFIELD, SIMPLEDATASOURCE_CLASS_SLASHES, "badConnectionCount", "J");
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(116, l12);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLineNumber(117, l13);
    mv.visitInsn(RETURN);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
  
  private void addSqlMapClientDefaultConstructor() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(55, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(48, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitTypeInsn(NEW, "java/lang/ThreadLocal");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ThreadLocal", "<init>", "()V");
    mv.visitFieldInsn(PUTFIELD, SQLMAPCLIENT_CLASS_SLASHES, "localSqlMapSession", "Ljava/lang/ThreadLocal;");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(56, l2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(57, l3);
    mv.visitInsn(RETURN);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
}
