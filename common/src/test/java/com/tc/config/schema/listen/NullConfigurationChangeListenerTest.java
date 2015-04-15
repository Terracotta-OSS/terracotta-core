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
package com.tc.config.schema.listen;

import com.tc.config.schema.MockXmlObject;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link NullConfigurationChangeListener}.
 */
public class NullConfigurationChangeListenerTest extends TCTestCase {

  public void testAll() throws Exception {
    NullConfigurationChangeListener.getInstance().configurationChanged(null, null);
    NullConfigurationChangeListener.getInstance().configurationChanged(null, new MockXmlObject());
    NullConfigurationChangeListener.getInstance().configurationChanged(new MockXmlObject(), null);
    NullConfigurationChangeListener.getInstance().configurationChanged(new MockXmlObject(), new MockXmlObject());
  }

}
