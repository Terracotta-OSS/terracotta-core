/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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

  @Override
  public String toString() {
    return uuid;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof UUID) {
      return toString().equals(obj.toString());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  public static void main(String args[]) {
    for (int i = 0; i < 10; i++) {
      System.out.println(UUID.getUUID());
    }
  }

}
