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
