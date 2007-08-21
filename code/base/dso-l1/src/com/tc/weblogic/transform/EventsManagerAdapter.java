/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.weblogic.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EventsManagerAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public EventsManagerAdapter() {
    super(null);
  }

  private EventsManagerAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new EventsManagerAdapter(visitor, loader);
  }

  public void visitEnd() {
    addSesssionAttributeListeners();
    addSessionListener();
    super.visitEnd();
  }

  // see the dummy EventsManager class in dso-weblogic-stubs for this method post-asm
  private void addSessionListener() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionListener",
                                      "()[Ljavax/servlet/http/HttpSessionListener;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/EventsManager", "sessListeners", "Ljava/util/List;");
    Label notNull = new Label();
    mv.visitJumpInsn(IFNONNULL, notNull);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
    mv.visitInsn(ARETURN);
    mv.visitLabel(notNull);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/EventsManager", "sessListeners", "Ljava/util/List;");
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "[Ljavax/servlet/http/HttpSessionListener;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  // see the dummy EventsManager class in dso-weblogic-stubs for this method post-asm
  private void addSesssionAttributeListeners() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionAttributeListeners",
                                      "()[Ljavax/servlet/http/HttpSessionAttributeListener;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/EventsManager", "sessAttrListeners",
                      "Ljava/util/List;");
    Label notNull = new Label();
    mv.visitJumpInsn(IFNONNULL, notNull);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
    mv.visitInsn(ARETURN);
    mv.visitLabel(notNull);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/EventsManager", "sessAttrListeners",
                      "Ljava/util/List;");
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "[Ljavax/servlet/http/HttpSessionAttributeListener;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

}
