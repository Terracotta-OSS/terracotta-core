package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WebAppFilterManagerClassAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  public WebAppFilterManagerClassAdapter() {
    super(null);
  }

  public WebAppFilterManagerClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WebAppFilterManagerClassAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (name.equals("init") && desc.equals("()V")) {
      mv = new InitMethodAdapter(mv);
    }
    return mv;
  }

  public void visitEnd() {
    createLoadDSOFilterMethod();
    super.visitEnd();
  }

  /**
   * <pre>
   * private void __tc_loadDSOFilterInfo() {
   *   // Remove leading slashes
   *   final String applicationName = webAppConfig.getContextRoot().replaceAll(&quot;&circ;/+&quot;, &quot;&quot;);
   *   if (ClassProcessorHelper.isDSOSessions(applicationName)) {
   *     FilterConfig dsoFilterConfig = new FilterConfig(&quot;TerracottaSessionFilterConfig&quot;);
   *     dsoFilterConfig.setName(&quot;TerracottaSessionFilter&quot;);
   *     dsoFilterConfig.addInitParameter(&quot;app-server&quot;, &quot;IBM-Websphere&quot;);
   *     URLClassLoader terracottaDSOSessionsLoader = new URLClassLoader(new URL[0], ThreadContextHelper
   *         .getContextClassLoader());
   *     ((NamedClassLoader) terracottaDSOSessionsLoader)
   *         .__tc_setClassLoaderName(Namespace.createLoaderName(Namespace.WEBSPHERE_NAMESPACE, &quot;terracotta-sessions-jar:&quot;
   *                                                                                            + webAppConfig
   *                                                                                                .getApplicationName()));
   *     ClassProcessorHelper.registerGlobalLoader((NamedClassLoader) terracottaDSOSessionsLoader);
   *     try {
   *       SessionsHelper.injectClasses(terracottaDSOSessionsLoader);
   *     } catch (Exception e) {
   *       throw new RuntimeException(&quot;Unable to inject Terracotta session filter classes into application &quot;
   *                                  + webAppConfig.getApplicationName(), e);
   *     }
   *     dsoFilterConfig.setFilterClassName(&quot;com.terracotta.session.SessionFilter&quot;);
   *     dsoFilterConfig.setFilterClassLoader(terracottaDSOSessionsLoader);
   *     webAppConfig.addFilterInfo(dsoFilterConfig);
   *     FilterMapping dsoFilterMapping = new FilterMapping(&quot;/*&quot;, dsoFilterConfig, null);
   *     addFilterMapping(dsoFilterMapping);
   *   }
   * }
   * </pre>
   */
  private void createLoadDSOFilterMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, ByteCodeUtil.TC_METHOD_PREFIX + "loadDSOFilterInfo", "()V", null,
                                         null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, "java/lang/Exception");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig",
                      "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot",
                       "()Ljava/lang/String;");
    mv.visitLdcInsn("^/+");
    mv.visitLdcInsn("");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replaceAll",
                       "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    mv.visitVarInsn(ASTORE, 1);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "isDSOSessions",
                       "(Ljava/lang/String;)Z");
    Label l4 = new Label();
    mv.visitJumpInsn(IFEQ, l4);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterConfig");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("TerracottaSessionFilterConfig");
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ASTORE, 2);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitLdcInsn("TerracottaSessionFilter");
    mv
        .visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setName",
                         "(Ljava/lang/String;)V");
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitLdcInsn("app-server");
    mv.visitLdcInsn("IBM-Websphere");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "addInitParameter",
                       "(Ljava/lang/String;Ljava/lang/String;)V");
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitTypeInsn(NEW, "java/net/URLClassLoader");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "java/net/URL");
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitMethodInsn(INVOKESTATIC, "com/ibm/ws/webcontainer/util/ThreadContextHelper", "getContextClassLoader",
                       "()Ljava/lang/ClassLoader;");
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitMethodInsn(INVOKESPECIAL, "java/net/URLClassLoader", "<init>", "([Ljava/net/URL;Ljava/lang/ClassLoader;)V");
    mv.visitVarInsn(ASTORE, 3);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/loaders/NamedClassLoader");
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLdcInsn("Websphere.");
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("terracotta-sessions-jar:");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig",
                      "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getApplicationName",
                       "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                       "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/loaders/NamedClassLoader", "__tc_setClassLoaderName",
                       "(Ljava/lang/String;)V");
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/loaders/NamedClassLoader");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "registerGlobalLoader",
                       "(Lcom/tc/object/loaders/NamedClassLoader;)V");
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses",
                       "(Ljava/lang/ClassLoader;)V");
    Label l15 = new Label();
    mv.visitJumpInsn(GOTO, l15);
    mv.visitLabel(l1);
    mv.visitVarInsn(ASTORE, 4);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Unable to inject Terracotta session filter classes into application ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig",
                      "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getApplicationName",
                       "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitVarInsn(ALOAD, 4);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>",
                       "(Ljava/lang/String;Ljava/lang/Throwable;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l15);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitLdcInsn("com.terracotta.session.SessionFilter");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setFilterClassName",
                       "(Ljava/lang/String;)V");
    Label l19 = new Label();
    mv.visitLabel(l19);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setFilterClassLoader",
                       "(Ljava/lang/ClassLoader;)V");
    Label l20 = new Label();
    mv.visitLabel(l20);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig",
                      "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "addFilterInfo",
                       "(Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;)V");
    Label l21 = new Label();
    mv.visitLabel(l21);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterMapping");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("/*");
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ACONST_NULL);
    mv
        .visitMethodInsn(
                         INVOKESPECIAL,
                         "com/ibm/ws/webcontainer/filter/FilterMapping",
                         "<init>",
                         "(Ljava/lang/String;Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;Lcom/ibm/wsspi/webcontainer/servlet/IServletConfig;)V");
    mv.visitVarInsn(ASTORE, 4);
    Label l22 = new Label();
    mv.visitLabel(l22);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "addFilterMapping",
                       "(Lcom/ibm/ws/webcontainer/filter/FilterMapping;)V");
    mv.visitLabel(l4);
    mv.visitInsn(RETURN);
    Label l23 = new Label();
    mv.visitLabel(l23);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/filter/WebAppFilterManager;", null, l2, l23, 0);
    mv.visitLocalVariable("applicationName", "Ljava/lang/String;", null, l3, l23, 1);
    mv.visitLocalVariable("dsoFilterConfig", "Lcom/ibm/ws/webcontainer/filter/FilterConfig;", null, l6, l4, 2);
    mv.visitLocalVariable("terracottaDSOSessionsLoader", "Ljava/net/URLClassLoader;", null, l11, l4, 3);
    mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, l16, l15, 4);
    mv.visitLocalVariable("dsoFilterMapping", "Lcom/ibm/ws/webcontainer/filter/FilterMapping;", null, l22, l4, 4);
    mv.visitMaxs(5, 5);
    mv.visitEnd();
  }

  private static class InitMethodAdapter extends MethodAdapter implements Opcodes {
    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      super.visitCode();
      super.visitVarInsn(ALOAD, 0);
      super.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager",
                            ByteCodeUtil.TC_METHOD_PREFIX + "loadDSOFilterInfo", "()V");
    }
  }

}
