/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class ServletContextFacadeAdapater extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public ServletContextFacadeAdapater(ClassVisitor cv) {
    super(cv);
  }

  public ServletContextFacadeAdapater() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new ServletContextFacadeAdapater(visitor);
  }

  public void visitEnd() {
    // add a method to let us get the session context
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.TC_METHOD_PREFIX + "getSessionContext",
                                         "()Ljava/lang/Object;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/facade/ServletContextFacade", "context",
                      "Lcom/ibm/wsspi/webcontainer/servlet/IServletContext;");
    mv.visitTypeInsn(CHECKCAST, "com/ibm/ws/webcontainer/webapp/WebApp");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebApp", "getSessionContext",
                       "()Lcom/ibm/ws/webcontainer/session/IHttpSessionContext;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    super.visitEnd();
  }

}
