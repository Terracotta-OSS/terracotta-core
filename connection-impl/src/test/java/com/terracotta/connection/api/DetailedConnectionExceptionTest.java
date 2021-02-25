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
