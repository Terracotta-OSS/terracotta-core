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
