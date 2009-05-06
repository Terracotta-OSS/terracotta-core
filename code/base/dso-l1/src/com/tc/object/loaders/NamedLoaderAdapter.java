/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

/**
 * Adds the NamedClassLoader interface (and required impl) to a loader implementation
 */
public class NamedLoaderAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  private static final String CLASSLOADER_NAME_NOT_SET_PREFIX = 
    "This classloader instance has not been registered (loader class:";
  private static final String CLASSLOADER_NAME_NOT_SET_SUFFIX =
    ").\n" +
    "\n" + 
    "The correct Terracotta Integration Module (TIM) may be missing from this\n" + 
    "installation of Terracotta, or an unsupported platform is being used.\n" + 
    "See the current list of supported platforms at\n" + 
    "http://www.terracotta.org/web/display/docs/Platform+Support.\n" + 
    "\n" + 
    "TIMs are required to integrate Terracotta with\n" + 
    "web containers, frameworks, and other technologies.\n" + 
    "\n" + 
    "For example, to integrate Apache Tomcat 5.5 with Terracotta on UNIX/Linux,\n" + 
    "install the correct TIM by entering the following command from the Terracotta\n" + 
    "installation root directory:\n" + 
    "\n" + 
    "[PROMPT] bin/tim-get.sh install tim-tomcat-5.5\n" + 
    "\n" + 
    "On Microsoft Windows, enter:\n" + 
    "\n" + 
    "[PROMPT] bin/tim-get.bat install tim-tomcat-5.5\n" + 
    "\n" + 
    "You must also add the TIM to the Terracotta configuration file (tc-config.xml\n" + 
    "by default) by adding its name and version number using a <module> element:\n" + 
    "\n" + 
    "<modules>\n" + 
    "  <module name=\"tim-tomcat-5.5\" version=\"1.0.0-SNAPSHOT\" />\n" + 
    "  <module name=\"tim-another-one\" version=\"1.2.3\" />\n" + 
    "  ...\n" + 
    "</modules>\n" + 
    "\n" + 
    "For more information, see http://www.terracotta.org/tim-error.";
  private static final String LOADER_NAME_FIELD = ByteCodeUtil.TC_FIELD_PREFIX + "loaderName";
  private String              owner;

  public NamedLoaderAdapter() {
    super(null);
  }

  private NamedLoaderAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new NamedLoaderAdapter(visitor, loader);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { ByteCodeUtil.NAMEDCLASSLOADER_CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
    this.owner = name;
  }

  public void visitEnd() {
    super.visitField(ACC_SYNTHETIC | ACC_VOLATILE | ACC_TRANSIENT | ACC_PRIVATE, LOADER_NAME_FIELD,
                     "Ljava/lang/String;", null, null);

    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "__tc_setClassLoaderName",
                                         "(Ljava/lang/String;)V", null, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "__tc_getClassLoaderName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    Label l1 = new Label();
    mv.visitJumpInsn(IFNONNULL, l1);
    mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn(CLASSLOADER_NAME_NOT_SET_PREFIX);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn(CLASSLOADER_NAME_NOT_SET_SUFFIX);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    super.visitEnd();
  }

}
