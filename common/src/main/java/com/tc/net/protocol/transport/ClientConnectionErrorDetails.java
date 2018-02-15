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

public class ClientConnectionErrorDetails implements ClientConnectionErrorListener{

  private final ConcurrentHashMap<ConnectionInfo, ConcurrentLinkedQueue<Exception>> exceptionMap
      = new ConcurrentHashMap<>();

  @Override
  public void onError(ConnectionInfo connInfo, Exception e) {
    ConcurrentLinkedQueue<Exception> exceptionList = exceptionMap.get(connInfo);
    if (exceptionList == null) {
      exceptionList = new ConcurrentLinkedQueue<>();
      exceptionMap.put(connInfo, exceptionList);
    }
    exceptionList.add(e);
  }

  public Map<String, List<Exception>> getErrors() {
    Map<String, List<Exception>> errorMessagesMap = new HashMap<>();
    if (exceptionMap != null) {
      for (Map.Entry<ConnectionInfo, ConcurrentLinkedQueue<Exception>> entry : exceptionMap.entrySet()) {
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
}
