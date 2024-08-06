/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
