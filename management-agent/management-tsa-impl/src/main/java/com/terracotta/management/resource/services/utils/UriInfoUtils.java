/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services.utils;

import com.tc.license.ProductID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
public class UriInfoUtils {

  public static Set<ProductID> extractProductIds(UriInfo info) {
    List<String> ids = info.getQueryParameters().get("productIds");
    if (ids == null) {
      return null;
    }

    Set<ProductID> result = new HashSet<ProductID>();
    for (String idsString : ids) {
      List<String> idNames = Arrays.asList(idsString.split(","));
      for (String idName : idNames) {
        if (idName.equals("*")) {
          result.addAll(Arrays.asList(ProductID.values()));
        }
        try {
          result.add(ProductID.valueOf(idName));
        } catch (IllegalArgumentException iae) {
          // ignore
        }
      }
    }
    return result;
  }


}
