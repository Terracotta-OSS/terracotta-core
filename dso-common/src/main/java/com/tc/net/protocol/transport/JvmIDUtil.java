/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.transport;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.UUID;

/**
 * Generates a unique identifier that is global to the JVM. One portion of the id comes from a java.util.UUID which is
 * shared among all class loaders by storing it as a system property. To deter from easy spoofing (e.g. a rogue class
 * setting the system property to some same-value on all jvms) the remaining portion of the id is appended after being
 * generated from the JVM's start time, represented as hex.
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
