package cs455.scaling.tasks;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import cs455.scaling.util.StatisticLock;

public class WriteBatchTask implements Task {

	private SelectionKey key;
	private byte[] hash;

	public WriteBatchTask(SelectionKey sk, byte[] h) {
		this.key = sk;
		this.hash = h;
	}

	public Task doTask() throws IOException {
		//System.out.println("WriteBatchTask doTask() was called.");
		StatisticLock statLock = (StatisticLock)this.key.attachment();

		if (!statLock.tryAcquire()) { // try for statLock semaphore failed
			return this; // add write task to the taskQueue once more
		}

		// Attempt to write hash to buffer
		SocketChannel client;
		try {
			client = (SocketChannel) this.key.channel();
			ByteBuffer buffer = ByteBuffer.wrap(this.hash);
			while (buffer.hasRemaining()) { // writing to client
				client.write(buffer);
			}
			statLock.incrementProcessedMessages(); // incrementing processed messages
		} catch (Exception e) {
			System.err.println("WriteBatchTask: There was an issue writing to a socket. Presumably the client has shut down.");
			statLock.release();
			return null;
		} finally {
			statLock.release(); // releasing the write semaphore for this key
			return null;
		}
	}
}
