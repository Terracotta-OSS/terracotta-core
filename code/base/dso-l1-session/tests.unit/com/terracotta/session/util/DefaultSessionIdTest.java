/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import junit.framework.TestCase;

public class DefaultSessionIdTest extends TestCase {

  public final void testConstruction() {
    final String id = "SomeSessionId";
    DefaultSessionId sid = new DefaultSessionId(id, null, id);
    assertNull(sid.getRequestedId());
    assertEquals(id, sid.getKey());
    assertEquals(id, sid.getExternalId());
    assertFalse(sid.isServerHop());
    assertTrue(sid.isNew());

    sid = new DefaultSessionId(id, id, id);
    assertEquals(id, sid.getRequestedId());
    assertEquals(id, sid.getExternalId());
    assertEquals(id, sid.getKey());
    assertFalse(sid.isServerHop());
    assertFalse(sid.isNew());

    sid = new DefaultSessionId(id, id + id, id);
    assertEquals(id + id, sid.getRequestedId());
    assertEquals(id, sid.getExternalId());
    assertEquals(id, sid.getKey());
    assertTrue(sid.isServerHop());
    assertFalse(sid.isNew());
  }
}
