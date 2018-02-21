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
import org.junit.Test;

import java.io.IOException;
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
    ConnectionInfo connInfo = new ConnectionInfo("localhost",9510);
    Exception exception1 = new IOException("Test Exception1");
    errorDetails.onError(connInfo, exception1);
    Exception exception2 = new IOException("Test Exception2");
    errorDetails.onError(connInfo, exception2);

    Map<String, List<Exception>> retMap = errorDetails.getErrors();
    retMap.get(connInfo);
    assertNotNull(retMap);
    assertEquals(1,retMap.size());
    List<Exception> errorList = retMap.get(connInfo.toString());
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
    ConnectionInfo connInfo1 = new ConnectionInfo("localhost",9510);
    Exception exception1 = new IOException("Test Exception1");
    errorDetails.onError(connInfo1, exception1);
    Exception exception2 = new IOException("Test Exception2");
    errorDetails.onError(connInfo1, exception2);

    ConnectionInfo connInfo2 = new ConnectionInfo("localhost",9710);
    Exception exception3 = new IOException("Test Exception3");
    errorDetails.onError(connInfo2, exception3);

    Map<String, List<Exception>> retMap = errorDetails.getErrors();
    assertNotNull(retMap);
    assertEquals(2, retMap.size());
    List<Exception> exceptionList1 = retMap.get(connInfo1.toString());

    assertEquals(1,exceptionList1.size());
    assertFalse(exceptionList1.contains(exception1));
    assertTrue(exceptionList1.contains(exception2));

    List<Exception> exceptionList2 = retMap.get(connInfo2.toString());

    assertEquals(1,exceptionList2.size());
    assertTrue(exceptionList2.contains(exception3));
  }
  
  @Test
  public void testDetatchCollector() {
    ClientConnectionErrorDetails errorDetails = new ClientConnectionErrorDetails();
    errorDetails.attachCollector();
    ConnectionInfo connInfo = new ConnectionInfo("localhost",9510);
    Exception exception1 = new IOException("Test Exception1");
    errorDetails.onError(connInfo, exception1);

    Map<String, List<Exception>> retMap = errorDetails.getErrors();
    retMap.get(connInfo);
    assertNotNull(retMap);
    assertEquals(1,retMap.size());
    List<Exception> errorList = retMap.get(connInfo.toString());
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