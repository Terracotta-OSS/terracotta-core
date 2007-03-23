/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.wicket;

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

    if(!"newSessionStore".equals(name) || !"()Lwicket/session/ISessionStore;".equals(desc)) {
      return mv;
    }

    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(603, l0);
    mv.visitTypeInsn(NEW, "wicket/protocol/http/HttpSessionStore");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "wicket/protocol/http/HttpSessionStore", "<init>", "()V");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lwicket/protocol/http/WebApplication;", null, l0, l1, 0);
    mv.visitMaxs(2, 1);
    mv.visitEnd();
    
    return null;
  }

}
