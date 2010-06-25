/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import org.apache.commons.io.IOUtils;

import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.util.AdaptedClassDumper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * NOTE: This isn't a "real" ClassFileTransformer used by a java agent. We're merely hijacking the interface out of
 * convenience since it is called from code in the boot jar
 */
public class ClassLoaderTransformer implements ClassFileTransformer {

  public ClassLoaderTransformer() {
    // run one transform to pre-load some classes to avoid deadlocks
    transform(getClass().getClassLoader(), "NOT_REAL.java.lang.ClassLoader", null, getClass().getProtectionDomain(),
              getClassLoaderBytes());
  }

  private byte[] getClassLoaderBytes() {
    InputStream in = ClassLoader.getSystemResourceAsStream("java/lang/ClassLoader.class");
    if (in == null) { throw new RuntimeException("no bytes for java.lang.ClassLoader"); }

    try {
      return IOUtils.toByteArray(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    ClassInfo classInfo;
    try {
      classInfo = AsmClassInfo.getClassInfo(classfileBuffer, loader);
    } catch (Exception e) {
      // this might happen if the input bytes are purposefully not a valid .class file (see InvalidClassBytesTest)
      return classfileBuffer;
    }

    // classloader subclass that overrides loadClass(String)
    final boolean needsClassLoaderInstrumentation = needsClassLoaderInstrumentation(classInfo);

    if (needsClassLoaderInstrumentation) {
      final ClassReader clReader = new ClassReader(classfileBuffer);
      final ClassWriter clWriter = new ClassWriter(clReader, ClassWriter.COMPUTE_MAXS);
      ClassVisitor clVisitor = new ClassLoaderSubclassAdapter(clWriter);
      clReader.accept(clVisitor, ClassReader.SKIP_FRAMES);
      classfileBuffer = clWriter.toByteArray();

      AdaptedClassDumper.INSTANCE.write(className, classfileBuffer);
    }

    return classfileBuffer;

  }

  private static boolean needsClassLoaderInstrumentation(final ClassInfo classInfo) {
    for (ClassInfo c = classInfo; c != null; c = c.getSuperclass()) {
      if ("java.lang.ClassLoader".equals(c.getName())) {
        // found ClassLoader in the heirarchy of subclasses, now check for a definition of loadClass in this class
        for (MethodInfo m : classInfo.getMethods()) {
          if ("loadClass".equals(m.getName()) && "(Ljava/lang/String;)Ljava/lang/Class;".equals(m.getSignature())) { return true; }
        }
        return false;

      }
    }
    return false;
  }

}
