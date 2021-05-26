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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.l2.L2DebugLogging;
import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

public class DiscoveryStateMachine implements ChannelEventListener {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryStateMachine.class);
  private final static long                        DISCOVERY_INTERVAL_MS;
  static {
    DISCOVERY_INTERVAL_MS = TCPropertiesImpl.getProperties()
        .getLong(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_DISCOVERY_INTERVAL);
  }
  private final DiscoveryState STATE_INIT            = new InitState();
  private final DiscoveryState STATE_CONNECTING      = new ConnectingState();
  private final DiscoveryState STATE_CONNECTED       = new ConnectedState();
  private final DiscoveryState STATE_CONNECT_TIMEOUT = new ConnectTimeoutState();
  private final DiscoveryState STATE_MAX_CONNECTION  = new MaxConnExceedState();
  private final DiscoveryState STATE_STACK_MISMATCH  = new CommStackMismatchState();
  private final DiscoveryState STATE_IO_EXCEPTION    = new IOExceptionState();
  private final DiscoveryState STATE_THROWABLE_EXCEP = new ThrowableExceptionState();
  private final DiscoveryState STATE_UNKNOWN_HOST    = new UnknownHostState();
  private final DiscoveryState STATE_MEMBER_IN_GROUP = new MemberInGroupState();

  private DiscoveryState       current;
  private DiscoveryState       previousBadState;

  private final Node           node;
  private int                  badCount;
  private long                 timestamp;
  private long                 previousLogTimeStamp;
  private MessageChannel       connectedChannel;

  public DiscoveryStateMachine(Node node) {
    this.node = node;
  }

  // reduce logging to about once per min
  public void loggerWarn(String message) {
    if (System.currentTimeMillis() > (previousLogTimeStamp + 60000)) {
      logger.warn(message);
      previousLogTimeStamp = System.currentTimeMillis();
    }
  }

  public final void start() {
    switchToState(initialState());
  }

  protected DiscoveryState initialState() {
    return (STATE_INIT);
  }

  protected synchronized void switchToState(DiscoveryState state) {
    Assert.assertNotNull(state);
    debugInfo("DiscoverStateMachine [" + node + "]: switching to state: " + state);
    this.current = state;
    state.enter();
  }

  protected synchronized boolean switchToStateFrom(DiscoveryState from, DiscoveryState to) {
    Assert.assertNotNull(from);
    Assert.assertNotNull(to);
    if (this.current == from) {
      this.current = to;
      to.enter();
      return true;
    } else {
      logger.warn("DiscoverStateMachine [" + node + "]: Ignored switching state from: " + from + ", to: " + to
                  + ", current: " + current);
      return false;
    }
  }

  Node getNode() {
    return node;
  }

  synchronized boolean isMemberInGroup() {
    return (current == STATE_MEMBER_IN_GROUP);
  }

  synchronized boolean isTimeToConnect() {
    return current.isTimeToConnect();
  }

  void connecting() {
    Assert.eval(current != STATE_CONNECTING);
    switchToState(STATE_CONNECTING);
  }

  void connected() {
    switchToStateFrom(STATE_CONNECTING, STATE_CONNECTED);
  }

  private void notifyDisconnected() {
    if (!switchToStateFrom(STATE_CONNECTING, STATE_INIT)) {
      switchToStateFrom(STATE_CONNECTED, STATE_INIT);
    }
  }

  synchronized void badConnect(DiscoveryState state) {
    if (current == STATE_MEMBER_IN_GROUP) { return; }
    switchToState(state);
  }

  void connectTimeout() {
    badConnect(STATE_CONNECT_TIMEOUT);
  }

  void maxConnExceed() {
    badConnect(STATE_MAX_CONNECTION);
  }

  void commStackMismatch() {
    badConnect(STATE_STACK_MISMATCH);
  }

  void connetIOException() {
    badConnect(STATE_IO_EXCEPTION);
  }

  void throwableException() {
    badConnect(STATE_THROWABLE_EXCEP);
  }

  synchronized void unknownHost() {
    if (current == STATE_UNKNOWN_HOST) { return; }
    if (current == STATE_MEMBER_IN_GROUP) { return; }
    switchToState(STATE_UNKNOWN_HOST);
  }

  synchronized void nodeJoined() {
    switchToState(STATE_MEMBER_IN_GROUP);
  }

  synchronized void nodeLeft() {
    switchToState(STATE_INIT);
  }

  /*
   * DiscoveryState -- base class for member discovery state
   */
  private abstract class DiscoveryState {
    private final String name;

    public DiscoveryState(String name) {
      this.name = name;
    }

    public void enter() {
      badCount = 0;
      previousBadState = null;
      previousLogTimeStamp = 0;
    }

    public boolean isTimeToConnect() {
      // override me if you want
      return true;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /*
   * InitState --
   */
  private class InitState extends DiscoveryState {
    public InitState() {
      super("Init");
    }

    @Override
    public void enter() {
      // do nothing
    }

    @Override
    public boolean isTimeToConnect() {
      return true;
    }
  }

  /*
   * ConnectingState --
   */
  private class ConnectingState extends DiscoveryState {
    public ConnectingState() {
      super("Connecting");
    }

    @Override
    public void enter() {
      // do nothing
    }

    @Override
    public boolean isTimeToConnect() {
      return false;
    }
  }

  /*
   * ConnectedState --
   */
  private class ConnectedState extends DiscoveryState {
    public ConnectedState() {
      super("Connected");
    }

    @Override
    public boolean isTimeToConnect() {
      return false;
    }
  }

  /*
   * BadState -- abstract bad connection
   */
  private abstract class BadState extends DiscoveryState {
    public BadState(String name) {
      super(name);
    }

    @Override
    public void enter() {
      if ((previousBadState == null) || (previousBadState != current)) {
        badCount = 0;
        previousBadState = current;
        previousLogTimeStamp = 0;
      } else {
        ++badCount;
      }
    }

    @Override
    public boolean isTimeToConnect() {
      // check 60 times then every min
      if (badCount < 60) { return true; }
      if (System.currentTimeMillis() > (timestamp + DISCOVERY_INTERVAL_MS * 60)) {
        timestamp = System.currentTimeMillis();
        return true;
      } else {
        return false;
      }
    }
  }

  /*
   * VeryBadState -- abstract very bad connection, no more attempts to connect
   */
  private abstract class VeryBadState extends DiscoveryState {
    public VeryBadState(String name) {
      super(name);
    }

    @Override
    public boolean isTimeToConnect() {
      return false;
    }
  }

  /*
   * ConnetTimeoutState --
   */
  private class ConnectTimeoutState extends BadState {
    public ConnectTimeoutState() {
      super("Connection-Timeouted");
    }
  }

  /*
   * MaxConnExceedState --
   */
  private class MaxConnExceedState extends BadState {
    public MaxConnExceedState() {
      super("Max-Connections-Exceed");
    }
  }

  /*
   * CommStackMismatchState --
   */
  private class CommStackMismatchState extends VeryBadState {
    public CommStackMismatchState() {
      super("Comm-Stack-Mismatch");
    }
  }

  /*
   * IOExceptionState --
   */
  private class IOExceptionState extends BadState {
    public IOExceptionState() {
      super("IO-Exception");
    }
  }

  /*
   * ThrowableExceptionState --
   */
  private class ThrowableExceptionState extends BadState {
    public ThrowableExceptionState() {
      super("Connection-Throwable");
    }
  }

  /*
   * UnknowHostState --
   */
  private class UnknownHostState extends DiscoveryState {
    public UnknownHostState() {
      super("Unknown-Host");
    }

    @Override
    public void enter() {
      if ((previousBadState == null) || (previousBadState != current)) {
        super.enter();
        timestamp = System.currentTimeMillis();
      }
    }

    @Override
    public boolean isTimeToConnect() {
      // check every 5 min
      if (System.currentTimeMillis() > (timestamp + 1000 * 60 * 5)) {
        timestamp = System.currentTimeMillis();
        return true;
      } else {
        return false;
      }
    }
  }

  /*
   * MemberInGroup -- A valid connection established
   */
  private class MemberInGroupState extends DiscoveryState {
    public MemberInGroupState() {
      super("Member-In-Group");
    }

    @Override
    public boolean isTimeToConnect() {
      return false;
    }
  }

  @Override
  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT ||
        event.getType() == ChannelEventType.CHANNEL_OPENED_EVENT) {
      synchronized (this) {
        this.connectedChannel = event.getChannel();
      }
    } else if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
               || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
      synchronized (this) {
        if (canNotifyDisconnect(event)) {
          notifyDisconnected();
          this.connectedChannel = null;
        }
      }
    }
  }

  private synchronized boolean canNotifyDisconnect(ChannelEvent event) {
    MessageChannel eventChannel = event.getChannel();
    boolean rv = (this.connectedChannel == null) ? false
        : ((this.connectedChannel == eventChannel)
           && (this.connectedChannel.getLocalAddress() == eventChannel.getLocalAddress()) && (this.connectedChannel
            .getRemoteAddress() == eventChannel.getRemoteAddress()));
    return rv;
  }

  private static void debugInfo(String message) {
    L2DebugLogging.log(logger, LogLevel.INFO, message, null);
  }
}
