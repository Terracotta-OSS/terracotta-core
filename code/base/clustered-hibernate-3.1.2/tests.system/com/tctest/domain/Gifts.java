package com.tctest.domain;


public class Gifts {

	private int id;

	private String name;

	private String category;

	public Gifts() {
		super();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
  public String toString() {
		return "gift.id: " + id + ", gift.name: " + name + ", gift.category: "
				+ category;
	}
}
