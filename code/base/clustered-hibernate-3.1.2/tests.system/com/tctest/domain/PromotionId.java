/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.domain;

import java.io.Serializable;

public class PromotionId implements Serializable {
  private Long customerId;
  
  private Long giftId;
  
  public PromotionId() {
    super();
  }
  
  public PromotionId(Long customerId, Long giftId) {
    this.customerId = customerId;
    this.giftId = giftId;
  }
  
  public boolean equals(Object obj) {
    if ((obj != null) && (obj instanceof PromotionId)) {
      PromotionId id = (PromotionId) obj;
      return (customerId.equals(id.customerId)) && (giftId.equals(id.giftId));
    } else {
      return false;
    }
  }
  
  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public Long getGiftId() {
    return giftId;
  }

  public void setGiftId(Long giftId) {
    this.giftId = giftId;
  }

  public int hashCode() {
    return customerId.hashCode() + giftId.hashCode();
  }

  public String toString() {
    return "promotionId.customerId: " + customerId + ", promotionId.giftId: " + giftId;
  }
}
