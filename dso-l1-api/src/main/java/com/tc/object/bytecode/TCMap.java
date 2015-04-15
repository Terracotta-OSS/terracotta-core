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
package com.tc.object.bytecode;

import java.util.Collection;

public interface TCMap {

  public void __tc_applicator_put(Object key, Object value);

  public void __tc_applicator_remove(Object key);

  public void __tc_applicator_clear();

  public void __tc_remove_logical(Object key);

  public void __tc_put_logical(Object key, Object value);

  public Collection __tc_getAllLocalEntriesSnapshot();

  public Collection __tc_getAllEntriesSnapshot();
}
