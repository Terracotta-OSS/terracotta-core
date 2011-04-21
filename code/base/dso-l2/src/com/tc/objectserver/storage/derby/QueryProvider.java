/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

public interface QueryProvider {
  String createControlDBTable(String tableName, String key, String value);

  String createBytesToBlobDBTable(String tableName, String key, String value);

  String createIntToBytesDBTable(String tableName, String key, String value);

  String createLongDBTable(String tableName, String key);

  String createLongToStringDBTable(String tableName, String key, String value);

  String createMapsDBTable(String tableName, String id, String key, String value);

  String createObjectDBTable(String tableName, String key, String value);

  String createRootDBTable(String tableName, String key, String value);

  String createStringToStringDBTable(String tableName, String key, String value);
}
