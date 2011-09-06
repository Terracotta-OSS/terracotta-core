/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class InvalidClassBytesTestAgent implements ClassFileTransformer {

  private static final Thread MAIN_THREAD;

  public static final byte[]  MAGIC = new byte[] { 6, 6, 6 };
  public static final byte[]  REAL  = getRealBytes();

  static {
    MAIN_THREAD = Thread.currentThread();
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    inst.addTransformer(new InvalidClassBytesTestAgent());
  }

  private static byte[] getRealBytes() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream in = InvalidClassBytesTestAgent.class.getClassLoader()
        .getResourceAsStream(InvalidClassBytesTestAgent.class.getName().replace('.', '/').concat(".class"));
    try {
      int read;
      while ((read = in.read()) >= 0) {
        out.write(read);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return out.toByteArray();
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    // avoid deadlocks (MNK-1259, MNK-1259, et. al)
    if (Thread.currentThread() != MAIN_THREAD) { return null; }

    if (Arrays.equals(MAGIC, classfileBuffer)) {
      System.err.println("\nMagic found!\n");
      return REAL;
    }

    return null;
  }
}
