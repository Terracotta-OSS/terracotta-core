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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.terracotta.entity.EntityMessage;


/**
 * This is a heavily simplified retirement manager which would have to be replaced/extended if we were to switch to a
 * multi-threaded passthrough server.
 * Additionally, it is currently very coarse:  on deferred message will block ALL retirement, not just the retirement on its
 * concurrency key.
 */
public class PassthroughRetirementManager {
  // NOTE:  This implementation assumes a single-threaded server so we check that the given thread matches.
  private Thread currentServerThread;

  // This implementation is VERY simple and makes a few corresponding assumptions about how it is being used:
  // -only one message is being run at any time
  // -a message can only defer to a SINGLE other message
  // -it is acceptable to treat the logical ordering constraints as global, instead of just within a key

  // The list of blocked tuples.  These represent the "global logical ordering" of retirement.
  private final List<RetirementTuple> blockedTuples;
  // The messages which are still blocking _some_ tuple in the blockedTuples list.
  private final Set<EntityMessage> blockingMessages;

  // Uses the assumption that only 1 message is running at a time to not need to store the dependency relationship, but just
  // the message the currently executing message must block on, for retirement.  When the message comes to retire, if this
  // is non-null, then it implies that the message should be blocked.
  private EntityMessage blockCurrentMessageOn;


  public PassthroughRetirementManager() {
    this.blockedTuples = new Vector<RetirementTuple>();
    this.blockingMessages = new HashSet<EntityMessage>();
  }

  /**
   * Sets the server thread the receiver can expect to see making all calls.  This is how the single-threaded assumption is
   * enforced within the code.
   * Any previous value is over-written as the receiver may be re-used, despite a server process starting and stopping.
   * 
   * @param serverThread The server thread which will now be operating the receiver
   */
  public void setServerThread(Thread serverThread) {
    this.currentServerThread = serverThread;
  }

  /**
   * Called to flag the currently executing message as one which must defer its retirement until the completion of the given
   * blockedOn message.
   * Note that this can only be called once for the currently executing message.
   * 
   * @param blockedOn The message on which the currently executing message must block its retirement
   */
  public void deferCurrentMessage(EntityMessage blockedOn) {
    Assert.assertTrue(Thread.currentThread() == this.currentServerThread);
    Assert.assertTrue(null == this.blockCurrentMessageOn);
    this.blockCurrentMessageOn = blockedOn;
  }

  /**
   * Called to state that a message has completed execution and would like to retire.  This usually just returns the tuple
   * given, but may return 0, if there is a blockage, or multiple, if this resulted in unblocking part of the list.
   * 
   * @param completedInternalOrNull The completed message or null, if the message wasn't visible (this is ONLY non-null in
   * the cases where it was a message which blocked someone).
   * @param tuple The tuple describing the retirement operation ready to run
   * @return A list of any unblocked retirement operations, in the order they must be run
   */
  public List<RetirementTuple> retireableListAfterMessageDone(EntityMessage completedInternalOrNull, RetirementTuple tuple) {
    Assert.assertTrue(Thread.currentThread() == this.currentServerThread);
    // Note that the message can be null if it isn't one which could unblock anything (only internally-created messages can
    // unblock).
    if (null != completedInternalOrNull) {
      boolean didRemove = this.blockingMessages.remove(completedInternalOrNull);
      Assert.assertTrue(didRemove);
    }
    boolean didBlockTuple = false;
    if (null != this.blockCurrentMessageOn) {
      // We want to block tuple.
      // We assume that the completed was null, in this case.
      // (NOTE:  This might not be true if this is a "skip-stone" but we currently have no such use-case so this is useful
      // debugging).
      Assert.assertTrue(null == completedInternalOrNull);
      boolean didAdd = this.blockingMessages.add(this.blockCurrentMessageOn);
      Assert.assertTrue(didAdd);
      tuple.blockedOn = this.blockCurrentMessageOn;
      this.blockedTuples.add(tuple);
      didBlockTuple = true;
      this.blockCurrentMessageOn = null;
    }
    
    // Now, determine if anything is good to be retired.
    List<RetirementTuple> readyToRetire = new Vector<RetirementTuple>();
    while ((this.blockedTuples.size() > 0)
        && ((null == this.blockedTuples.get(0).blockedOn) || !this.blockingMessages.contains(this.blockedTuples.get(0).blockedOn))) {
      RetirementTuple readyTuple = this.blockedTuples.remove(0);
      readyToRetire.add(readyTuple);
    }
    
    // Now, if there is anything left on the list and we didn't already add this, block it.
    if ((this.blockedTuples.size() > 0) && !didBlockTuple) {
      this.blockedTuples.add(tuple);
      didBlockTuple = true;
    }
    // (otherwise, it can be returned, as well).
    if (!didBlockTuple) {
      readyToRetire.add(tuple);
    }
    return readyToRetire;
  }

  /**
   * Contains the basic data related to a frozen retirement operation.
   */
  public static class RetirementTuple {
    // This blockedOn field is only set if we are put into the blocked list.
    public EntityMessage blockedOn;
    public final PassthroughConnection sender;
    public final byte[] response;
    
    public RetirementTuple(PassthroughConnection sender, byte[] response) {
      this.sender = sender;
      this.response = response;
    }
  }
}