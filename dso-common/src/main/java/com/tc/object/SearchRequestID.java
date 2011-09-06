/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.util.AbstractIdentifier;

/**
 * Identifier to coordinate search requests.
 * 
 * @author Nabib El-Rahman
 */
public class SearchRequestID extends AbstractIdentifier {

  public static final SearchRequestID NULL_ID = new SearchRequestID();

  private static final String         ID_TYPE = "SearchRequestID";

  public SearchRequestID(final long id) {
    super(id);
  }

  private SearchRequestID() {
    super();
  }

  @Override
  public String getIdentifierType() {
    return ID_TYPE;
  }

}
