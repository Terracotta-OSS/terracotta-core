/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharedqueue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.tcclient.cluster.DsoNode;

public class Queue {

	private static final int MAX_HISTORY_LENGTH = 15;
	private static final int MAX_QUEUE_LENGTH = 150;

	private List<Job> queue = Collections.synchronizedList(new LinkedList<Job>());
	private List<Worker> workers = Collections.synchronizedList(new LinkedList<Worker>());
	private List<Job> completedJobs = Collections.synchronizedList(new LinkedList<Job>());

	private int nextJobId;
	private int port;

	public Queue(final int port) {
		this.port = port;
		this.nextJobId = 1;
	}

	public final Job getJob() {
		synchronized (queue) {
			while (queue.size() == 0) {
				try {
					queue.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return queue.remove(0);
		}
	}

	public final String getXmlData() {
		// the list of jobs in the queue
		String data = "<workqueue>";
		synchronized (queue) {
			for (Job job : queue) {
				data += job.toXml();
			}
		}
		data += "</workqueue>";

		// the list of completed jobs
		data += "<completed>";
		synchronized (completedJobs) {
			for (Job job : completedJobs) {
				data += job.toXml();
			}
		}
		data += "</completed>";

		// the list of registered job consumers
		data += "<consumers>";
		synchronized (workers) {
			for (Worker worker : workers) {
				data += worker.toXml();
			}
		}
		data += "</consumers>";
		return data;
	}

	public final Worker createWorker(final DsoNode node) {
		synchronized (workers) {
			Worker worker = new Worker(this, port, node);
			workers.add(worker);
			Thread t = new Thread(worker);
			t.setDaemon(true);
			t.start();
			return worker;
		}
	}

	public final Worker getWorker(final DsoNode node) {
		synchronized (workers) {
			for (Worker worker : workers) {
				if (worker.getNode().equals(node)) {
					return worker;
				}
			}
		}
		return null;
	}

	public final void log(final Job job) {
		synchronized (completedJobs) {
			completedJobs.add(0, job);
			if (completedJobs.size() > MAX_HISTORY_LENGTH) {
				completedJobs.remove(completedJobs.size() - 1);
			}
		}
	}

	public final void reap() {
		synchronized (workers) {
			ListIterator<Worker> i = workers.listIterator();
			while (i.hasNext()) {
				Worker worker = i.next();
				if (worker.expire()) {
					i.remove();
				}
			}
		}
	}

	public final void addJob() {
		synchronized (queue) {
			if (queue.size() < MAX_QUEUE_LENGTH) {
				Job job = new Job(Queue.getHostName() + " " + port, nextJobId);
				nextJobId = nextJobId < 999 ? nextJobId + 1 : 1;
				queue.add(job);
				queue.notifyAll();
			}
		}
	}

	public final void addJob(final Job job) {
		synchronized (queue) {
			queue.add(job);
			queue.notifyAll();
		}
	}

	public final static String getHostName() {
		try {
			final InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostName();
		} catch (UnknownHostException e) {
			return "Unknown";
		}
	}
}
