/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.AbstractTCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.delivery.OOOProtocolMessage;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.util.Assert;
import com.tc.util.Conversion;

/**
 * This class models the header portion of a TC wire protocol message. NOTE: This class makes no attempt to be thread
 * safe! All concurrent access must be syncronized
 * 
 * <pre>
 *        0                   1                   2                   3
 *        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |Version|   HL  |Type of Service|  Time to Live |    Protocol   |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                        Magic number                           |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                    32 Bit Total Length                        |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                Alder32 Header Checksum                        |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                       Source Address                          |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                    Destination Address                        |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |          Source Port          |      Destination Port         |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |          Message Count        |          Padding              |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |     Options                                |    Padding       |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author teck
 */

public class WireProtocolHeader extends AbstractTCNetworkHeader implements Cloneable {
  public static final byte    VERSION_1                    = 1;
  public static final byte    VERSION_2                    = 2;
  public static final byte[]  VALID_VERSIONS               = new byte[] { VERSION_1, VERSION_2 };

  public static final short   DEFAULT_TTL                  = 64;
  public static final int     MAX_MESSAGE_COUNT            = 0xFFFF;

  public static final short   PROTOCOL_UNKNOWN             = 0;
  public static final short   PROTOCOL_TCM                 = 1;
  public static final short   PROTOCOL_TRANSPORT_HANDSHAKE = 2;
  public static final short   PROTOCOL_OOOP                = 3;
  public static final short   PROTOCOL_HEALTHCHECK_PROBES  = 4;
  public static final short   PROTOCOL_MSGGROUP            = 5;

  private static final int    MAGIC_NUM                    = 0xAAAAAAAA;

  public static final short[] VALID_PROTOCOLS              = new short[] { PROTOCOL_TCM, PROTOCOL_TRANSPORT_HANDSHAKE,
      PROTOCOL_OOOP, PROTOCOL_HEALTHCHECK_PROBES, PROTOCOL_MSGGROUP };

  // 15 32-bit words max
  static final short          MAX_LENGTH                   = 15 * 4;

  // 8 32-bit words min
  static final short          MIN_LENGTH                   = 8 * 4;

  public static short getProtocolForMessageClass(TCNetworkMessage msg) {
    // TODO: is there a better way to do this (ie. not using instanceof)?
    if (msg instanceof TCMessage) {
      return PROTOCOL_TCM;
    } else if (msg instanceof OOOProtocolMessage) { return PROTOCOL_OOOP; }

    return PROTOCOL_UNKNOWN;
  }

  public WireProtocolHeader() {
    super(MIN_LENGTH, MAX_LENGTH);

    setMagicNum(MAGIC_NUM);
    setVersion(VERSION_2);
    setHeaderLength((byte) (MIN_LENGTH / 4));
    setTimeToLive(DEFAULT_TTL);
    setTypeOfService(TypeOfService.DEFAULT_TOS.getByteValue());
  }

  public WireProtocolHeader(TCByteBuffer buffer) {
    super(buffer, MIN_LENGTH, MAX_LENGTH);
  }

  private void setMagicNum(int magic_num2) {
    data.putInt(4, MAGIC_NUM);
  }

  public void setVersion(byte version) {
    if ((version <= 0) || (version > 15)) { throw new IllegalArgumentException("invalid version: " + version); }

    set4BitValue(0, true, version);
  }

  protected void setHeaderLength(short length) {
    if ((length < 6) || (length > 15)) { throw new IllegalArgumentException("Header length must in range 6-15"); }

    set4BitValue(0, false, (byte) length);
  }

  public void setTypeOfService(short tos) {
    data.putUbyte(1, tos);
  }

  public void setTimeToLive(short ttl) {
    data.putUbyte(2, ttl);
  }

  public void setProtocol(short protocol) {
    data.putUbyte(3, protocol);
  }

  public void setTotalPacketLength(int length) {
    data.putInt(8, length);
  }

  public void setSourceAddress(byte[] srcAddr) {
    data.put(16, srcAddr, 0, 4);
  }

  public void setDestinationAddress(byte[] destAddr) {
    data.put(20, destAddr, 0, 4);
  }

  public void setSourcePort(int srcPort) {
    data.putUshort(24, srcPort);
  }

  public void setDestinationPort(int dstPort) {
    data.putUshort(26, dstPort);
  }

  public void setMessageCount(int count) {
    Assert.eval(count <= MAX_MESSAGE_COUNT);
    data.putUshort(28, count);
  }

  public int getMessageCount() {
    return data.getUshort(28);
  }

  public int getMagicNum() {
    return data.getInt(4);
  }

  public byte getVersion() {
    return get4BitValue(0, true);
  }

  public byte getHeaderLength() {
    return get4BitValue(0, false);
  }

  public short getTypeOfService() {
    return data.getUbyte(1);
  }

  public short getTimeToLive() {
    return data.getUbyte(2);
  }

  public short getProtocol() {
    return data.getUbyte(3);
  }

  public int getTotalPacketLength() {
    return data.getInt(8);
  }

  public long getChecksum() {
    return data.getUint(12);
  }

  public byte[] getSourceAddress() {
    return getBytes(16, 4);
  }

  public byte[] getDestinationAddress() {
    return getBytes(20, 4);
  }

  public int getSourcePort() {
    return data.getUshort(24);
  }

  public int getDestinationPort() {
    return data.getUshort(26);
  }

  public void computeChecksum() {
    computeAdler32Checksum(12, true);
  }

  public boolean isChecksumValid() {
    return getChecksum() == computeAdler32Checksum(12, false);
  }

  public void validate() throws WireProtocolHeaderFormatException {
    // validate the magic num
    int magic = getMagicNum();
    if (magic != MAGIC_NUM) { throw new WireProtocolHeaderFormatException("Invalid magic number: " + magic + " != "
                                                                          + MAGIC_NUM); }

    // validate the version byte
    boolean validVersion = false;
    byte version = getVersion();

    for (int i = 0; i < VALID_VERSIONS.length; i++) {
      if (version == VALID_VERSIONS[i]) {
        validVersion = true;
        break;
      }
    }

    if (!validVersion) { throw new WireProtocolHeaderFormatException("Bad Version: " + Conversion.byte2uint(version)); }

    // TODO: validate the TOS byte

    // validate the TTL byte
    int ttl = getTimeToLive();
    if (0 == ttl) { throw new WireProtocolHeaderFormatException("TTL byte cannot be equal to zero"); }

    // validate the protocol byte
    boolean validProtocol = false;
    short protocol = getProtocol();

    for (int i = 0; i < VALID_PROTOCOLS.length; i++) {
      if (protocol == VALID_PROTOCOLS[i]) {
        validProtocol = true;
        break;
      }
    }

    if (!validProtocol) { throw new WireProtocolHeaderFormatException("Bad Protocol byte: " + protocol); }

    // validate the total packet length value
    int totalLength = getTotalPacketLength();

    if (totalLength < MIN_LENGTH) { throw new WireProtocolHeaderFormatException(
                                                                                "Total length ("
                                                                                    + totalLength
                                                                                    + ") can not be less than minimum header size ("
                                                                                    + MIN_LENGTH + ")"); }

    if (totalLength < getHeaderByteLength()) { throw new WireProtocolHeaderFormatException(
                                                                                           "Total length ("
                                                                                               + totalLength
                                                                                               + ") can not be less than actual header length ("
                                                                                               + getHeaderByteLength()
                                                                                               + ")"); }

    // validate the checksum
    if (!isChecksumValid()) { throw new WireProtocolHeaderFormatException("Invalid Checksum"); }

    if (getSourcePort() == 0) { throw new WireProtocolHeaderFormatException("Source port cannot be zero"); }

    if (getDestinationPort() == 0) { throw new WireProtocolHeaderFormatException("Destination port cannot be zero"); }

    // if (Arrays.equals(getDestinationAddress(), FOUR_ZERO_BYTES)) { throw new WireProtocolHeaderFormatException(
    // "Destination address cannot be 0.0.0.0"); }
    //
    // if (Arrays.equals(getSourceAddress(), FOUR_ZERO_BYTES)) { throw new WireProtocolHeaderFormatException(
    // "Source address cannot be 0.0.0.0"); }

    // TODO: validate options (once they exist)
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Version: ").append(Conversion.byte2uint(getVersion())).append(", ");
    buf.append("Header Length: ").append(Conversion.byte2uint(getHeaderLength())).append(", ");
    buf.append("TOS: ").append(getTypeOfService()).append(", ");
    buf.append("TTL: ").append(getTimeToLive()).append(", ");
    buf.append("Protocol: ").append(getProtocolString());
    buf.append("\n");
    buf.append("Total Packet Length: ").append(getTotalPacketLength()).append("\n");
    buf.append("Adler32 Checksum: ").append(getChecksum()).append(" (valid: ").append(isChecksumValid()).append(")\n");
    buf.append("Source Addresss: ");

    byte src[] = getSourceAddress();
    byte dest[] = getDestinationAddress();

    for (int i = 0; i < src.length; i++) {
      buf.append(Conversion.byte2uint(src[i]));
      if (i != (src.length - 1)) {
        buf.append(".");
      }
    }
    buf.append("\n");

    buf.append("Destination Addresss: ");
    for (int i = 0; i < dest.length; i++) {
      buf.append(Conversion.byte2uint(dest[i]));
      if (i != (dest.length - 1)) {
        buf.append(".");
      }
    }
    buf.append("\n");

    buf.append("Source Port: ").append(getSourcePort());
    buf.append(", Destination Port: ").append(getDestinationPort());
    buf.append("\n");

    buf.append("Total Msg Count: " + getMessageCount());
    buf.append("\n");

    String errMsg = "no message";
    boolean valid = true;
    try {
      validate();
    } catch (WireProtocolHeaderFormatException e) {
      errMsg = e.getMessage();
      valid = false;
    }
    buf.append("Header Validity: ").append(valid).append(" (").append(errMsg).append(")\n");

    // TODO: display the options (if any)

    return buf.toString();
  }

  private String getProtocolString() {
    final short protocol = getProtocol();
    switch (protocol) {
      case PROTOCOL_TCM: {
        return "TCM";
      }
      case PROTOCOL_OOOP: {
        return "OOOP";
      }
      case PROTOCOL_HEALTHCHECK_PROBES: {
        return "HEALTHCHECK_PROBES";
      }
      case PROTOCOL_TRANSPORT_HANDSHAKE: {
        return "TRANSPORT HANDSHAKE";
      }
      case PROTOCOL_MSGGROUP: {
        return "TRANSPORT MSGGROUP";
      }
      default: {
        return "UNKNOWN (" + protocol + ")";
      }
    }
  }

  public int getMaxByteLength() {
    return WireProtocolHeader.MAX_LENGTH;
  }

  public int getMinByteLength() {
    return WireProtocolHeader.MIN_LENGTH;
  }

  public int getHeaderByteLength() {
    return 4 * getHeaderLength();
  }

  public boolean isHandshakeOrHealthCheckMessage() {
    final short proto = getProtocol();
    return proto == PROTOCOL_TRANSPORT_HANDSHAKE || proto == PROTOCOL_HEALTHCHECK_PROBES;

  }

  public boolean isMessagesGrouped() {
    return PROTOCOL_MSGGROUP == getProtocol();
  }

  @Override
  protected Object clone() {
    WireProtocolHeader rv = new WireProtocolHeader();
    rv.setVersion(this.getVersion());
    rv.setHeaderLength(this.getHeaderLength());
    rv.setTypeOfService(this.getTypeOfService());
    rv.setTimeToLive(this.getTimeToLive());
    rv.setProtocol(this.getProtocol());

    rv.setMagicNum(this.getMagicNum());
    rv.setTotalPacketLength(this.getTotalPacketLength());
    rv.computeChecksum();
    rv.setSourceAddress(this.getSourceAddress());
    rv.setDestinationAddress(this.getDestinationAddress());
    rv.setSourcePort(this.getSourcePort());
    rv.setDestinationPort(this.getDestinationPort());
    rv.setMessageCount(this.getMessageCount());
    return rv;
  }

}
