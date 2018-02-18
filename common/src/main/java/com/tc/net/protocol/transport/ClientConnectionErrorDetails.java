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
package com.tc.net.protocol.transport;

import com.tc.net.core.ConnectionInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnectionErrorDetails implements ClientConnectionErrorListener {

  private volatile ConcurrentHashMap<ConnectionInfo, ConcurrentLinkedQueue<Exception>> exceptionMap;

  @Override
  public void onError(ConnectionInfo connInfo, Exception e) {
    ConcurrentHashMap<ConnectionInfo, ConcurrentLinkedQueue<Exception>> internalExceptionCollector = exceptionMap;
    if (internalExceptionCollector != null) {
      /*
      Effectively Keeping only the last exception for a given connection info. Keeping the internal data-structure as 
      list only to support (future) use cases where we need to store last N (or All) exceptions for a given connection.
       */
      ConcurrentLinkedQueue<Exception> exceptionList = new ConcurrentLinkedQueue<>();
      exceptionList.add(e);
      internalExceptionCollector.put(connInfo, exceptionList);
    }
  }

  public Map<String, List<Exception>> getErrors() {
    Map<String, List<Exception>> errorMessagesMap = new HashMap<>();
    ConcurrentHashMap<ConnectionInfo, ConcurrentLinkedQueue<Exception>> internalExceptionCollector = exceptionMap;
    if (internalExceptionCollector != null) {
      for (Map.Entry<ConnectionInfo, ConcurrentLinkedQueue<Exception>> entry : internalExceptionCollector.entrySet()) {
        ConnectionInfo connInfo = entry.getKey();
        ConcurrentLinkedQueue<Exception> exceptionList = entry.getValue();
        Object[] errorObjects = exceptionList.toArray();
        List<Exception> errorMessages = new ArrayList<>();
        for (Object errorObj : errorObjects) {
          Exception e = (Exception) errorObj;
          errorMessages.add(e);
        }
        errorMessagesMap.put(connInfo.toString(), errorMessages);
      }
    }
    return errorMessagesMap;
  }
  
  public void attachCollector() {
    exceptionMap =  new ConcurrentHashMap<>();
  }
  
  public void removeCollector() {
    exceptionMap = null;
  }
}
