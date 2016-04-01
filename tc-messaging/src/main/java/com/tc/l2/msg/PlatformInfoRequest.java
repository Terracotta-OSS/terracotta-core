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

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import java.io.IOException;

/**
 *
 */
public class PlatformInfoRequest extends AbstractGroupMessage {
//  message types  
  public static final int REQUEST               = 0; 
  public static final int SERVER_INFO               = 1; 
  public static final int SERVER_STATE               = 2; 
  
  private RequestType requestType;
  
  private String name;
  private String version;
  private String build;
  
  private String state;
    
  private long startTime;
  private long activateTime;
  
  public enum RequestType {
    SERVER_INFO;
  }

  public PlatformInfoRequest() {
    this(REQUEST);
  }
  
  public PlatformInfoRequest(int type) {
    super(type);
  }
  
  public PlatformInfoRequest(String state, long activate, MessageID requestID) {
    this(SERVER_STATE, requestID);
    this.state = state;
    this.activateTime = activate;
  }  
  
  public PlatformInfoRequest(String name, String version, String buildid, long startTime, MessageID requestID) {
    this(SERVER_INFO, requestID);
    this.name = name;
    this.build = buildid;
    this.version = version;
    this.startTime = startTime;
  }

  private PlatformInfoRequest(int type, MessageID requestID) {
    super(type, requestID);
  }
  
  public void setRequestType(RequestType type) {
    requestType = type;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public String getVersion() {
    return version;
  }

  public String getBuild() {
    return build;
  }
  
  public String getName() {
    return name;
  }

  public String getState() {
    return state;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getActivateTime() {
    return activateTime;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    if (getType() == REQUEST) {
      requestType = RequestType.values()[in.readInt()];
    } else if (getType() == SERVER_INFO) {
      this.name = in.readString();
      this.version = in.readString();
      this.build = in.readString();
      this.startTime = in.readLong();
    } else if (getType() == SERVER_STATE) {
      this.state = in.readString();
      this.activateTime = in.readLong();
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    if (getType() == REQUEST) {
      if (requestType != null) {
        out.writeInt(requestType.ordinal());
      } else {
        out.writeInt(-1);
      }
    } else if (getType() == SERVER_INFO) {
      out.writeString(name);
      out.writeString(version);
      out.writeString(build);
      out.writeLong(startTime);
    } else if (getType() == SERVER_STATE) {
      out.writeString(state);
      out.writeLong(activateTime);
    }
  }
}
