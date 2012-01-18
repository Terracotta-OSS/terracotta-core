package org.terracotta.tests.base;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class TestClientLauncher {

  /**
   * @param args
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    String clientClassName = args[0];
    // Client Class must implement Runnable and has a constructor with
    // arguments String[]
    try {
      Class<? extends Runnable> clientClass = (Class<? extends Runnable>) TestClientLauncher.class.getClassLoader()
          .loadClass(clientClassName);

      String[] clientArgs = Arrays.copyOfRange(args, 1, args.length);
      Constructor<Runnable> constructor;
      constructor = (Constructor<Runnable>) clientClass.getConstructor(String[].class);
      Runnable newInstance = constructor.newInstance(new Object[] { clientArgs });
      newInstance.run();
    } catch (NoSuchMethodException e) {
      System.out.println("Class " + clientClassName
                         + "should have one constructor having argument and array of String( String[]) . ");
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
      throw new AssertionError("Exception while launching test client : " + e.getMessage());
    }

  }
}
