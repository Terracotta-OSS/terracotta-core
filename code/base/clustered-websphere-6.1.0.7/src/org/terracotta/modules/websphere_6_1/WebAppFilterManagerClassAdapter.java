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
   *   String applicationName = webAppConfig.getContextRoot().replaceAll(&quot;&circ;/+&quot;, &quot;&quot;);
   *   if (ClassProcessorHelper.isDSOSessions(applicationName)) {
   *     FilterConfig dsoFilterConfig = new FilterConfig(&quot;TerracottaSessionFilterConfig&quot;);
   *     dsoFilterConfig.setName(&quot;TerracottaSessionFilter&quot;);
   *     dsoFilterConfig.addInitParameter(&quot;app-server&quot;, &quot;IBM-Websphere&quot;);
   *
   *     ClassLoader webAppLoader = this.webApp.getClassLoader();
   *     SessionsHelper.injectClasses(webAppLoader);
   *
   *     dsoFilterConfig.setFilterClassName(&quot;com.terracotta.session.SessionFilter&quot;);
   *     dsoFilterConfig.setFilterClassLoader(webAppLoader);
   *     webAppConfig.addFilterInfo(dsoFilterConfig);
   *     FilterMapping dsoFilterMapping = new FilterMapping(&quot;/*&quot;, dsoFilterConfig, null);
   *     addFilterMapping(dsoFilterMapping);
   *   }
   * }
   * </pre>
   */
  private void createLoadDSOFilterMethod() {

    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "__tc_loadDSOFilterInfo", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(17, l0);
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
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(18, l1);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "isDSOSessions",
                       "(Ljava/lang/String;)Z");
    Label l2 = new Label();
    mv.visitJumpInsn(IFEQ, l2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(20, l3);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterConfig");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("TerracottaSessionFilterConfig");
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ASTORE, 2);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(21, l4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitLdcInsn("TerracottaSessionFilter");
    mv
        .visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setName",
                         "(Ljava/lang/String;)V");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(22, l5);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitLdcInsn("app-server");
    mv.visitLdcInsn("IBM-Websphere");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "addInitParameter",
                       "(Ljava/lang/String;Ljava/lang/String;)V");
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(24, l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webApp",
                      "Lcom/ibm/ws/webcontainer/webapp/WebApp;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebApp", "getClassLoader",
                       "()Ljava/lang/ClassLoader;");
    mv.visitVarInsn(ASTORE, 3);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(25, l7);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses",
                       "(Ljava/lang/ClassLoader;)V");
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(27, l8);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitLdcInsn("com.terracotta.session.SessionFilter");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setFilterClassName",
                       "(Ljava/lang/String;)V");
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLineNumber(28, l9);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setFilterClassLoader",
                       "(Ljava/lang/ClassLoader;)V");
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(29, l10);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig",
                      "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "addFilterInfo",
                       "(Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;)V");
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitLineNumber(30, l11);
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
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(31, l12);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "addFilterMapping",
                       "(Lcom/ibm/ws/webcontainer/filter/FilterMapping;)V");
    mv.visitLabel(l2);
    mv.visitLineNumber(33, l2);
    mv.visitInsn(RETURN);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/filter/WebAppFilterManager;", null, l0, l13, 0);
    mv.visitLocalVariable("applicationName", "Ljava/lang/String;", null, l1, l13, 1);
    mv.visitLocalVariable("dsoFilterConfig", "Lcom/ibm/ws/webcontainer/filter/FilterConfig;", null, l4, l2, 2);
    mv.visitLocalVariable("webAppLoader", "Ljava/lang/ClassLoader;", null, l7, l2, 3);
    mv.visitLocalVariable("dsoFilterMapping", "Lcom/ibm/ws/webcontainer/filter/FilterMapping;", null, l12, l2, 4);
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
