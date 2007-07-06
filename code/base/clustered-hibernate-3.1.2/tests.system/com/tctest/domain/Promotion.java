package com.tctest.domain;


public class Promotion {

	private PromotionId id = new PromotionId();

	private String reason;

	public Promotion() {
		super();
	}
  
	public PromotionId getId() {
		return id;
	}

	public void setId(PromotionId id) {
		this.id = id;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

  public String toString() {
		return "promotion.id: " + id + ", promotion.reason: " + reason;
	}
}
