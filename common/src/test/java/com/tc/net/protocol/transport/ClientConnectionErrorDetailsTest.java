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
package com.tc.net.protocol.transport;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static com.tc.util.Assert.assertEquals;
import static com.tc.util.Assert.assertNotNull;
import static com.tc.util.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ClientConnectionErrorDetailsTest {

  @Test
  public void testOnError() {
    ClientConnectionErrorDetails errorDetails = new ClientConnectionErrorDetails();
    errorDetails.attachCollector();
    InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost", 9510);
    Exception exception1 = new IOException("Test Exception1");
    errorDetails.onError(serverAddress, exception1);
    Exception exception2 = new IOException("Test Exception2");
    errorDetails.onError(serverAddress, exception2);

    Map<String, List<Exception>> retMap = errorDetails.getErrors();
    retMap.get(serverAddress.toString());
    assertNotNull(retMap);
    assertEquals(1,retMap.size());
    List<Exception> errorList = retMap.get(serverAddress.toString());
    /*
    Stores only the last exeption
     */
    assertEquals(1,errorList.size());
    assertFalse(errorList.contains(exception1));
    assertTrue(errorList.contains(exception2));
  }

  @Test
  public void testGetErrorWithNoError() {
    ClientConnectionErrorDetails errorDetails = new ClientConnectionErrorDetails();
    errorDetails.attachCollector();
    Map<String, List<Exception>> retMap = errorDetails.getErrors();
    assertNotNull(retMap);
    assertTrue(retMap.isEmpty());
  }

  @Test
  public void testGetError() {
    ClientConnectionErrorDetails errorDetails = new ClientConnectionErrorDetails();
    errorDetails.attachCollector();
    InetSocketAddress serverAddress1 = InetSocketAddress.createUnresolved("localhost",9510);
    Exception exception1 = new IOException("Test Exception1");
    errorDetails.onError(serverAddress1, exception1);
    Exception exception2 = new IOException("Test Exception2");
    errorDetails.onError(serverAddress1, exception2);

    InetSocketAddress serverAddress2 = InetSocketAddress.createUnresolved("localhost",9710);
    Exception exception3 = new IOException("Test Exception3");
    errorDetails.onError(serverAddress2, exception3);

    Map<String, List<Exception>> retMap = errorDetails.getErrors();
    assertNotNull(retMap);
    assertEquals(2, retMap.size());
    List<Exception> exceptionList1 = retMap.get(serverAddress1.toString());

    assertEquals(1,exceptionList1.size());
    assertFalse(exceptionList1.contains(exception1));
    assertTrue(exceptionList1.contains(exception2));

    List<Exception> exceptionList2 = retMap.get(serverAddress2.toString());

    assertEquals(1,exceptionList2.size());
    assertTrue(exceptionList2.contains(exception3));
  }
  
  @Test
  public void testDetatchCollector() {
    ClientConnectionErrorDetails errorDetails = new ClientConnectionErrorDetails();
    errorDetails.attachCollector();
    InetSocketAddress serverAddress = InetSocketAddress.createUnresolved("localhost",9510);
    Exception exception1 = new IOException("Test Exception1");
    errorDetails.onError(serverAddress, exception1);

    Map<String, List<Exception>> retMap = errorDetails.getErrors();
    retMap.get(serverAddress);
    assertNotNull(retMap);
    assertEquals(1,retMap.size());
    List<Exception> errorList = retMap.get(serverAddress.toString());
    assertEquals(1,errorList.size());

    errorDetails.removeCollector();
    
    /*
    Collector is detached. Internal errors are not available
     */
    retMap = errorDetails.getErrors();
    assertNotNull(retMap);
    assertEquals(0,retMap.size());
  }
}