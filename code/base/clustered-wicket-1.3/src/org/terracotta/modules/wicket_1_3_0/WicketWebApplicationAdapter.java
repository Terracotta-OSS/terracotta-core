/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.wicket_1_3_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;


/**
 * Custom adapter used to replace <code>ISessionStore</code> implementation in <code>WebApplication</code>.
 * 
 * @see wicket.protocol.http.WebApplication.newSessionStore()
 * 
 * @author Eugene Kuleshov
 */
public class WicketWebApplicationAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public WicketWebApplicationAdapter() {
    super(null);
  }

  public WicketWebApplicationAdapter(ClassVisitor visitor, ClassLoader loader) {
    super(visitor);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WicketWebApplicationAdapter(visitor, loader);
  }
  
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if(!"newSessionStore".equals(name) || !"()Lorg/apache/wicket/session/ISessionStore;".equals(desc)) {
      return mv;
    }

    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(591, l0);
    mv.visitTypeInsn(NEW, "org/apache/wicket/protocol/http/HttpSessionStore");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "org/apache/wicket/protocol/http/HttpSessionStore", "<init>", "(Lorg/apache/wicket/Application;)V");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lorg/apache/wicket/protocol/http/WebApplication;", null, l0, l1, 0);
    mv.visitMaxs(3, 1);
    mv.visitEnd();
    
    return null;
  }

}
