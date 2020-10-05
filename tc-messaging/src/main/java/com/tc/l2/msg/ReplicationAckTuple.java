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


/**
 * Elements used in the ReplicationMessageAck batch.
 * The respondTo is the message to which we are responding.  The result determines if this is a RECEIVED, SUCCESS, or FAIL.
 */
public class ReplicationAckTuple {
  public final SyncReplicationActivity.ActivityID respondTo;
  public final ReplicationResultCode result;
  
  public ReplicationAckTuple(SyncReplicationActivity.ActivityID respondTo, ReplicationResultCode result) {
    this.respondTo = respondTo;
    this.result = result;
  }
}
