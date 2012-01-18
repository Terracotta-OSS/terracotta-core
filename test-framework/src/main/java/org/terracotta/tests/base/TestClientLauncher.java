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
		//  Client Class must implement Runnable and has a constructor with arguments String[]
		try {
			Class<? extends Runnable> clientClass = (Class<? extends Runnable>) TestClientLauncher.class
					.getClassLoader().loadClass(clientClassName);

			String[][] clientArgs = new String[1][];
			clientArgs[0] = Arrays.copyOfRange(args, 1, args.length);
			Constructor<Runnable> constructor = (Constructor<Runnable>) clientClass
					.getConstructor(String[].class);
			Runnable newInstance = constructor.newInstance(clientArgs);
			newInstance.run();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
