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


public class PlatformInfoRequest extends AbstractGroupMessage {
  // Factory methods.
  /**
   * Called by the active when a new passive joins the cluster or when an active is selected, in order to ask all the
   *  passives for their information.
   * 
   * @param type Always SERVER_INFO, for now.
   * @return The message instance.
   */
  public static PlatformInfoRequest createEmptyRequest(RequestType type) {
    PlatformInfoRequest request = new PlatformInfoRequest(REQUEST);
    request.setRequestType(type);
    return request;
  }

  /**
   * Called by a server to create a message to send back another server which has requested its state.
   * 
   * @param state The server state name.
   * @param active The millisecond time when the server became active.
   * @param requestID The ID of the message which requested the state.
   * @return The message instance.
   */
  public static PlatformInfoRequest createServerStateMessage(String state, long activate, MessageID requestID) {
    return new PlatformInfoRequest(state, activate, requestID);
  }

  /**
   * Called by a server to create a message to send back another server which has requested its info.
   * 
   * @param name The name of the server.
   * @param version The version of the server.
   * @param buildid The build ID of the server.
   * @param startTime The millisecond time when the server came online.
   * @param requestID The ID of the message which requested the state.
   * @return The message instance.
   */
  public static PlatformInfoRequest createServerInfoMessage(String name, String version, String buildid, long startTime, MessageID requestID) {
    return new PlatformInfoRequest(name, version, buildid, startTime, requestID);
  }


//  message types  
  public static final int ERROR               = -1; 
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


  // Must be public for serialization initializer.
  public PlatformInfoRequest() {
    this(ERROR);
  }
  
  private PlatformInfoRequest(int type) {
    super(type);
  }
  
  private PlatformInfoRequest(String state, long activate, MessageID requestID) {
    this(SERVER_STATE, requestID);
    this.state = state;
    this.activateTime = activate;
  }  
  
  private PlatformInfoRequest(String name, String version, String buildid, long startTime, MessageID requestID) {
    this(SERVER_INFO, requestID);
    this.name = name;
    this.build = buildid;
    this.version = version;
    this.startTime = startTime;
  }

  private PlatformInfoRequest(int type, MessageID requestID) {
    super(type, requestID);
  }
  
  private void setRequestType(RequestType type) {
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
