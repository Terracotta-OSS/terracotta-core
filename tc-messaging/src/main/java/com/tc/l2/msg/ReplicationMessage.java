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
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ReplicationMessage extends AbstractGroupMessage implements OrderedEventContext {
  // We don't have an explicit message type - the ReplicationMessage is only a container.
  public static final int IGNORED = 0;

  // Factory methods.
  public static ReplicationMessage createActivityContainer(SyncReplicationActivity activity) {
    Assert.assertNotNull(activity);
    return new ReplicationMessage(activity);
  }

  public static ReplicationMessage createLocalContainer(SyncReplicationActivity activity) {
    Assert.assertNotNull(activity);
    ReplicationMessage message = new ReplicationMessage(activity);
    message.didCreateLocally = false;
    return message;
  }


  private List<SyncReplicationActivity> activities;
  long rid = 0;
  // We will keep a flag to track whether this message is outgoing (created here and being sent to the network) or incoming
  //  (created elsewhere and decoded here) to ensure that it is being used correctly.
  // (Note that this check can be removed in the future - it is mostly to validate during refactoring and buffering
  //  implementation).
  private boolean didCreateLocally;
  
  public ReplicationMessage() {
    super(IGNORED);
    this.didCreateLocally = false;
  }
  
  protected ReplicationMessage(int type) {
    super(type);
    this.didCreateLocally = false;
  }
  
//  a true replicated message
  private ReplicationMessage(SyncReplicationActivity activity) {
    super(IGNORED);
    this.activities = new ArrayList<SyncReplicationActivity>();
    this.activities.add(activity);
    this.didCreateLocally = true;
  }
  
  public void setReplicationID(long rid) {
    this.rid = rid;
  }

  @Override
  public long getSequenceID() {
    return rid;
  }

  /**
   * Adds the given activity to the current batch, returning the new size of the batch.
   * 
   * @param activity The activity to add to the batch.
   * @return The number of activities now in the message batch.
   */
  public int addActivity(SyncReplicationActivity activity) {
    this.activities.add(activity);
    return this.activities.size();
  }

  public List<SyncReplicationActivity> getActivities() {
    // If this was created locally, we shouldn't be reaching into it to read the underlying activity - this is for the
    //  receiving side, only.
    Assert.assertFalse(this.didCreateLocally);
    return this.activities;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    int messageType = getType();
    switch (messageType) {
      case IGNORED:
        this.rid = in.readLong();
        int batchSize = in.readInt();
        // We don't send empty batches.
        Assert.assertTrue(batchSize > 0);
        this.activities = new ArrayList<SyncReplicationActivity>();
        for (int i = 0; i < batchSize; ++i) {
          SyncReplicationActivity activity = SyncReplicationActivity.deserializeFrom(in);
          Assert.assertNotNull(activity);
          this.activities.add(activity);
        }
        // Make sure that the message type and activity type are consistent.
        break;
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    int messageType = getType();
    switch (messageType) {
      case IGNORED:
        out.writeLong(rid);
        int batchSize = this.activities.size();
        Assert.assertTrue(batchSize > 0);
        out.writeInt(batchSize);
        for (SyncReplicationActivity activity : this.activities) {
          activity.serializeTo(out);
        }
        break;
    }
  }
  
  public String getDebugId() {
    return this.getType() + " " + ((this.activities != null) ? (this.activities.size() + " activities") : "no activities");
  }

  @Override
  public String toString() {
    return "ReplicationMessage{rid=" + rid + ", " + ((this.activities != null) ? (this.activities.size() + " activities") : "no activities") + "}";
  }
}
