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


import com.tc.bytes.TCReference;
import com.tc.net.protocol.TCNetworkHeader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import com.tc.net.protocol.TCNetworkMessageImpl;
import java.util.EnumSet;
import java.util.Objects;

/**
 */
public class TCActionNetworkMessageImpl extends TCNetworkMessageImpl implements TCActionNetworkMessage {

  private final Supplier<TCReference> payloadSupplier;
  private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

  public TCActionNetworkMessageImpl(TCNetworkHeader header, Supplier<TCReference> payloadSupplier) {
    super(header);
    Objects.requireNonNull(payloadSupplier);
    this.payloadSupplier = payloadSupplier;
  }

  @Override
  public boolean load() {
    if (updateState(State.SEALED, EnumSet.of(State.PENDING))) {
      TCReference ref = payloadSupplier.get();
      if (ref != null) {
        setPayload(ref);
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean commit() {
    return updateState(State.COMMITTED, EnumSet.of(State.SEALED));
  }

  @Override
  public boolean cancel() {
    return updateState(State.CANCELLED, EnumSet.of(State.PENDING, State.SEALED));
  }
  
  private boolean updateState(State to, EnumSet<State> ifCurrentIs) {
    return state.accumulateAndGet(to, (current, update)->{
      if (ifCurrentIs.contains(current)) {
        return update;
      } else {
        return current;
      }
    }) == to;
  }
  
  @Override
  public boolean isCancelled() {
    return state.get() == State.CANCELLED;
  }

  enum State {
    PENDING,
    SEALED,
    COMMITTED,
    CANCELLED;
  }
}
