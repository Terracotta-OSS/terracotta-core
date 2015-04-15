/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  public CustomLifespanSerializedEntryApplicator(final DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public void dehydrate(final ClientObjectManager objectManager, final TCObject tco, final DNAWriter writer,
                        final Object pojo) {
    super.dehydrate(objectManager, tco, writer, pojo);
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
        throw new AssertionError("Unknown physical action: " + a);
      }
    }
  }

}
