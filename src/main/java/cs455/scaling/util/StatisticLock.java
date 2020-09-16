package cs455.scaling.util;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class StatisticLock {
	private Semaphore writeLock;
	private AtomicInteger processedMessages;

	public StatisticLock() {
		this.writeLock = new Semaphore(1);
		processedMessages = new AtomicInteger(0);
	}

	public void release() {
		this.writeLock.release();
	}

	public boolean tryAcquire() {
		return this.writeLock.tryAcquire();
	}

	public void incrementProcessedMessages() {
		this.processedMessages.getAndIncrement();
	}

	public Integer getProcessedMessages() {
		return this.processedMessages.get();
	}

	public void resetProcessedMessages() {
		this.processedMessages.set(0);
	}
}