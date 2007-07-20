/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

/**
 * Sending this network message to the L2 signals that the L1 has successfully
 * started its JMX server and registered all of its beans, and can be connected
 * to and interrogated by the L2 server.
 */
public class L1JmxReady extends DSOMessageBase {

	public L1JmxReady(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out,
			MessageChannel channel, TCMessageType type) {
		super(sessionID, monitor, out, channel, type);
	}

	public L1JmxReady(SessionID sessionID, MessageMonitor monitor,
			MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
		super(sessionID, monitor, channel, header, data);
	}

}
