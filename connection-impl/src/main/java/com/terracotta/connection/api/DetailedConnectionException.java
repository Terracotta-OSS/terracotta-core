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
package com.terracotta.connection.api;

import org.terracotta.connection.ConnectionException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DetailedConnectionException extends ConnectionException {

  private final Map<String, List<Exception>> connectionErrorMap;

  public DetailedConnectionException(Throwable cause, Map<String, List<Exception>> errorMap) {
    super(cause);
    this.connectionErrorMap = errorMap;
  }

  public Map<String, List<Exception>> getConnectionErrorMap() {
    if (this.connectionErrorMap == null) {
      return null;
    }
    return Collections.unmodifiableMap(connectionErrorMap);
  }
  
  public static String getDetailedMessage(Map<String, List<Exception>> errorMap) {
    StringBuilder builder = new StringBuilder();
    errorMap.forEach((k, v)-> {
      if (!v.isEmpty()) {
        builder.append(k);
        builder.append('=');
        builder.append(v.get(0).getMessage());
        builder.append(';');
      }
    });
    return builder.toString();
  }
}