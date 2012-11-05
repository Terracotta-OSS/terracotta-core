/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

/**
 *
 * @author mscott
 */
public enum SequenceNames {
  CLIENTID_SEQUENCE_NAME("clientids"),
  OID_STORE_LOG_SEQUENCE_DB_NAME("oid_store_log_sequence"),
  TRANSACTION_SEQUENCE_DB_NAME("transactionsequence"),
  OBJECTID_SEQUENCE_NAME("objectids"),
  DGC_SEQUENCE_NAME("dgcSequence");
  
  String name;
  
  SequenceNames(String name) {
      this.name = name;
  }
  
  public String getName() {
      return name;
  }
}
