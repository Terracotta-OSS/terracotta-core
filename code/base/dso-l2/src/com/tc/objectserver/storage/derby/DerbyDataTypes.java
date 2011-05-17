/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

public interface DerbyDataTypes {
  String STRING      = "VARCHAR (32672)";
  String VARCHAR_32K = "VARCHAR (32672) FOR BIT DATA";
  String BLOB_1G     = "BLOB(1024M)";
  String LONG        = "BIGINT";
  String INT         = "INT";
}
