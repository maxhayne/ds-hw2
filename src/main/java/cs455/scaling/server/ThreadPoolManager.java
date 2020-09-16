package cs455.scaling.server;
import cs455.scaling.tasks.*;
import java.net.*;
import java.io.*; 
import java.nio.*;
import java.util.*;
import java.nio.channels.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPoolManager extends Thread {

	private BlockingQueue<ArrayList<Task>> batchQueue = null;
	private BlockingQueue<Task> taskQueue = null;
	private List<PoolThread> poolThreads = null;
	private Selector selector;
	private Integer threadPoolSize;
	private Integer batchSize;
	private Integer batchTime; // should be in milliseconds
	private boolean isStopped;

	public ThreadPoolManager(Selector s, Integer tps, Integer bs, Integer bt) {
		this.batchQueue = new LinkedBlockingQueue<ArrayList<Task>>();
		this.taskQueue = new LinkedBlockingQueue<Task>();
		this.poolThreads = new ArrayList<PoolThread>();
		this.selector = s;
		this.threadPoolSize = tps;
		this.batchSize = bs;
		this.batchTime = bt;
		this.isStopped = false;
	}

	public synchronized boolean getIsStopped() { // returns whether thread is running
		return this.isStopped;
	}

	public synchronized void startPool() throws IOException {
		// Creating and adding all pool threads to the poolThreads array
		for (int i = 0; i < threadPoolSize; i++) {
			PoolThread pthread = new PoolThread(batchQueue, taskQueue);
			poolThreads.add(pthread);
		}

		// Starting all threads in the pool
		for (PoolThread pthread : poolThreads) {
			pthread.start();
		}
	}

	public synchronized void stopPool() { // Stop thread pool manager and all threads
		this.isStopped = true;

		for (PoolThread pthread : poolThreads) {
			pthread.stopThread();
		}
	}

	public void addBatch(ArrayList<Task> b) { // add batch to batchQueue
		try {
			this.batchQueue.put(b);		
		} catch(InterruptedException ie) {
			System.err.print("TPM: batchQueue could not be added to.");
		}
	}

	public void addTask(Task t) { // method to add to taskQueue
		try {
			this.taskQueue.put(t);		
		} catch(InterruptedException ie) {
			System.err.print("TPM: taskQueue could not be added to.");
		}
	}

	public Task getTask() { // get task from head of taskQueue
		try {
			return this.taskQueue.take();		
		} catch(InterruptedException ie) {
			System.err.print("TPM: taskQueue could not be taken from.");
		}
		return null;
	}

	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		// Consider only checking the isStopped variable once every second or so, for speed
		while (!this.getIsStopped()) {
			try {
				if (this.selector.select(this.batchTime/2) == 0) { // checking select at least twice per batchTime
					// If no keys are available to select, still need to make sure we are
					// flushing non-empty taskQueues to the batchQueue at batchTime intervals
					long endTime = System.currentTimeMillis();
					if (endTime-startTime > this.batchTime) { // push taskBuffer to taskQueue
						int size = this.taskQueue.size();
						if (size > 0) {
							if (size > this.batchSize) size = batchSize;
							ArrayList<Task> taskBuffer = new ArrayList<Task>(size);
							for (int i = 0; i < size; i++) {
								taskBuffer.add(i,this.getTask());
							}
							this.addBatch(taskBuffer);
							//System.out.println("TPM: Pushing taskBuffer to batchQueue.");
						}
						startTime = System.currentTimeMillis();
					}
					continue;
				}
			} catch (IOException ioe) {
				System.err.println("TPM: There was a problem calling selectNow().");
				continue;
			}

			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) { // Iterate through SelectionKeys
				SelectionKey key = iter.next();
				iter.remove();

				if (key.isValid() == false) {
					continue;
				}

				if (key.isAcceptable()) { // We know this key points to the ServerSocketChannel
					// Create RegisterBatchTask and add to taskQueue
					RegisterBatchTask task = new RegisterBatchTask(this.selector, key);
					this.addTask(task);
					//key.interestOps(0); // Should this be done?
				} else if (key.isReadable()) {
					// Create ReadBatchTask and add to taskQueue
					ReadBatchTask task = new ReadBatchTask(key);
					this.addTask(task);
					key.interestOps(0); // make sure we don't select on this channel until this is unset in the readbatchtask
				}

				long endTime = System.currentTimeMillis();
				if (endTime-startTime > this.batchTime) { // push taskBuffer to taskQueue
					int size = this.taskQueue.size();
					if (size > 0) {
						if (size > this.batchSize) size = batchSize;
						ArrayList<Task> taskBuffer = new ArrayList<Task>(size);
						for (int i = 0; i < size; i++) {
							taskBuffer.add(i,this.getTask());
						}
						this.addBatch(taskBuffer);
						//System.out.println("TPM: Pushing taskBuffer to batchQueue.");
					}
					startTime = System.currentTimeMillis();
				} else if (this.taskQueue.size() >= this.batchSize) { // push full taskBuffer to taskQueue
					//System.out.println(this.taskQueue.size());
					ArrayList<Task> taskBuffer = new ArrayList<Task>(this.batchSize);
					for (int i = 0; i < this.batchSize; i++) {
						taskBuffer.add(i,this.getTask());
					}
					this.addBatch(taskBuffer);
					//System.out.println("TPM: Pushing taskBuffer to batchQueue.");
					startTime = System.currentTimeMillis();
				} 
			}
		}

		System.out.println("TPM: ThreadPoolManager has stopped.");
		try {
			selector.close(); // closing the selector
		} catch (IOException ioe) {
			System.err.println("TPM: There was a problem closing the selector.");
		}
	}
}