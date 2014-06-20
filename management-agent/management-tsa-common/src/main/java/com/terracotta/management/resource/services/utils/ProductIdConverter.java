/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services.utils;

import com.tc.license.ProductID;

import java.util.HashSet;
import java.util.Set;

public class ProductIdConverter {
  
  public static Set<ProductID> stringsToProductsIds(Set<String> clientProductIds) {
    if (clientProductIds ==  null) {
      return null;
    }
    Set<ProductID> productIds = new HashSet<ProductID>();
    for (String clientProductID : clientProductIds) {
      productIds.add(ProductID.valueOf(clientProductID));
    }
    return productIds;
  }

  public static Set<String> productIdsToStrings(Set<ProductID> productIDs) {
    if (productIDs == null) {
      return null;
    }
    Set<String> strings = new HashSet<String>();
    for (ProductID productID : productIDs) {
      strings.add(productID.name());
    }
    return strings;
  }
  
}
