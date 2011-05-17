/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

class DerbyQueryProvider implements QueryProvider {

  public String createBytesToBlobDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.VARCHAR_32K + ", " + value + " "
           + DerbyDataTypes.BLOB_1G + ", PRIMARY KEY(" + key + ") )";
  }

  public String createControlDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " VARCHAR (10), " + value + " SMALLINT, PRIMARY KEY(" + key
           + ") )";
  }

  public String createIntToBytesDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.INT + ", " + value + " "
           + DerbyDataTypes.BLOB_1G + ", PRIMARY KEY(" + key + ") )";
  }

  public String createLongDBTable(String tableName, String key) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.LONG + ", PRIMARY KEY(" + key + ") )";
  }

  public String createLongToStringDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.LONG + ", " + value + " "
           + DerbyDataTypes.STRING + ", PRIMARY KEY(" + key + ") )";
  }

  public String createMapsDBTable(String tableName, String mapId, String key, String bigKey, String value) {
    return "CREATE TABLE " + tableName + "(" + mapId + " " + DerbyDataTypes.LONG + ", " + key + " "
           + DerbyDataTypes.VARCHAR_32K + ", " + bigKey + " " + DerbyDataTypes.BLOB_1G + ", " + value + " "
           + DerbyDataTypes.BLOB_1G + ", PRIMARY KEY(" + mapId + ", " + key + ") )";
  }

  public String createObjectDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.LONG + ", " + value + " "
           + DerbyDataTypes.BLOB_1G + ", PRIMARY KEY(" + key + ") )";
  }

  public String createRootDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.VARCHAR_32K + ", " + value + " "
           + DerbyDataTypes.LONG + ", PRIMARY KEY(" + key + ") )";
  }

  public String createStringToStringDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.STRING + ", " + value + " "
           + DerbyDataTypes.STRING + ", PRIMARY KEY(" + key + ") )";
  }

}
