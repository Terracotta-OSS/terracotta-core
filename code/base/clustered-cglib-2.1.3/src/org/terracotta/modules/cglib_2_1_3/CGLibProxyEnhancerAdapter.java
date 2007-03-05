/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.cglib_2_1_3;

import org.osgi.framework.Bundle;
import org.terracotta.modules.cglib_2_1_3.util.FilterTCMethods;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.MethodNode;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.plugins.ModulesLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class CGLibProxyEnhancerAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {
  private final ClassLoader caller;
  private final Bundle bundle;

  public CGLibProxyEnhancerAdapter(Bundle bundle) {
    super(null);
    this.caller = null;
    this.bundle = bundle;
  }

  private CGLibProxyEnhancerAdapter(ClassVisitor cv, ClassLoader caller, Bundle bundle) {
    super(cv);
    this.caller = caller;
    this.bundle = bundle;
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new CGLibProxyEnhancerAdapter(visitor, loader, bundle);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    // public final Object intercept(Object object, Method method, Object args[], MethodProxy proxy)

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("getMethods".equals(name)
        && "(Ljava/lang/Class;[Ljava/lang/Class;Ljava/util/List;Ljava/util/List;Ljava/util/Set;)V".equals(desc)) { return new InterceptAdapter(
                                                                                                                                               mv); }

    return mv;

  }

  public void visitEnd() {
    addFilterMethod();
    super.visitEnd();
  }

  private void addFilterMethod() {
    try {
      byte[] bytes = getBytesForClass(bundle, FilterTCMethods.class);
      ClassReader tcCR = new ClassReader(bytes);
      ClassNode tcCN = new ClassNode();
      tcCR.accept(tcCN, true);

      List tcMethods = tcCN.methods;
      for (Iterator i = tcMethods.iterator(); i.hasNext();) {
        MethodNode mNode = (MethodNode) i.next();
        if (!"<init>".equals(mNode.name)) {
          mNode.accept(cv);
        }
      }
    } catch (ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    }
  }

  public byte[] getBytesForClass(final Bundle bundle, Class clazz) throws ClassNotFoundException {
    String className = clazz.getName().replace('.', '/') + ".class";
    InputStream is = null;
    ByteArrayOutputStream baos = null;
    try {
      is = ModulesLoader.getJarResource(new URL(bundle.getLocation()), className);
      if (is == null) { throw new ClassNotFoundException("No resource found for class: " + className); }
      final int size = 4096;
      byte[] buffer = new byte[size];
      baos = new ByteArrayOutputStream(size);

      int read;

      while ((read = is.read(buffer, 0, size)) > 0) {
        baos.write(buffer, 0, read);
      }
    } catch (IOException ioe) {
      throw new ClassNotFoundException("Error reading bytes for " + className, ioe);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }

    return baos.toByteArray();
  }

  private static class InterceptAdapter extends MethodAdapter implements Opcodes {

    public InterceptAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, "net/sf/cglib/proxy/Enhancer", "filterTCMethods", "(Ljava/util/Collection;)V");
      }
      super.visitInsn(opcode);
    }
  }

}
