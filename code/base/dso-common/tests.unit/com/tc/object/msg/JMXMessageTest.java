/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.test.TCTestCase;
import com.tc.util.NonPortableReason;

public class JMXMessageTest extends TCTestCase {

  private NonPortableReason createReason(byte b) {
    NonPortableReason reason = new NonPortableReason(JMXMessage.class, b);
    return reason;
  }

  public void testMessage() throws Exception {
    NonPortableReason reason = createReason(NonPortableReason.CLASS_NOT_ADAPTABLE);
    reason.setMessage(" Hello there\n");
    createMessageAndTest(reason);

    reason = createReason(NonPortableReason.SUPER_CLASS_NOT_ADAPTABLE);
    reason.addErroneousSuperClass(TCTestCase.class);
    createMessageAndTest(reason);

    reason = createReason(NonPortableReason.CLASS_NOT_IN_BOOT_JAR);
    createMessageAndTest(reason);

    reason = createReason(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED);
    reason.addErroneousSuperClass(Object.class);
    createMessageAndTest(reason);

    reason = createReason(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED);
    reason.addErroneousSuperClass(getClass());
    createMessageAndTest(reason);

    reason = createReason(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED);
    reason.addErroneousSuperClass(Object.class);
    reason.addErroneousSuperClass(getClass());
    createMessageAndTest(reason);

    reason = createReason(NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS);
    reason.addErroneousSuperClass(getClass());
    createMessageAndTest(reason);

    reason = createReason(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG);
    createMessageAndTest(reason);
  }

  private void createMessageAndTest(NonPortableReason reason) throws Exception {

    JMXMessage msg = new JMXMessage(new SessionID(0), new NullMessageMonitor(), new TCByteBufferOutputStream(4, 4096, false), null,
                                    TCMessageType.JMX_MESSAGE);
    msg.setJMXObject(reason);
    msg.dehydrate();
    JMXMessage msg2 = new JMXMessage(SessionID.NULL_ID, new NullMessageMonitor(), null, (TCMessageHeader) msg
        .getHeader(), msg.getPayload());
    msg2.hydrate();
    assertNonPortableReasonEquals(msg.getJMXObject(), msg2.getJMXObject());
  }

  private void assertNonPortableReasonEquals(Object object, Object object2) {
    assertTrue(object instanceof NonPortableReason);
    assertTrue(object2 instanceof NonPortableReason);
    NonPortableReason npr1 = (NonPortableReason) object;
    NonPortableReason npr2 = (NonPortableReason) object2;
    assertEquals(npr1.getClassName(), npr2.getClassName());
    assertEquals(npr1.getErroneousSuperClasses(), npr2.getErroneousSuperClasses());
    assertEquals(npr1.getReason(), npr2.getReason());
    assertEquals(npr1.getMessage(), npr2.getMessage());
    assertEquals(npr1.getDetailedReason(), npr2.getDetailedReason());
    System.out.println(npr1.getDetailedReason() + "\n");
  }

}
