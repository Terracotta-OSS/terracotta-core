/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

class DerbyQueryProvider implements QueryProvider {

  public String createBytesToBlobDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.TC_BYTE_ARRAY_KEY + ", " + value + " "
           + DerbyDataTypes.TC_BYTE_ARRAY_VALUE + ", PRIMARY KEY(" + key + ") )";
  }

  public String createControlDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " VARCHAR (10), " + value + " SMALLINT, PRIMARY KEY(" + key
           + ") )";
  }

  public String createIntToBytesDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.TC_INT + ", " + value + " "
           + DerbyDataTypes.TC_BYTE_ARRAY_VALUE + ", PRIMARY KEY(" + key + ") )";
  }

  public String createLongDBTable(String tableName, String key) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.TC_LONG + ", PRIMARY KEY(" + key + ") )";
  }

  public String createLongToStringDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.TC_LONG + ", " + value + " "
           + DerbyDataTypes.TC_STRING + ", PRIMARY KEY(" + key + ") )";
  }

  public String createMapsDBTable(String tableName, String id, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + id + " " + DerbyDataTypes.TC_LONG + ", " + key + " "
           + DerbyDataTypes.TC_BYTE_ARRAY_KEY + ", " + value + " " + DerbyDataTypes.TC_BYTE_ARRAY_VALUE
           + ", PRIMARY KEY(" + key + "," + id + ") )";
  }

  public String createObjectDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.TC_LONG + ", " + value + " "
           + DerbyDataTypes.TC_BYTE_ARRAY_VALUE + ", PRIMARY KEY(" + key + ") )";
  }

  public String createRootDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.TC_BYTE_ARRAY_KEY + ", " + value + " "
           + DerbyDataTypes.TC_LONG + ", PRIMARY KEY(" + key + ") )";
  }

  public String createStringToStringDBTable(String tableName, String key, String value) {
    return "CREATE TABLE " + tableName + "(" + key + " " + DerbyDataTypes.TC_STRING + ", " + value + " "
           + DerbyDataTypes.TC_STRING + ", PRIMARY KEY(" + key + ") )";
  }

}
