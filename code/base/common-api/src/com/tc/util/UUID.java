/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

public class UUID implements java.io.Serializable {

  public static final UUID NULL_ID;
  public static final int  SIZE;
  private final String     uuid;

  /*
   * making NULL_ID length same as that of other UUIDs
   */
  static {
    int uuidLength = getRandomUUID().toString().length();
    StringBuilder sb = new StringBuilder(uuidLength);
    for (int i = 0; i < uuidLength; i++) {
      sb.append('f');
    }
    NULL_ID = new UUID(sb.toString());
    SIZE = NULL_ID.toString().length();
  }

  public static UUID getUUID() {
    return new UUID(getRandomUUID());
  }

  private static String getRandomUUID() {
    java.util.UUID uuid = java.util.UUID.randomUUID();
    String s = uuid.toString();
    return s.replaceAll("[^A-Fa-f0-9]", "");
  }

  public UUID(String uuid) {
    this.uuid = uuid;
  }

  public String toString() {
    return uuid;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof UUID)) { return false; }
    UUID peerObj = (UUID) obj;
    return (this.toString().equals(peerObj.toString()));
  }

  public static void main(String args[]) {
    for (int i = 0; i < 10; i++) {
      System.out.println(UUID.getUUID());
    }
  }

}
