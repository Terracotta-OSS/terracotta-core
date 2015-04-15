/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
