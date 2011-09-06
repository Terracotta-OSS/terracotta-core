/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.proxy;

import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.AsmNullAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Compile a proxy class for the delegation strategy.
 * <p/>
 * All interfaces methods are taken in the given interface order and implemented using delegation.
 * A single constructor is compiled wich accept each interface as argument
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class ProxyDelegationCompiler implements TransformationConstants {

  /**
   * Compile the proxy
   *
   * @param loader
   * @param interfaces
   * @param proxyClassName
   * @return
   */
  public static byte[] compileProxyFor(
          final ClassLoader loader, final Class[] interfaces, final String proxyClassName) {
    final ClassWriter proxyWriter = AsmHelper.newClassWriter(true);

    final Set methodSignatures = new HashSet();
    final String[] interfaceClassNames = new String[interfaces.length];
    final String[] interfaceSignatures = new String[interfaces.length];
    for (int i = 0; i < interfaces.length; i++) {
      interfaceClassNames[i] = interfaces[i].getName().replace('.', '/');
      interfaceSignatures[i] = 'L' + interfaceClassNames[i] + ';';
    }

    //FIXME copy interfaces class level annotations, and make sure we ignore doublons if any
    ProxyCompilerClassVisitor createProxy = new ProxyDelegationCompiler.ProxyCompilerClassVisitor(
            proxyWriter,
            proxyClassName.replace('.', '/'),
            methodSignatures,
            interfaceClassNames,
            interfaceSignatures
    );

    // visit each interface
    for (int i = 0; i < interfaces.length; i++) {
      Class anInterface = interfaces[i];
      final String interfaceClassName = anInterface.getName().replace('.', '/');

      InputStream in = null;
      final ClassReader classReader;
      try {
        if (loader != null) {
          in = loader.getResourceAsStream(interfaceClassName + ".class");
        } else {
          in = ClassLoader.getSystemClassLoader().getResourceAsStream(interfaceClassName + ".class");
        }
        classReader = new ClassReader(in);
      } catch (IOException e) {
        throw new WrappedRuntimeException("Cannot compile proxy for " + anInterface, e);
      } finally {
        try {
          in.close();
        } catch (Throwable t) {
          // ignore
        }
      }
      classReader.accept(createProxy, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);  // no need for debug info
    }
    return proxyWriter.toByteArray();
  }

  /**
   * Proxy compiler. Ones can call accept as many times as needed.
   * visitEnd allow to track the index of the visited interface
   */
  public static class ProxyCompilerClassVisitor extends AsmNullAdapter.NullClassAdapter implements Opcodes, TransformationConstants {
    final ClassVisitor m_proxyCv;
    final String m_proxyClassName;
    final Set m_signatures;
    private int currentInterfaceIndex = 0;
    final String[] m_interfaceClassNames;
    final String[] m_interfaceSignatures;

    /**
     * Create the class, a field per interface, and the single constructor
     *
     * @param proxyCv
     * @param proxyClassName
     * @param signatures
     * @param interfaceClassNames
     */
    public ProxyCompilerClassVisitor(final ClassVisitor proxyCv, final String proxyClassName, final Set signatures, final String[] interfaceClassNames, final String[] interfaceSignatures) {
      m_proxyCv = proxyCv;
      m_proxyClassName = proxyClassName;
      m_signatures = signatures;
      m_interfaceClassNames = interfaceClassNames;
      m_interfaceSignatures = interfaceSignatures;

      m_proxyCv.visit(
              AsmHelper.JAVA_VERSION,
              ACC_PUBLIC + ACC_SYNTHETIC + ACC_SUPER,
              m_proxyClassName,
              null,
              OBJECT_CLASS_NAME,
              interfaceClassNames
      );

      // create one field per implemented interface
      for (int i = 0; i < interfaceClassNames.length; i++) {
        m_interfaceSignatures[i] = 'L' + interfaceClassNames[i] + ';';
        m_proxyCv.visitField(
                ACC_PRIVATE + ACC_SYNTHETIC + ACC_FINAL,
                "DELEGATE_" + i,
                m_interfaceSignatures[i],
                null,
                null
        );
      }

      // create ctor
      StringBuffer ctorDesc = new StringBuffer("(");
      for (int i = 0; i < interfaceClassNames.length; i++) {
        ctorDesc.append(m_interfaceSignatures[i]);
      }
      ctorDesc.append(")V");
      MethodVisitor init = m_proxyCv.visitMethod(
              ACC_PUBLIC + ACC_SYNTHETIC,
              INIT_METHOD_NAME,
              ctorDesc.toString(),
              null,
              null
      );
      init.visitVarInsn(ALOAD, 0);
      init.visitMethodInsn(INVOKESPECIAL, OBJECT_CLASS_NAME, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
      for (int i = 0; i < interfaceClassNames.length; i++) {
        init.visitVarInsn(ALOAD, 0);
        init.visitVarInsn(ALOAD, 1 + i);
        init.visitFieldInsn(PUTFIELD, m_proxyClassName, "DELEGATE_" + i, m_interfaceSignatures[i]);
      }
      init.visitInsn(RETURN);
      init.visitMaxs(0, 0);
    }

    /**
     * Implement the interface method by delegating to the corresponding field
     *
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @return
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (m_signatures.contains(name + desc)) {
        return super.visitMethod(access, name, desc, signature, exceptions);
      }
      m_signatures.add(name + desc);

      MethodVisitor cv = m_proxyCv.visitMethod(
              access & ~ACC_ABSTRACT,
              name,
              desc,
              signature,
              exceptions
      );

      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(
              GETFIELD,
              m_proxyClassName,
              "DELEGATE_" + currentInterfaceIndex,
              m_interfaceSignatures[currentInterfaceIndex]
      );
      AsmHelper.loadArgumentTypes(cv, Type.getArgumentTypes(desc), false);
      cv.visitMethodInsn(
              INVOKEINTERFACE,
              m_interfaceClassNames[currentInterfaceIndex],
              name,
              desc
      );
      AsmHelper.addReturnStatement(cv, Type.getReturnType(desc));
      cv.visitMaxs(0, 0);

      // as we return cv we will copy the interface[currentInterfaceIndex] current method annotations
      // which is what we want
      return cv;
    }

    /**
     * Update the interface index for the next accept()
     */
    public void visitEnd() {
      currentInterfaceIndex++;
      super.visitEnd();
    }
  }

}
