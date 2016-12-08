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
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

import java.io.IOException;

public class StripeIDMismatchGroupMessage extends AbstractGroupMessage {

  public static final int ERROR_STRIPEID_MISMATCH  = 0;
  public static final int ERROR_NOT_CLUSTER_MEMBER = 1;
  public static final int ERROR_MISMATCH_STRIPEID  = 2;
  public static final int ERROR_MISMATCH_GROUPID   = 3;
  public static final int MISMATCH_TEMPORARY       = 4;
  public static final int MISMATCH_NOT_READY_YET   = 5;

  private int             errorType;
  private String          reason;

  // To make serialization happy
  public StripeIDMismatchGroupMessage() {
    super(-1);
  }

  public StripeIDMismatchGroupMessage(int type, int errorType, String reason) {
    super(type);
    this.reason = reason;
    this.errorType = errorType;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    errorType = in.readInt();
    reason = in.readString();
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    out.writeInt(errorType);
    out.writeString(reason);
  }

  @Override
  public String toString() {
    return "StripeIDMismatchGroupMessage [ " + errorType + " , " + reason + " ]";
  }

  public String getReason() {
    return reason;
  }

  public int getErrorType() {
    return errorType;
  }

}
