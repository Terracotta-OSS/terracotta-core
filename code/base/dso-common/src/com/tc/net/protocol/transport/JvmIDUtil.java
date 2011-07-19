/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.UUID;

/**
 * Generates a 48-byte unique identifier that is global to the JVM. One portion of the id (36 bytes) comes from a
 * java.util.UUID which is shared among all class loaders by storing it as a system property. To deter from easy
 * spoofing (e.g. a rogue class setting the system property to some same-value on all jvms) the remaining portion of the
 * id (12 bytes) is appended after being generated from the JVM's start time, represented as hex.
 * 
 * @author jhouse
 */
public class JvmIDUtil {

  private static final String SYS_PROP_KEY_FOR_JVM_ID_BASE_SHARE = "___terracotta_jvm_uuid";

  private static String       jvmId;

  private static String generateJVMIdBaseShare() {
    return UUID.randomUUID().toString();
  }

  private static String generateJVMIdExtensionShare() {
    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
    return Long.toHexString(bean.getStartTime());
  }

  public static String getJvmID() {
    synchronized (SYS_PROP_KEY_FOR_JVM_ID_BASE_SHARE.intern()) {
      if (jvmId != null) return jvmId;

      String base = System.getProperty(SYS_PROP_KEY_FOR_JVM_ID_BASE_SHARE);
      if (base == null) {
        base = generateJVMIdBaseShare();
        System.setProperty(SYS_PROP_KEY_FOR_JVM_ID_BASE_SHARE, base);
      }

      jvmId = base + "-" + generateJVMIdExtensionShare();

      return jvmId;
    }
  }

}
