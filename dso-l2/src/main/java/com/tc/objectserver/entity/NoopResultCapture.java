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
package com.tc.objectserver.entity;

import com.tc.objectserver.api.ResultCapture;
import com.tc.tracing.Trace;
import java.util.function.Supplier;
import org.terracotta.exception.EntityException;

/**
 *
 */
 
public class NoopResultCapture implements ResultCapture {
  
  public NoopResultCapture() {
  }

  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waitFor) {

  }

  @Override
  public void waitForReceived() {

  }

  @Override
  public void received() {
    Trace.activeTrace().log("Received");
  }

  @Override
  public void complete() {
    Trace.activeTrace().log("Completed without result ");
  }  

  @Override
  public void complete(byte[] value) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Completed with result of length " + value.length);
    }
  }

  @Override
  public void failure(EntityException ee) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Failure - exception: " + ee.getLocalizedMessage());
    }
  }
  
  @Override
  public void message(byte[] message) {
    
  }

  @Override
  public void retired() {
    Trace.activeTrace().log("Retired");
  }
  
  
}
