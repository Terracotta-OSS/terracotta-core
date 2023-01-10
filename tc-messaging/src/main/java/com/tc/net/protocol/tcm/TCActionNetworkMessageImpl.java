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


import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.TCNetworkHeader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import com.tc.net.protocol.TCNetworkMessageImpl;

/**
 */
public class TCActionNetworkMessageImpl extends TCNetworkMessageImpl implements TCActionNetworkMessage {

  private final Supplier<TCByteBuffer[]> payloadSupplier;
  private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

  public TCActionNetworkMessageImpl(TCNetworkHeader header, Supplier<TCByteBuffer[]> payloadSupplier) {
    super(header);
    this.payloadSupplier = payloadSupplier;
  }

  @Override
  public boolean load() {
    if (!isSealed() && !isCancelled() && payloadSupplier != null) {
      setPayload(payloadSupplier.get());
      seal();
      return true;
    } else {
      return !isCancelled();
    }
  }

  @Override
  public boolean commit() {
    return state.compareAndSet(State.PENDING, State.COMMITTED) || State.COMMITTED.equals(state.get());
  }

  @Override
  public boolean cancel() {
    if (state.compareAndSet(State.PENDING, State.CANCELLED) || State.CANCELLED.equals(state.get())) {
      setPayload(null);
      complete();
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public boolean isCancelled() {
    return state.get() == State.CANCELLED;
  }

  enum State {
    PENDING,
    COMMITTED,
    CANCELLED;
  }
}
