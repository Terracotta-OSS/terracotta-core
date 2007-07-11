/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class SessionContextAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public SessionContextAdapter(ClassVisitor cv) {
    super(cv);
  }

  public SessionContextAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new SessionContextAdapter(visitor);
  }

  public void visitEnd() {
    addTCSessionMethods();
    super.visitEnd();
  }

  private void addTCSessionMethods() {
    MethodVisitor mv;

    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieComment", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(14, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext",
                         "getSessionCookieComment", "()Ljava/lang/String;");
      mv.visitInsn(ARETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieDomain", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(18, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieDomain",
                         "()Ljava/lang/String;");
      mv.visitInsn(ARETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieMaxAgeSecs", "()I", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(22, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieMaxAge",
                         "()I");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieName", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(26, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieName",
                         "()Ljava/lang/String;");
      mv.visitInsn(ARETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookiePath", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(30, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookiePath",
                         "()Ljava/lang/String;");
      mv.visitInsn(ARETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieSecure", "()Z", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(34, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieSecure",
                         "()Z");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookiesEnabled", "()Z", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(38, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "isUsingCookies", "()Z");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionAttributeListeners",
                             "()[Ljavax/servlet/http/HttpSessionAttributeListener;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(47, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext",
                         "getHttpSessionAttributeListeners", "()Ljava/util/ArrayList;");
      mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "unmodifiableList",
                         "(Ljava/util/List;)Ljava/util/List;");
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(46, l1);
      mv.visitVarInsn(ASTORE, 1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLineNumber(48, l2);
      mv.visitVarInsn(ALOAD, 1);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLineNumber(49, l3);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitLineNumber(48, l4);
      mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
      mv.visitVarInsn(ASTORE, 2);
      Label l5 = new Label();
      mv.visitLabel(l5);
      mv.visitLineNumber(51, l5);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 3);
      Label l6 = new Label();
      mv.visitLabel(l6);
      mv.visitLineNumber(52, l6);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;");
      mv.visitVarInsn(ASTORE, 4);
      Label l7 = new Label();
      mv.visitLabel(l7);
      Label l8 = new Label();
      mv.visitJumpInsn(GOTO, l8);
      Label l9 = new Label();
      mv.visitLabel(l9);
      mv.visitLineNumber(53, l9);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
      mv.visitTypeInsn(CHECKCAST, "java/util/List");
      mv.visitVarInsn(ASTORE, 5);
      Label l10 = new Label();
      mv.visitLabel(l10);
      mv.visitLineNumber(54, l10);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ILOAD, 3);
      mv.visitIincInsn(3, 1);
      mv.visitVarInsn(ALOAD, 5);
      mv.visitInsn(ICONST_1);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");
      mv.visitTypeInsn(CHECKCAST, "javax/servlet/http/HttpSessionAttributeListener");
      mv.visitInsn(AASTORE);
      mv.visitLabel(l8);
      mv.visitLineNumber(52, l8);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
      mv.visitJumpInsn(IFNE, l9);
      Label l11 = new Label();
      mv.visitLabel(l11);
      mv.visitLineNumber(56, l11);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(ARETURN);
      Label l12 = new Label();
      mv.visitLabel(l12);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l12, 0);
      mv.visitLocalVariable("listeners", "Ljava/util/List;", null, l2, l12, 1);
      mv.visitLocalVariable("rv", "[Ljavax/servlet/http/HttpSessionAttributeListener;", null, l5, l12, 2);
      mv.visitLocalVariable("index", "I", null, l6, l12, 3);
      mv.visitLocalVariable("i", "Ljava/util/Iterator;", null, l7, l11, 4);
      mv.visitLocalVariable("l", "Ljava/util/List;", null, l10, l8, 5);
      mv.visitMaxs(4, 6);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionListener",
                             "()[Ljavax/servlet/http/HttpSessionListener;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(65, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext",
                         "getHttpSessionListeners", "()Ljava/util/ArrayList;");
      mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "unmodifiableList",
                         "(Ljava/util/List;)Ljava/util/List;");
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(64, l1);
      mv.visitVarInsn(ASTORE, 1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLineNumber(66, l2);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
      mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
      mv.visitVarInsn(ASTORE, 2);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLineNumber(68, l3);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 3);
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitLineNumber(69, l4);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;");
      mv.visitVarInsn(ASTORE, 4);
      Label l5 = new Label();
      mv.visitLabel(l5);
      Label l6 = new Label();
      mv.visitJumpInsn(GOTO, l6);
      Label l7 = new Label();
      mv.visitLabel(l7);
      mv.visitLineNumber(70, l7);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
      mv.visitTypeInsn(CHECKCAST, "java/util/List");
      mv.visitVarInsn(ASTORE, 5);
      Label l8 = new Label();
      mv.visitLabel(l8);
      mv.visitLineNumber(71, l8);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ILOAD, 3);
      mv.visitIincInsn(3, 1);
      mv.visitVarInsn(ALOAD, 5);
      mv.visitInsn(ICONST_1);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");
      mv.visitTypeInsn(CHECKCAST, "javax/servlet/http/HttpSessionListener");
      mv.visitInsn(AASTORE);
      mv.visitLabel(l6);
      mv.visitLineNumber(69, l6);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
      mv.visitJumpInsn(IFNE, l7);
      Label l9 = new Label();
      mv.visitLabel(l9);
      mv.visitLineNumber(73, l9);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(ARETURN);
      Label l10 = new Label();
      mv.visitLabel(l10);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l10, 0);
      mv.visitLocalVariable("listeners", "Ljava/util/List;", null, l2, l10, 1);
      mv.visitLocalVariable("rv", "[Ljavax/servlet/http/HttpSessionListener;", null, l3, l10, 2);
      mv.visitLocalVariable("index", "I", null, l4, l10, 3);
      mv.visitLocalVariable("i", "Ljava/util/Iterator;", null, l5, l9, 4);
      mv.visitLocalVariable("l", "Ljava/util/List;", null, l8, l6, 5);
      mv.visitMaxs(4, 6);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getIdLength", "()I", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(77, l0);
      mv.visitFieldInsn(GETSTATIC, "com/ibm/ws/webcontainer/httpsession/SessionContext", "sessionIDLength", "I");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getServerId", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(81, l0);
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getSessionDelimiter", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(85, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getCloneSeparator",
                         "()Ljava/lang/String;");
      mv.visitInsn(ARETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getSessionTimeoutSecs", "()I", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(89, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionTimeOut",
                         "()I");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getTrackingEnabled", "()Z", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(93, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext",
                         "isSessionTrackingActive", "()Z");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getURLRewritingEnabled", "()Z", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(97, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "isUsingURL", "()Z");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

  }

  /**
   * The code template for this instrumentation
   *
   * <pre>
   * package com.ibm.ws.webcontainer.httpsession;
   *
   * import java.util.Collections;
   * import java.util.Iterator;
   * import java.util.List;
   *
   * import javax.servlet.http.HttpSessionAttributeListener;
   * import javax.servlet.http.HttpSessionListener;
   *
   * public final class InstrumentedSessionContext extends SessionContext implements com.terracotta.session.WebAppConfig {
   *
   *   public String __tc_session_getCookieComment() {
   *     return getSessionCookieComment();
   *   }
   *
   *   public String __tc_session_getCookieDomain() {
   *     return getSessionCookieDomain();
   *   }
   *
   *   public int __tc_session_getCookieMaxAgeSecs() {
   *     return getSessionCookieMaxAge();
   *   }
   *
   *   public String __tc_session_getCookieName() {
   *     return getSessionCookieName();
   *   }
   *
   *   public String __tc_session_getCookiePath() {
   *     return getSessionCookiePath();
   *   }
   *
   *   public boolean __tc_session_getCookieSecure() {
   *     return getSessionCookieSecure();
   *   }
   *
   *   public boolean __tc_session_getCookiesEnabled() {
   *     return isUsingCookies();
   *   }
   *
   *   // IBM adds listeners as a two-entry list: 0 = String, 1 = Object (the
   *   // listener)
   *
   *   public HttpSessionAttributeListener[] __tc_session_getHttpSessionAttributeListeners() {
   *     List listeners = Collections.unmodifiableList(getHttpSessionAttributeListeners());
   *     HttpSessionAttributeListener[] rv = new HttpSessionAttributeListener[listeners.size()];
   *
   *     int index = 0;
   *     for (Iterator i = listeners.iterator(); i.hasNext();) {
   *       List l = (List) i.next();
   *       rv[index++] = (HttpSessionAttributeListener) l.get(1);
   *     }
   *     return rv;
   *   }
   *
   *   // IBM adds listeners as a two-entry list: 0 = String, 1 = Object (the listener)
   *   public HttpSessionListener[] __tc_session_getHttpSessionListener() {
   *     List listeners = Collections.unmodifiableList(getHttpSessionListeners());
   *     HttpSessionListener[] rv = new HttpSessionListener[listeners.size()];
   *
   *     int index = 0;
   *     for (Iterator i = listeners.iterator(); i.hasNext();) {
   *       List l = (List) i.next();
   *       rv[index++] = (HttpSessionListener) l.get(1);
   *     }
   *     return rv;
   *   }
   *
   *   public int __tc_session_getIdLength() {
   *     return sessionIDLength;
   *   }
   *
   *   public String __tc_session_getServerId() {
   *     return null;
   *   }
   *
   *   public String __tc_session_getSessionDelimiter() {
   *     return getCloneSeparator();
   *   }
   *
   *   public int __tc_session_getSessionTimeoutSecs() {
   *     return getSessionTimeOut();
   *   }
   *
   *   public boolean __tc_session_getTrackingEnabled() {
   *     return isSessionTrackingActive();
   *   }
   *
   *   public boolean __tc_session_getURLRewritingEnabled() {
   *     return isUsingURL();
   *   }
   *
   *   public InstrumentedSessionContext(SessionContextParameters sessioncontextparameters,
   *                                     SessionApplicationParameters sessionapplicationparameters) {
   *     super(sessioncontextparameters, sessionapplicationparameters);
   *   }
   *
   *   IHttpSession createSessionData(String s) {
   *     return null;
   *   }
   *
   *   void performInvalidation() {
   *   }
   *
   * }
   * </pre>
   */
}
