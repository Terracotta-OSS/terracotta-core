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
    addIsWeblogic8();
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
    addSessionDelimiter();
    super.visitEnd();
  }
  
  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  // The method being added is used by most of the ohers being added to switch between wl8 
  // and wl9 versions.
  public void addIsWeblogic8() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_isWeblogic8",
                                      "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
    mv.visitLabel(l0);
    mv.visitLineNumber(42, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitLdcInsn("sessionCookieName");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
    mv.visitInsn(POP);
    mv.visitLabel(l1);
    mv.visitLineNumber(43, l1);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l2);
    mv.visitLineNumber(44, l2);
    mv.visitVarInsn(ASTORE, 1);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(46, l3);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l4, 0);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSesssionAttributeListeners() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionAttributeListeners",
                                      "()[Ljavax/servlet/http/HttpSessionAttributeListener;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(76, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(77, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessAttrListeners", "Ljava/util/List;");
    Label l3 = new Label();
    mv.visitJumpInsn(IFNONNULL, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(78, l4);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l3);
    mv.visitLineNumber(80, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessAttrListeners", "Ljava/util/List;");
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "[Ljavax/servlet/http/HttpSessionAttributeListener;");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(83, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "getEventsManager", "()Lweblogic/servlet/internal/EventsManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/EventsManager", "__tc_session_getHttpSessionAttributeListeners", "()[Ljavax/servlet/http/HttpSessionAttributeListener;");
    mv.visitInsn(ARETURN);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l5, 0);
    mv.visitMaxs(2, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionListener() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionListener",
                                      "()[Ljavax/servlet/http/HttpSessionListener;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(89, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(90, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessListeners", "Ljava/util/List;");
    Label l3 = new Label();
    mv.visitJumpInsn(IFNONNULL, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(91, l4);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l3);
    mv.visitLineNumber(93, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessListeners", "Ljava/util/List;");
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "[Ljavax/servlet/http/HttpSessionListener;");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(96, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "getEventsManager", "()Lweblogic/servlet/internal/EventsManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/EventsManager", "__tc_session_getHttpSessionListener", "()[Ljavax/servlet/http/HttpSessionListener;");
    mv.visitInsn(ARETURN);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l5, 0);
    mv.visitMaxs(2, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionCookieComment() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieComment", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(102, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(103, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieComment", "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(105, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "getCookieComment", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();  
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionCookieDomain() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieDomain", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(111, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(112, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieDomain", "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(114, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "getCookieDomain", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionCookieMaxAge() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieMaxAgeSecs", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(120, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(121, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieMaxAgeSecs", "I");
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(123, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "getCookieMaxAgeSecs", "()I");
    mv.visitInsn(IRETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionCookieName() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(129, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(130, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieName", "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(132, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "getCookieName", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionCookiePath() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookiePath", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(138, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(139, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookiePath", "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(141, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "getCookiePath", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionCookieSecure() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookieSecure", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(147, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(148, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookieSecure", "Z");
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(150, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "isCookieSecure", "()Z");
    mv.visitInsn(IRETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionCookieEnabled() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getCookiesEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(156, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(157, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionCookiesEnabled", "Z");
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(159, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "isSessionCookiesEnabled", "()Z");
    mv.visitInsn(IRETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionIdLength() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getIdLength", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(165, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(166, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionIDLength", "I");
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(168, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "getIDLength", "()I");
    mv.visitInsn(IRETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addServerId() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getServerId", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(184, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "httpServer", "Lweblogic/servlet/internal/HttpServer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/HttpServer", "getServerHash", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionDelimiter() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getSessionDelimiter", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(208, l0);
    mv.visitLdcInsn("!");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionTimeoutSeconds() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getSessionTimeoutSecs", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(174, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(175, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionTimeoutSecs", "I");
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(177, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "getSessionTimeoutSecs", "()I");
    mv.visitInsn(IRETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionTrackingEnabled() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getTrackingEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(189, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(190, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionTrackingEnabled", "Z");
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(192, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "isSessionTrackingEnabled", "()Z");
    mv.visitInsn(IRETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // see the dummy WebAppServletContext class in dso-weblogic-stubs for this method post-asm.
  private void addSessionUrlRewritingEnabled() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_session_getURLRewritingEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(198, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/WebAppServletContext", "__tc_isWeblogic8", "()Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(199, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionURLRewritingEnabled", "Z");
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitLineNumber(201, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/WebAppServletContext", "sessionContext", "Lweblogic/servlet/internal/session/SessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionContext", "getConfigMgr", "()Lweblogic/servlet/internal/session/SessionConfigManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/servlet/internal/session/SessionConfigManager", "isUrlRewritingEnabled", "()Z");
    mv.visitInsn(IRETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/WebAppServletContext;", null, l0, l3, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }
}
