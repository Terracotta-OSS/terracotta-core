/*
 @COPYRIGHT@
 */
package demo.sharedqueue;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.ResourceHandler;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tcclient.cluster.DsoNode;

public class Main implements DsoClusterListener {

	private final File cwd = new File(System.getProperty("user.dir"));

	private int lastPortUsed;
	private demo.sharedqueue.Queue queue;
	private Worker worker;

	@InjectedDsoInstance
	private DsoCluster cluster;

	public final void start(int port) throws Exception {
		cluster.addClusterListener(this);

		DsoNode node = cluster.getCurrentNode();
		port = setPort(port);

		System.out.println("DSO SharedQueue (node " + node + ")");
		System.out.println("Open your browser and go to - http://"
				+ getHostName() + ":" + port + "/webapp\n");

		Server server = new Server();
		Connector connector = new SocketConnector();
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });

		queue = new Queue(port);
		worker = queue.createWorker(node);

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setResourceBase(".");

		ContextHandler ajaxContext = new ContextHandler();
		ajaxContext.setContextPath(SimpleHttpHandler.ACTION);
		ajaxContext.setResourceBase(cwd.getPath());
		ajaxContext.setClassLoader(Thread.currentThread()
				.getContextClassLoader());
		ajaxContext.addHandler(new SimpleHttpHandler(queue));

		HandlerCollection handlers = new HandlerCollection();
		handlers.setHandlers(new Handler[] { ajaxContext, resourceHandler });
		server.setHandler(handlers);

		startReaper();
		server.start();
		server.join();
	}

	private final int setPort(int port) {
		if (port == -1) {
			if (lastPortUsed == 0) {
				port = lastPortUsed = 1990;
			} else {
				port = ++lastPortUsed;
			}
		} else {
			lastPortUsed = port;
		}
		return port;
	}

	/**
	 * Starts a thread to identify dead workers (From nodes that have been
	 * brought down) and removes them from the (shared) list of workers.
	 */
	private final void startReaper() {
		Thread reaper = new Thread(new Runnable() {
			public void run() {
				while (true) {
					queue.reap();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						System.err.println(ie.getMessage());
					}
				}
			}
		});
		reaper.start();
	}

	public final static void main(final String[] args) throws Exception {
		int port = -1;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
		}
		new Main().start(port);
	}

	static final String getHostName() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostAddress();
		} catch (UnknownHostException e) {
			return "Unknown";
		}
	}

	public void nodeLeft(final DsoClusterEvent event) {
		DsoNode node = event.getNode();
		Worker worker = queue.getWorker(node);
		if (worker != null) {
			worker.markForExpiration();
		} else {
			System.err.println("Worker for node: " + node + " not found.");
		}
	}

	public void nodeJoined(final DsoClusterEvent event) {
		// unused
	}

	public void operationsDisabled(final DsoClusterEvent event) {
		// unused
	}

	public void operationsEnabled(final DsoClusterEvent event) {
		// unused
	}
}
