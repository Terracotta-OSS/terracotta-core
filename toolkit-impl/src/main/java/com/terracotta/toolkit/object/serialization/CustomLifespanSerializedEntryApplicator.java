/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.object.serialization;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Chris Dennis
 */
public class CustomLifespanSerializedEntryApplicator extends SerializedMapValueApplicator {

  public static final String CUSTOM_TTI_FIELD = "customTti";
  public static final String CUSTOM_TTL_FIELD = "customTtl";

  public CustomLifespanSerializedEntryApplicator(final DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public void dehydrate(final ClientObjectManager objectManager, final TCObject tco, final DNAWriter writer,
                        final Object pojo) {
    super.dehydrate(objectManager, tco, writer, pojo);

    CustomLifespanSerializedMapValue entry = (CustomLifespanSerializedMapValue) pojo;
    writer.addPhysicalAction(CUSTOM_TTI_FIELD, entry.internalGetCustomTti());
    writer.addPhysicalAction(CUSTOM_TTL_FIELD, entry.internalGetCustomTtl());
  }

  @Override
  public void hydrate(final ClientObjectManager objectManager, final TCObject tco, final DNA dna, final Object pojo)
      throws IOException, ClassNotFoundException {
    CustomLifespanSerializedMapValue<Serializable> se = (CustomLifespanSerializedMapValue<Serializable>) pojo;

    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      if (a.isEntireArray()) {
        se.internalSetValue((byte[]) a.getObject());
      } else {
        if (CREATE_TIME_FIELD_NAME.equals(a.getFieldName())) {
          se.internalSetCreateTime((Integer) a.getObject());
        } else if (LAST_ACCESS_TIME_FIELD_NAME.equals(a.getFieldName())) {
          se.internalSetLastAccessedTime((Integer) a.getObject());
        } else if (CUSTOM_TTI_FIELD.equals(a.getFieldName())) {
          se.internalSetCustomTti((Integer) a.getObject());
        } else if (CUSTOM_TTL_FIELD.equals(a.getFieldName())) {
          se.internalSetCustomTtl((Integer) a.getObject());
        } else {
          throw new AssertionError("Unknown physical action: " + a);
        }
      }
    }
  }

}
