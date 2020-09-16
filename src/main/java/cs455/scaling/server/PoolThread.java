package cs455.scaling.server;
import cs455.scaling.tasks.*;
import java.net.*; 
import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PoolThread extends Thread {

	// PoolThread has access to taskQueue, where it will grab batches,
	// and taskStage, where it will append tasks that the TPM will batch

	private BlockingQueue<ArrayList<Task>> batchQueue = null;
	private BlockingQueue<Task> taskQueue = null;
	private boolean isStopped;

	public PoolThread(BlockingQueue<ArrayList<Task>> bq, BlockingQueue<Task> tq) {
		this.batchQueue = bq;
		this.taskQueue = tq;
		this.isStopped = false;
	}

	public synchronized boolean getIsStopped() {
		return this.isStopped;
	}

	public synchronized void stopThread() {
		this.isStopped = true;
	}

	public void addTask(Task t) {
		try {
			this.taskQueue.put(t);		
		} catch(InterruptedException ie) {
			System.err.print("PT: taskQueue could not be added to.");
		}
	}

	@Override
	public void run() {
		while (!this.getIsStopped()) { // Continue until stopped by TPM
			ArrayList<Task> batch = null;
			try {
				batch = this.batchQueue.poll(1, TimeUnit.SECONDS); // poll batchQueue for tasks
			} catch (InterruptedException ie) {
				System.err.println("PT: There was a problem grabbing a value from the taskQueue.");
			}
			if (batch == null) {
				//System.out.println("There was no batch to grab.");
				continue;
			} else {
				// Complete all the tasks in the batch...
				for (Task task : batch) {
					if (task == null) break;
					try { // do the task
						Task newTask = task.doTask(); // do task
						if (newTask != null) {
							this.addTask(newTask);
						}
					} catch (IOException ioe) {
						System.err.println("PT: Thread could not perform task in its buffer.");
					}
				}
			}
		}

		System.out.println("PoolThread stopping.");
	}

}