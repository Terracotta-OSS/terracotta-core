/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Random;

public class UUID {

  private static final TCLogger logger = TCLogging.getLogger(UUID.class);
  private static final Method   jdk15createMethod;

  private final String          uuid;

  public static UUID getUUID() {
    if (jdk15createMethod != null) { return new UUID(getJDK15()); }
    return new UUID(getDefault());
  }

  private static String getDefault() {
    Random r = new SecureRandom();
    long l1 = r.nextLong();
    long l2 = r.nextLong();
    return Long.toHexString(l1) + Long.toHexString(l2);
  }

  private static String getJDK15() {
    try {
      Object object = jdk15createMethod.invoke(null, new Object[] {});
      String s = object.toString();
      return s.replaceAll("[^A-Fa-f0-9]", "");
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public String toString() {
    return uuid;
  }

  private UUID(String uuid) {
    this.uuid = makeSize32(uuid);
    if(32 != this.uuid.length()) {
      throw new AssertionError("UUID : length is not 32 but " + this.uuid.length() + " : UUID is : " + this.uuid);
    }
  }

  // TODO:: TIM FIXME
  private String makeSize32(String uuid2) {
    int len = uuid2.length() ;
    if(len < 32 ) {
      StringBuffer sb = new StringBuffer(32);
      while(len ++ < 32) {
        sb.append('0');
      }
      sb.append(uuid2);
      return sb.toString();
    } else if ( len > 32) {
      return uuid2.substring(0, 32);
    } else {
      return uuid2;
    }
  }

  public static boolean usesJDKImpl() {
    return jdk15createMethod != null;
  }

  static {
    Method jdk15UUID = null;
    try {
      Class c = Class.forName("java.util.UUID");
      jdk15UUID = c.getDeclaredMethod("randomUUID", new Class[] {});
    } catch (Throwable t) {
      logger.warn("JDK1.5+ UUID class not available, falling back to default implementation. " + t.getMessage());
    }

    jdk15createMethod = jdk15UUID;
  }

  public static void main(String args[]) {
    for (int i = 0; i < 10; i++) {
      System.out.println(UUID.getUUID());
    }
  }

}
