/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.common.IPCMessageConstants;


/**
 * Acts as the lock-step synchronization mechanism, on the client side.  Whenever the client wants to ask the harness to do
 * something, it sends a message, via the output stream, then blocks on the response (input stream processed in another
 * thread).  Sometimes the response is just an ACK but sometimes it is the unblocking of a cross-process
 * locking/synchronization operation.
 */
public class ClientSideIPCManager {
  private final PrintStream outputStream;
  private final Thread streamProcessor;
  private String nextEventString;
  private boolean didGetEvent;

  public ClientSideIPCManager(final InputStream inputStream, PrintStream outputStream) {
    this.outputStream = outputStream;
    
    this.streamProcessor = new Thread() {
      @Override
      public void run() {
        // We want to look for events one line at a time so use a buffered reader.
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
          // We will read until IOException.
          while (true) {
            String line = reader.readLine();
            ClientSideIPCManager.this.didReadLine(line);
          }
        } catch (IOException e) {
          // We don't expect IOException in this case.
          Assert.unexpected(e);
        }
      }
    };
    
    // XXX: We probably should correctly life-cycle this but there is only one per client, and it lives for the entire client
    // so we will set it to daemon.
    this.streamProcessor.setDaemon(true);
    this.streamProcessor.start();
  }

  private synchronized void didReadLine(String line) {
    if (null != this.nextEventString) {
      int index = line.indexOf(this.nextEventString);
      if (-1 != index) {
        // We found the line so we want to set the flag, clear the event string, and notify.
        this.didGetEvent = true;
        this.nextEventString = null;
        notifyAll();
      }
    }
  }

  public void sendSyncAndWait() {
    sendAndWait(IPCMessageConstants.SYNC);
  }

  public void startOneServer() { sendAndWait(IPCMessageConstants.START_ONE_SERVER); }

  public void terminateActive() { sendAndWait(IPCMessageConstants.TERMINATE_ACTIVE); }

  public void shutDownStripeAndWaitForTermination() {
    sendAndWait(IPCMessageConstants.SHUT_DOWN_STRIPE);
  }

  public void waitForActive() {
    sendAndWait(IPCMessageConstants.WAIT_FOR_ACTIVE);
  }

  public void waitForPassive() {
    sendAndWait(IPCMessageConstants.WAIT_FOR_PASSIVE);
  }

  public void sendShutDownAndWait() {
    sendAndWait(IPCMessageConstants.CLIENT_SHUT_DOWN);
  }


  private synchronized void sendAndWait(String eventName) {
    // Unset the flag and set what we want to see.
    this.didGetEvent = false;
    this.nextEventString = IPCMessageConstants.ackFrom(eventName);
    
    // Write the syn.
    this.outputStream.println(IPCMessageConstants.synFrom(eventName));
    this.outputStream.flush();
    // Wait for the ack.
    while (!this.didGetEvent) {
      try {
        wait();
      } catch (InterruptedException e) {
        // TODO:  Determine if we want to support interruption in this testing stub.
        Assert.unexpected(e);
      }
    }
  }
}
