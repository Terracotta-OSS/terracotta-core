/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.passthrough;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.terracotta.entity.EntityMessage;


/**
 * This is a heavily simplified retirement manager which would have to be replaced/extended if we were to switch to a
 * multi-threaded passthrough server.
 * Additionally, it is currently very coarse:  on deferred message will block ALL retirement, not just the retirement on its
 * concurrency key.
 */
public class PassthroughRetirementManager {

  // This implementation is VERY simple and makes a few corresponding assumptions about how it is being used:
  // -only one message is being run at any time
  // -it is acceptable to treat the logical ordering constraints as global, instead of just within a key

  // The list of blocked tuples.  These represent the "global logical ordering" of retirement.
  private final LinkedList<RetirementTuple> blockedTuples;
  // The messages which are still blocking _some_ tuple in the blockedTuples list.
  private final Set<EntityMessage> blockingMessages;

  private final List<EntityMessage> blockCurrentMessageOn = new LinkedList<>();

  public PassthroughRetirementManager() {
    this.blockedTuples = new LinkedList<>();
    this.blockingMessages = Collections.newSetFromMap(new IdentityHashMap<>());
  }
  /**
   * Called to flag the currently executing message as one which must defer its retirement until the completion of the given
   * blockedOn message.
   * Note that this can only be called once for the currently executing message.
   * 
   * @param blockedOn The message on which the currently executing message must block its retirement
   */
  public synchronized void deferCurrentMessage(EntityMessage blockedOn) {
    this.blockCurrentMessageOn.add(blockedOn);
  }
  
  public synchronized boolean addRetirementTuple(RetirementTuple tuple) {
    boolean didBlockTuple = false;
    if (!this.blockedTuples.isEmpty() || !this.blockCurrentMessageOn.isEmpty()) {
      this.blockingMessages.addAll(this.blockCurrentMessageOn);
      tuple.blockedOn.addAll(this.blockCurrentMessageOn);
      this.blockedTuples.add(tuple);
      didBlockTuple = true;
      this.blockCurrentMessageOn.clear();
    }
    return didBlockTuple;
  }

  /**
   * Called to state that a message has completed execution and would like to retire.  This usually just returns the tuple
   * given, but may return 0, if there is a blockage, or multiple, if this resulted in unblocking part of the list.
   * 
   * @param completedInternalOrNull The completed message or null, if the message wasn't visible (this is ONLY non-null in
   * the cases where it was a message which blocked someone).
   * @return A list of any unblocked retirement operations, in the order they must be run
   */
  public synchronized List<RetirementTuple> retireableListAfterMessageDone(EntityMessage completedInternalOrNull) {
    
    if (null != completedInternalOrNull) {
      this.blockingMessages.remove(completedInternalOrNull);
      for (RetirementTuple blockedTuple : blockedTuples) {
        blockedTuple.blockedOn.remove(completedInternalOrNull);
      }
    }

    // Now, determine if anything is good to be retired.
    List<RetirementTuple> readyToRetire = new ArrayList<>();
    Iterator<RetirementTuple> blocked = this.blockedTuples.iterator();
    while (blocked.hasNext()) {
      RetirementTuple check = blocked.next();
      if (check.blockedOn.isEmpty()) {
        blocked.remove();
        readyToRetire.add(check);
      } else {
        break;
      }
    }

    return readyToRetire;
  }

  /**
   * Contains the basic data related to a frozen retirement operation.
   */
  public static class RetirementTuple {
    // This blockedOn field is only set if we are put into the blocked list.
    public Set<EntityMessage> blockedOn = Collections.newSetFromMap(new IdentityHashMap<>());
    public final PassthroughConnection sender;
    public final byte[] response;
    
    public RetirementTuple(PassthroughConnection sender, byte[] response) {
      this.sender = sender;
      this.response = response;
    }
  }
}