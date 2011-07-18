/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.inventory;

public class Department {
	private final String code;
	private final String name;
	private final Product[] products;

	public Department(final String c, final String n, final Product[] p) {
		code = c;
		name = n;
		products = p;
	}

	public final String getCode() {
		return code;
	}
	
	public final String getName() {
		return name;
	}

	public final Product[] getProducts() {
		return products;
	}
}
