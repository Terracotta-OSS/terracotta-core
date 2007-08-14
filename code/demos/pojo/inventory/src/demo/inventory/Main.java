/*
 @COPYRIGHT@
 */
package demo.inventory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

public class Main {
	private Store store = new Store();
	private PrintWriter out = new PrintWriter(System.out, true);

	private void run() {
		menu_main();
	}

	public static void main(String[] args) {
		try {
			new Main().run();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.flush();
		}
	}

	private void printInventory() {
		out.println("+-------------------+");
		out.println("| Inventory Listing |");
		out.println("+-------------------+");
		out.println();
		printProductHeader();
		for (Iterator i = store.getInventory().values().iterator(); i.hasNext();) {
			Product p = (Product) i.next();
			printProduct(p);
		}
	}

	private void printDepartments() {
		out.println("+----------------------------------+");
		out.println("| Inventory Listing by Departments |");
		out.println("+----------------------------------+");
		out.println();
		for (Iterator i = store.getDepartments().iterator(); i.hasNext();) {
			Department d = (Department) i.next();
			out.println("Department: " + d.getName());
			Product[] products = d.getProducts();
			for (int p = 0; p < products.length; p++) {
				printProduct(products[p]);
			}
			out.println();
		}
	}

	private void printProductHeader() {
		out.println("SKU     Product Name        Price");
		out.println("------  ------------------  --------");
	}

	private void printProduct(Product p) {
		out.print(padString(p.getSKU(), 8));
		out.print(padString(p.getName(), 20));
		out.print(padString(p.getPrice() + "", 8));
		out.println();
	}

	private void menu_main() {
		out.println();
		out.println("DSO Inventory Manager");
		out.println();
		out
				.println("This sample application shows how to use Terracotta DSO to share and");
		out.println("propagate changes to data structures.");
		out.println();
		out
				.println("To perform an action, press the key encased in the square-brackets");
		out.println("from the list of options presented.");
		out.println();
		out
				.println("Press the [H] key for detailed information on each action.");
		out.println();
		while (true) {
			out.println();
			out
					.println("+------------------------------------------------------------------+");
			out
					.println("| [I]nventory  [D]epartments  [U]pdate              [H]elp  [Q]uit |");
			out
					.println("+------------------------------------------------------------------+");
			out.print("> ");
			out.flush();
			String input = getInput().trim().toUpperCase();

			if (input.length() == 0)
				continue;

			switch (input.charAt(0)) {
			case 'I':
				printInventory();
				continue;
			case 'Q':
				return;
			case 'D':
				printDepartments();
				continue;
			case 'U':
				updatePrice();
				continue;
			case 'H':
				printHelp();
				continue;
			}
		}
	}

	private void updatePrice() {
		Product p = null;
		{
			printInventory();
			out.println("\nEnter SKU of product to update:");
			out.print("> ");
			out.flush();
			String s = getInput().toUpperCase();
			p = (Product) store.getInventory().get(s);
			if (p == null) {
				out.print("[ERR] No such product with SKU '" + s + "'\n");
				return;
			}
		}
		double d = -1;
		out.println();
		do {
			out.println("Enter new price for '" + p.getName() + "': ");
			out.print("> ");
			out.flush();
			String s = getInput().toUpperCase();
			try {
				d = Double.valueOf(s).doubleValue();
			} catch (NumberFormatException nfe) {
				continue;
			}
			synchronized (p) {
				p.setPrice(d);
			}
			;
		} while (d < 0);
		out.println("\nPrice updated:");
		printProduct(p);
	}

	private String getInput() {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(
				System.in));
		try {
			return stdin.readLine();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return "";
		}
	}

	private String padString(String in, int length) {
		StringWriter out = new StringWriter();
		out.write(in);
		length -= in.length();
		for (int i = 0; i < length; i++)
			out.write(' ');
		return out.toString();
	}

	private void printHelp() {
		out.println("+------+");
		out.println("| Help |");
		out.println("+------+");
		out.println();
		out
				.println("Press the key that correspond the action that you wish to perform");
		out.println("Here is what each of the actions will do:");
		out.println();
		out.println("[I]nventory:");
		out.println("This will list the contents of the inventory.");
		out.println();
		out.println("[D]epartments:");
		out
				.println("This will list the contents of the inventory, grouped by the");
		out.println("department that owns the inventory item.");
		out.println();
		out.println("[U]pdate:");
		out
				.println("Takes you into 'edit' mode to change the 'price' field value");
		out.println("of an inventory item.");
		out.println();
		out.println("[H]elp:");
		out.println("Print this information.");
		out.println();
		out.println("[Q]uit:");
		out.println("Exit this application.");
		out.println();
	}
}
