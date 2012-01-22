/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.util;

import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * Generates a UUID. <p/>A Universally Unique Identifier (UUID) is a 128 bit number generated according to an algorithm
 * that is garanteed to be unique in time A space from all other UUIDs.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class UuidGenerator {
  /**
   * Random seeder.
   */
  private static SecureRandom s_seeder = null;

  /**
   * Mid value, needed for calculation.
   */
  private static String s_midValue = null;

  /**
   * Defines if the generator is initialized or not.
   */
  private static boolean s_initialized = false;

  /**
   * Private constructor to prevent subclassing
   */
  private UuidGenerator() {
  }

  /**
   * Returns a unique uuid.
   *
   * @param obj the calling object (this)
   * @return a unique uuid
   */
  public static String generate(Object obj) {
    if (!s_initialized) {
      initialize(obj);
    }
    long timeNow = System.currentTimeMillis();

    // getDefault int value as unsigned
    int timeLow = (int) timeNow & 0xFFFFFFFF;
    int node = s_seeder.nextInt();
    return (hexFormat(timeLow, 8) + s_midValue + hexFormat(node, 8));
  }

  /**
   * Initializes the factory.
   *
   * @param obj
   */
  private synchronized static void initialize(final Object obj) {
    try {
      InetAddress inet = InetAddress.getLocalHost();
      byte[] bytes = inet.getAddress();
      String hexInetAddress = hexFormat(getInt(bytes), 8);
      String thisHashCode = hexFormat(System.identityHashCode(obj), 8);
      s_midValue = hexInetAddress + thisHashCode;
      s_seeder = new SecureRandom();
      s_seeder.nextInt();
    } catch (java.net.UnknownHostException e) {
      throw new Error("can not initialize the UuidGenerator generator");
    }
    s_initialized = true;
  }

  /**
   * Utility method.
   *
   * @param abyte
   * @return
   */
  private static int getInt(final byte[] abyte) {
    int i = 0;
    int j = 24;
    for (int k = 0; j >= 0; k++) {
      int l = abyte[k] & 0xff;
      i += (l << j);
      j -= 8;
    }
    return i;
  }

  /**
   * Utility method.
   *
   * @param i
   * @param j
   * @return
   */
  private static String hexFormat(final int i, final int j) {
    String s = Integer.toHexString(i);
    return padHex(s, j) + s;
  }

  /**
   * Utility method.
   *
   * @param str
   * @param i
   * @return
   */
  private static String padHex(final String str, final int i) {
    StringBuffer buf = new StringBuffer();
    if (str.length() < i) {
      for (int j = 0; j < (i - str.length()); j++) {
        buf.append('0');
      }
    }
    return buf.toString();
  }
}
