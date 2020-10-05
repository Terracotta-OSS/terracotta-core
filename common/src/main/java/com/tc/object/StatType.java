/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

/**
 *
 */
 public enum StatType {
  CLIENT_ENCODE("CLIENT:encode"), 
  CLIENT_SEND("CLIENT:send"), 
  CLIENT_SENT("CLIENT:sent"), 
  CLIENT_RECEIVED("CLIENT:receivedAck"), 
  CLIENT_COMPLETE("CLIENT:complete"), 
  CLIENT_GOT("CLIENT:retrieved"), 
  CLIENT_DECODED("CLIENT:decoded"), 
  CLIENT_RETIRED("CLIENT:retired"),
  SERVER_ADD("SERVER:add"),
  SERVER_RECEIVED("SERVER:received"),
  SERVER_SCHEDULE("SERVER:schedule"),
  SERVER_BEGININVOKE("SERVER:begin"),
  SERVER_ENDINVOKE("SERVER:end"),
  SERVER_COMPLETE("SERVER:complete"),
  SERVER_RETIRED("SERVER:retired"),
  END("NOT USED")
  ;
  
  private final String description;

  StatType(String description) {
    this.description = description;
  }

  public String description() {
    return this.description;
  }
  
  public int serverSpot() {
    return this.ordinal() - CLIENT_RETIRED.ordinal();
  }
}
