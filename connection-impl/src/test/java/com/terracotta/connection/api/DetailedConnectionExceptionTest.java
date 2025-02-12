/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;

public class DetailedConnectionExceptionTest {

  @Test
  public void testGetConnectionErrorMapWithNullMap(){
    Exception exc = mock(Exception.class);
    DetailedConnectionException connException = new DetailedConnectionException(exc, null);
    Map<String, List<Exception>> exceptionMap = connException.getConnectionErrorMap();
    assertNull(exceptionMap);
  }

  @Test
  public void testGetConnectionErrorMap(){
    Exception thrownException = mock(Exception.class);
    String endoint = "localhost:9510";
    Exception e1 = new IOException("first exception");
    Exception e2 = new IOException("second exception");
    List<Exception> errorList = new ArrayList<>();
    errorList.add(e1);
    errorList.add(e2);
    Map<String, List<Exception>> errorMap = new HashMap<>();
    errorMap.put(endoint, errorList);
    DetailedConnectionException connException = new DetailedConnectionException(thrownException, errorMap);
    Map<String, List<Exception>> exceptionMap = connException.getConnectionErrorMap();
    assertNotNull(exceptionMap);
    assertEquals(1, exceptionMap.size());
    
    List<Exception> retExceptions = exceptionMap.get(endoint);
    assertNotNull(retExceptions);
    assertEquals(2, retExceptions.size());
    assertTrue(retExceptions.contains(e1));
    assertTrue(retExceptions.contains(e2));
  }
}
