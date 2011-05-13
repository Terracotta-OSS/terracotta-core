/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

public interface DerbyDataTypes {
  String TC_STRING           = "VARCHAR (32672)";
  String TC_BYTE_ARRAY_KEY   = "VARCHAR (32672) FOR BIT DATA";
  String TC_BYTE_ARRAY_VALUE = "BLOB(1024M)";
  String TC_LONG             = "BIGINT";
  String TC_INT              = "INT";
}
