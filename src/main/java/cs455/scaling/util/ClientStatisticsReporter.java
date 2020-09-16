package cs455.scaling.util;
import java.util.concurrent.atomic.*;
import java.util.TimerTask;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClientStatisticsReporter extends TimerTask {
	
	private AtomicInteger sent;
	private AtomicInteger received;
	private LinkedList<byte[]> sentHashes;

	public ClientStatisticsReporter() {
		this.sent = new AtomicInteger(0);
		this.received = new AtomicInteger(0);
		this.sentHashes = new LinkedList<byte[]>();
	}

	public void incrementSent() {
		this.sent.getAndIncrement();
	}

	public void incrementReceived() {
		this.received.getAndIncrement();
	}

	public Integer getSent() {
		return this.sent.get();
	}

	public Integer getReceived() {
		return this.received.get();
	}

	public void resetSent() {
		this.sent.set(0);
	}

	public void resetReceived() {
		this.received.set(0);
	}

	public void addHash(byte[] hash) {
		synchronized (this.sentHashes) {
			this.sentHashes.add(hash);
		}
	}

	// Method to remove hash from linked list if it exists
	public boolean removeHash(byte[] hash) {
		boolean found = false;
		synchronized (this.sentHashes) {
			for (int i = 0; i < this.sentHashes.size(); i++) {
				if (Arrays.equals(this.sentHashes.get(i), hash)) {
					this.sentHashes.remove(i);
					found = true;
					break;
				}
			}
		}
		return found;
	}

	// Run just prints out statistics held by the class object
	public void run() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime time = LocalDateTime.now();
		System.out.print("[" + dtf.format(time) + "] Total Sent Count: " + this.getSent() + ", Total Received Count: " + this.getReceived() + "\n");
		this.resetSent();
		this.resetReceived();
	}
}