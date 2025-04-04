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
package com.tc.net.protocol.tcm;


import com.tc.net.core.TCConnectionManager;
import junit.framework.TestCase;
import static org.mockito.Mockito.mock;
import org.slf4j.LoggerFactory;

public class MessageMonitorImplTest extends TestCase {
  
  public void tests() throws Exception {
    MessageMonitorImpl mm = new MessageMonitorImpl(null, LoggerFactory.getLogger(MessageMonitorImplTest.class), mock(TCConnectionManager.class), 5);
    mm.newIncomingMessage(new TestTCMessage());
    mm.newOutgoingMessage(new TestTCMessage());
    System.out.println(mm);
  }
}
