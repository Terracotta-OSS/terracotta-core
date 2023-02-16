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
package com.tc.net.protocol;

import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class TCNetworkMessageImplTest {
  @Test
  public void testMultipleCompleteActions() {
    Runnable r1 = mock(Runnable.class);
    Runnable r2 = mock(Runnable.class);
    TCNetworkHeader hdr = mock(TCNetworkHeader.class);
    TCNetworkMessage msg = new TCNetworkMessageImpl(hdr);
    msg.addCompleteCallback(r1);
    msg.addCompleteCallback(r2);
    msg.complete();
    verify(r1).run();
    verify(r2).run();
  }
}
