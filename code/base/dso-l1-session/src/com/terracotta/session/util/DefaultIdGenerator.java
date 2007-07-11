/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.object.bytecode.Manager;
import com.terracotta.session.SessionId;

import java.security.SecureRandom;

/**
 * generates session cookie id of the format: <4hex:random><4hex:nextId>[[16hex:random]*]
 */
public class DefaultIdGenerator implements SessionIdGenerator {

  // NOTE: IMPORTANT!!! don't change MIN_LENGTH without reviewing generateKey method
  private static final int    MIN_LENGTH = 8;

  private static final char[] HEXCHARS   = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
      'C', 'D', 'E', 'F'                };

  private final SecureRandom  random;
  private final int           idLength;
  private final String        serverId;
  private final String        delimiter;
  private final int           lockType;
  private short               nextId     = Short.MIN_VALUE;

  public static SessionIdGenerator makeInstance(ConfigProperties cp, int lockType) {
    Assert.pre(cp != null);
    final int idLength = cp.getSessionIdLength();
    final String serverId = cp.getServerId();
    final String delimiter = cp.getDelimiter();
    return new DefaultIdGenerator(idLength, serverId, lockType, delimiter);
  }

  // for non-synchronous-write tests
  public DefaultIdGenerator(final int idLength, final String serverId) {
    this(idLength, serverId, Manager.LOCK_TYPE_WRITE, ConfigProperties.defaultDelimiter);
  }

  public DefaultIdGenerator(final int idLength, final String serverId, int lockType, String delimiter) {
    random = new SecureRandom();
    // init
    random.nextInt();

    this.lockType = lockType;
    this.idLength = Math.max(idLength, MIN_LENGTH);
    this.serverId = serverId;
    this.delimiter = delimiter;
  }

  public SessionId generateNewId() {
    final String key = generateKey();
    final String externalId = makeExternalId(key);
    return new DefaultSessionId(key, null, externalId, lockType, false);
  }

  public SessionId makeInstanceFromBrowserId(String requestedSessionId) {
    Assert.pre(requestedSessionId != null);
    final int dlmIndex = getDLMIndex(requestedSessionId);
    // everything before dlmIndex is key, everything after is serverId
    if (dlmIndex > 0) {
      final String key = requestedSessionId.substring(0, dlmIndex);
      final String requestedServerId = requestedSessionId.substring(dlmIndex + 1);
      final String externalId = makeExternalId(key);
      return new DefaultSessionId(key, requestedSessionId, externalId, lockType, !requestedServerId.equals(serverId));
    } else {
      // DLM is missing. someone is messing with our session ids!
      return null;
    }
  }

  protected int getDLMIndex(String requestedSessionId) {
    return requestedSessionId.indexOf(delimiter);
  }

  protected String makeExternalId(String key) {
    String externalId = key + delimiter + serverId;
    return externalId;
  }

  protected String getDelimiter() {
    return delimiter;
  }

  protected String getServerId() {
    return serverId;
  }

  protected int getLockType() {
    return lockType;
  }

  public SessionId makeInstanceFromInternalKey(String key) {
    final String externalId = makeExternalId(key);
    return new DefaultSessionId(key, externalId, externalId, lockType, false);
  }

  /**
   * NOTE: IMPORTANT!!! don't change MIN_LENGTH without reviewing generateKey method mininum size of generated string
   * must be 8 chars
   */
  protected synchronized String generateKey() {
    final byte[] bytes = new byte[2];
    final StringBuffer sb = new StringBuffer();
    // append 4hex:random
    random.nextBytes(bytes);
    sb.append(toHex(bytes, 2));

    // append 4hex:nextId
    toBytes(getNextId(), bytes);
    sb.append(toHex(bytes, 2));

    // append random bytes until we reach required length
    if (sb.length() < idLength) {
      final byte[] extraBytes = new byte[idLength - MIN_LENGTH];
      random.nextBytes(extraBytes);
      sb.append(toHex(extraBytes, extraBytes.length));
    }
    return sb.substring(0, idLength);
  }

  protected synchronized short getNextId() {
    return nextId++;
  }

  protected static void toBytes(short s, byte[] bytes) {
    bytes[0] = (byte) ((s & 0xff00) >> 8);
    bytes[1] = (byte) (s & 0x00ff);
  }

  protected static String toHex(byte[] bytes, int byteCnt) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < byteCnt; i++) {
      byte b = bytes[i];
      byte b1 = (byte) ((b & 0xf0) >> 4);
      byte b2 = (byte) (b & 0x0f);
      sb.append(HEXCHARS[b1]).append(HEXCHARS[b2]);
    }
    return sb.toString();
  }
}
