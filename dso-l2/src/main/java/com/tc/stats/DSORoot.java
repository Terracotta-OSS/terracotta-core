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
package com.tc.stats;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.ObjectID;
import com.tc.stats.api.DSORootMBean;

import javax.management.NotCompliantMBeanException;

public class DSORoot extends AbstractTerracottaMBean implements DSORootMBean {
  private final ObjectID           objectID;
  private final String             rootName;

  public DSORoot(ObjectID rootID, String name) throws NotCompliantMBeanException {
    super(DSORootMBean.class, false);

    this.objectID = rootID;
    this.rootName = name;
  }

  @Override
  public String getRootName() {
    return this.rootName;
  }

  @Override
  public ObjectID getObjectID() {
    return objectID;
  }

  @Override
  public void reset() {
    /**/
  }
}
