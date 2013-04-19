package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author Eugene Shelestovich
 */
public class EvictionInterestMessageImpl extends DSOMessageBase implements EvictionInterestMessage {

  private final static DNAEncoding encoder = new SerializerDNAEncodingImpl();
  private final static DNAEncoding decoder = new StorageDNAEncodingImpl();

  private static final byte KEY_ID = 0;
  private Object key;

  public EvictionInterestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final TCByteBufferOutputStream out, final MessageChannel channel,
                                     final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public EvictionInterestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final MessageChannel channel, final TCMessageHeader header,
                                     final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(KEY_ID, true);
    encoder.encode(key, getOutputStream());
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case KEY_ID:
        try {
          getBooleanValue();
          key = decoder.decode(getInputStream());
        } catch (ClassNotFoundException e) {
          return false;
        }
        return true;
      default:
        return false;
    }
  }

  @Override
  public Object getKey() {
    return key;
  }

  @Override
  public void setKey(final Object key) {
    this.key = key;
  }
}
