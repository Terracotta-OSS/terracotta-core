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
