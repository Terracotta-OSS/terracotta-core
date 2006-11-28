package test.inventory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Iterator;

public class Client {

  private ApplicationContext m_ctx   = new ClassPathXmlApplicationContext("test/inventory/inventory.xml");
  private Store              m_store = (Store) m_ctx.getBean("store");

  private void printInventory() {
    System.out.println("Current inventory:\n");
    printProductHeader();
    for (Iterator i = m_store.getInventory().values().iterator(); i.hasNext();) {
      Product product = (Product) i.next();
      printProduct(product);
    }
  }

  private void printDepartments() {
    System.out.println("Current departments:");
    for (Iterator i = m_store.getDepartments().iterator(); i.hasNext();) {
      Department department = (Department) i.next();
      System.out.println("Department: " + department.getName());
      Product[] products = department.getProducts();
      for (int j = 0; j < products.length; j++) {
        printProduct(products[j]);
      }
      System.out.println();
    }
  }

  private void printProductHeader() {
    System.out.println("SKU     Product Name        Price");
    System.out.println("------  ------------------  --------");
  }

  private void printProduct(Product p) {
    System.out.print(padString(p.getSku(), 8));
    System.out.print(padString(p.getName(), 20));
    System.out.print(padString(p.getPrice() + "", 8));
    System.out.println();
  }

  private void run() {
    while (true) {
      System.out.println("\n\nMain menu: I)nventory  D)epartments U)pdate  Q)uit");
      String input = getInput().trim().toUpperCase();
      if (input.length() == 0) continue;
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

      }
    }
  }

  private void updatePrice() {
    printInventory();

    System.out.println("\nEnter SKU of product to update:");
    String sku = getInput().toUpperCase();
    Product product = (Product) m_store.getInventory().get(sku);
    if (product == null) {
      System.out.print("No such product with SKU '" + sku + "'\n\n");
      return;
    }

    double price = -1;
    do {
      System.out.println("Enter new price for '" + product.getName() + "': ");
      try {
        price = Double.valueOf(getInput().toUpperCase()).doubleValue();
      } catch (NumberFormatException nfe) {
        continue;
      }
      product.setPrice(price);
    } while (price < 0);

    System.out.println("\nPrice updated:");
    printProduct(product);
  }

  private String getInput() {
    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
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
    for (int i = 0; i < length; i++) {
      out.write(' ');
    }
    return out.toString();
  }

  /**
   * Initializes the store by pushing in some initial data.
   * In a real-world app we would probably get this info from the DB or have some other 
   * way of pagingg in the data, but now it is hard-coded for simplicity.
   */
  private void initialize() {
    synchronized (m_store) {
      if (m_store.getDepartments().size() != 0) { return; }
      
      System.out.println("### initializing store...");
      // populate our store with some sample products and departments
      Product warandpeace = new Product("War and Peace", 7.99, "WRPC");
      Product tripod = new Product("Camera Tripod", 78.99, "TRPD");
      Product usbmouse = new Product("USB Mouse", 19.99, "USBM");
      Product flashram = new Product("1GB FlashRAM card", 47.99, "1GFR");

      Department housewares = new Department(
          "B", "Books", new Product[] { warandpeace });
      Department photography = new Department(
          "P", "Photography", new Product[] { tripod, flashram });
      Department computers = new Department(
          "C", "Computers", new Product[] { usbmouse, flashram, });

      m_store.addDepartment(housewares);
      m_store.addDepartment(photography);
      m_store.addDepartment(computers);

      m_store.addInventoryItem(warandpeace.getSku(), warandpeace);
      m_store.addInventoryItem(tripod.getSku(), tripod);
      m_store.addInventoryItem(usbmouse.getSku(), usbmouse);
      m_store.addInventoryItem(flashram.getSku(), flashram);
    }
  }
  
  public static void main(String[] args) {
    try {
      Client client = new Client();
      client.initialize();
      client.run();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.flush();
    }
  }
}
