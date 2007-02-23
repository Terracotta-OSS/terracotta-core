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
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WebAppServletContextAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public WebAppServletContextAdapter() {
    super(null);
  }

  private WebAppServletContextAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WebAppServletContextAdapter(visitor, loader);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { "com/terracotta/session/WebAppConfig" });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitEnd() {
    addSesssionAttributeListeners();
    addSessionListener();
    addSessionCookieComment();
    addSessionCookieDomain();
    addSessionCookieMaxAge();
    addSessionCookieName();
    addSessionCookiePath();
    addSessionCookieSecure();
    addSessionCookieEnabled();
    addSessionIdLength();
    addSessionTimeoutSeconds();
    addServerId();
    addSessionTrackingEnabled();
    addSessionUrlRewritingEnabled();
    super.visitEnd();
  }

  private void addSessionListener() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionListener",
                                      "()[Ljavax/servlet/http/HttpSessionListener;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessListeners", "Ljava/util/List;");
    Label notNull = new Label();
    mv.visitJumpInsn(IFNONNULL, notNull);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
    mv.visitInsn(ARETURN);
    mv.visitLabel(notNull);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessListeners", "Ljava/util/List;");
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "[Ljavax/servlet/http/HttpSessionListener;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSesssionAttributeListeners() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionAttributeListeners",
                                      "()[Ljavax/servlet/http/HttpSessionAttributeListener;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessAttrListeners",
                      "Ljava/util/List;");
    Label notNull = new Label();
    mv.visitJumpInsn(IFNONNULL, notNull);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
    mv.visitInsn(ARETURN);
    mv.visitLabel(notNull);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessAttrListeners",
                      "Ljava/util/List;");
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "[Ljavax/servlet/http/HttpSessionAttributeListener;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionCookieComment() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieComment", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieComment",
                      "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionCookieDomain() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieDomain", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieDomain",
                      "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionCookieMaxAge() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieMaxAgeSecs", "()I", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieMaxAgeSecs", "I");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionCookieName() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieName",
                      "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionCookiePath() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookiePath", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookiePath",
                      "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionCookieSecure() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieSecure", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieSecure", "Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionCookieEnabled() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookiesEnabled", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookiesEnabled", "Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionIdLength() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getIdLength", "()I", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionIDLength", "I");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addServerId() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getServerId", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "httpServer",
                      "Lweblogic/servlet/internal/HttpServer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/HttpServer", "getServerHash", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionTimeoutSeconds() {

    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getSessionTimeoutSecs", "()I", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionTimeoutSecs", "I");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionTrackingEnabled() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getTrackingEnabled", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionTrackingEnabled", "Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSessionUrlRewritingEnabled() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getURLRewritingEnabled", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionURLRewritingEnabled", "Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
}
