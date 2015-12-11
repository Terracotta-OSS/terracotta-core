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
package com.tc.object;

import org.terracotta.exception.EntityException;

import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage.Acks;

import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;
import static org.mockito.Mockito.mock;


public class InFlightMessageTest extends TestCase {
  public void testInterruptedGet() {
    // Create the message we will use in the test.
    NetworkVoltronEntityMessage mockedEntityMessage = mock(NetworkVoltronEntityMessage.class);
    // We need to use the interlock message since we want to interrupt only after the get() has been called to ensure that
    // the message interrupt call actually knows which thread to interrupt.
    // While we could interrupt the thread before the blocking call, and it would still behave as if we interrupted it after
    // it started the call, the message won't know what thread(s) are blocked until they call it.
    InterlockMessage message = new InterlockMessage(mockedEntityMessage, Collections.<Acks>emptySet());
    // Create the thread which we will interrupt.
    InterruptableThread thread = new InterruptableThread(message);
    
    // Start the thread: it will begin progressing to the get().
    thread.start();
    // Wait for the other thread to enter the monitor.
    message.waitOnEnter();
    // We can now interrupt the message and know that the other thread will receive the interrupt.
    message.interrupt();
    try {
      thread.join();
    } catch (InterruptedException e) {
      // NOT expected on our side.
      fail();
    }
    assertTrue(thread.didInterrupt);
  }

  /**
   * This thread just calls get() on the given message and records whether or not it was interrupted.
   */
  private static class InterruptableThread extends Thread {
    public boolean didInterrupt;
    private final InFlightMessage message;

    public InterruptableThread(InFlightMessage message) {
      this.didInterrupt = false;
      this.message = message;
    }

    @Override
    public void run() {
      try {
        message.get();
      } catch (InterruptedException e) {
        this.didInterrupt = true;
      } catch (EntityException e) {
        // This was NOT expected in this test.
        fail();
      }
    }
  }

  /**
   * Even though we are trying to test InFlightMessage we need this subclass to expose details of the monitor state in order
   * to test the thread interaction in a deterministic way.
   */
  private static class InterlockMessage extends InFlightMessage {
    private boolean didEnter;
    
    public InterlockMessage(NetworkVoltronEntityMessage message, Set<Acks> acks) {
      super(message, acks);
      this.didEnter = false;
    }

    @Override
    public synchronized byte[] get() throws InterruptedException, EntityException {
      // Notify anyone waiting so that they know we now have the monitor and are going to block in get().
      this.didEnter = true;
      notifyAll();
      // Call the super to actually block.
      return super.get();
    }
    
    /**
     * Blocks the caller until someone has called get() on the same instance.
     */
    public synchronized void waitOnEnter() {
      while (!this.didEnter) {
        try {
          wait();
        } catch (InterruptedException e) {
          // NOT expected.
          fail();
        }
      }
    }
  }
}
