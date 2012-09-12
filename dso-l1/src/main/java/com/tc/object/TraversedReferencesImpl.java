/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
