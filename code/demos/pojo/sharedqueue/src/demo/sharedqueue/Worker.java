/*
 @COPYRIGHT@
 */
package demo.sharedqueue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.tcclient.cluster.DsoNode;

class Worker implements Runnable {

	private final static int HEALTH_ALIVE = 0;
	private final static int HEALTH_DYING = 1;
	private final static int HEALTH_DEAD = 2;
	private final static int MAX_LOAD = 10;

	private final String name;
	private final int port;
	private final Queue queue;
	private final List<Job> jobs;
	private final DsoNode node;

	private int health = HEALTH_ALIVE;

	public Worker(final Queue queue, final int port, final DsoNode node) {
		this.name = Queue.getHostName();
		this.port = port;
		this.queue = queue;
		this.node = node;
		jobs = Collections.synchronizedList(new LinkedList<Job>());
	}

	public final DsoNode getNode() {
		return node;
	}

	public final String getName() {
		return "node: " + node + " (" + name + ":" + port + ")";
	}

	public final String toXml() {
		synchronized (jobs) {
			String data = "<worker><name>" + getName() + "</name><jobs>";
			for (Job job : jobs) {
				data += job.toXml();
			}
			data += "</jobs></worker>";
			return data;
		}
	}

	/**
	 * Attempt to mark the Worker as dead (if it's already dying); Note that we
	 * synchronize this method since it's mutating a shared object (this class)
	 *
	 * @return True if the Worker is dead.
	 */
	public final synchronized boolean expire() {
		if (HEALTH_DYING == health) {
			// a dying Worker wont die until it has
			// consumed all of it's jobs
			if (jobs.size() > 0) {
				queue.addJob(jobs.remove(0));
			} else {
				setHealth(HEALTH_DEAD);
			}
		}
		return (HEALTH_DEAD == health);
	}

	/**
	 * Set the state of the Worker's health; Note that we synchronize this
	 * method since it's mutating a shared object (this class)
	 *
	 * @param health
	 */
	private final synchronized void setHealth(final int health) {
		this.health = health;
	}

	/**
	 * Set the state of the Worker's health to dying; Note that we synchronize
	 * this method since it's mutating a shared object (this class)
	 *
	 * @param health
	 */
	public final synchronized void markForExpiration() {
		setHealth(HEALTH_DYING);
	}

	public final void run() {
		while (HEALTH_DEAD != health) {
			if ((HEALTH_ALIVE == health) && (jobs.size() < MAX_LOAD)) {
				final Job job = queue.getJob();

				try {
					Thread.sleep(500);
				} catch (InterruptedException ie) {
					System.err.println(ie.getMessage());
				}

				synchronized (jobs) {
					jobs.add(job);
				}

				Thread processor = new Thread(new Runnable() {
					public void run() {
						job.run(Worker.this);
						synchronized (jobs) {
							jobs.remove(job);
						}
						queue.log(job);
					}
				});
				processor.start();
			}
		}
	}
}
