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
package com.tc.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TraversedReferencesImpl implements TraversedReferences {

  private final Collection<TraversedReference> references = new ArrayList<TraversedReference>();
  
  @Override
  public void addAnonymousReference(Object o) {
    references.add(new AnonymousTraversedReference(o));
  }
  
  @Override
  public void addNamedReference(String className, String fieldName, Object value) {
    references.add(new NamedTraversedReference(className, fieldName, value));
  }

  @Override
  public void addNamedReference(String fullyQualifiedFieldName, Object value) {
    references.add(new NamedTraversedReference(fullyQualifiedFieldName, value));
  }

  @Override
  public Iterator<TraversedReference> iterator() {
    return references.iterator();
  }

}
