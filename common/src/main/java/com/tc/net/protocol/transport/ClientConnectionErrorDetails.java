/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnectionErrorDetails implements ClientConnectionErrorListener {

  private volatile ConcurrentHashMap<InetSocketAddress, ConcurrentLinkedQueue<Exception>> exceptionMap;

  @Override
  public void onError(InetSocketAddress serverAddress, Exception e) {
    ConcurrentHashMap<InetSocketAddress, ConcurrentLinkedQueue<Exception>> internalExceptionCollector = exceptionMap;
    if (internalExceptionCollector != null) {
      /*
      Effectively Keeping only the last exception for a given connection info. Keeping the internal data-structure as 
      list only to support (future) use cases where we need to store last N (or All) exceptions for a given connection.
       */
      ConcurrentLinkedQueue<Exception> exceptionList = new ConcurrentLinkedQueue<>();
      exceptionList.add(e);
      internalExceptionCollector.put(serverAddress, exceptionList);
    }
  }

  public Map<String, List<Exception>> getErrors() {
    Map<String, List<Exception>> errorMessagesMap = new HashMap<>();
    ConcurrentHashMap<InetSocketAddress, ConcurrentLinkedQueue<Exception>> internalExceptionCollector = exceptionMap;
    if (internalExceptionCollector != null) {
      for (Map.Entry<InetSocketAddress, ConcurrentLinkedQueue<Exception>> entry : internalExceptionCollector.entrySet()) {
        InetSocketAddress serverAddress = entry.getKey();
        ConcurrentLinkedQueue<Exception> exceptionList = entry.getValue();
        Object[] errorObjects = exceptionList.toArray();
        List<Exception> errorMessages = new ArrayList<>();
        for (Object errorObj : errorObjects) {
          Exception e = (Exception) errorObj;
          errorMessages.add(e);
        }
        errorMessagesMap.put(serverAddress.toString(), errorMessages);
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
