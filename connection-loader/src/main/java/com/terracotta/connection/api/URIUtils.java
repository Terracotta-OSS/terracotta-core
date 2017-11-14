/*
 * Copyright (c) 2011-2017 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracotta.connection.api;

import java.net.URI;
import java.net.URISyntaxException;

class URIUtils {
  static void validateTerracottaURI(URI uri) throws URISyntaxException {
    String path = uri.getPath();
    if (path != null && path.length() > 0 && !path.equals("/")) {
      throw new URISyntaxException(uri.toString(), "A path should not be specified in a Terracotta URI");
    }

    String query = uri.getQuery();
    if (query != null) {
      throw new URISyntaxException(uri.toString(), "A query should not be specified in a Terracotta URI");
    }

    String fragment = uri.getFragment();
    if (fragment != null) {
      throw new URISyntaxException(uri.toString(), "A fragment should not be specified in a Terracotta URI");
    }
  }
}
