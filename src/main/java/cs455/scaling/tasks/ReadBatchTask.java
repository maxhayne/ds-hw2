package cs455.scaling.tasks;
import java.net.*; 
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class ReadBatchTask implements Task {

	private SelectionKey key;

	public ReadBatchTask(SelectionKey sk) {
		this.key = sk;
	}

	public Task doTask() throws IOException {		
		SocketChannel client;
		ByteBuffer buffer;
		int bytesRead = 0;
		// Attempt to read from socket channel
		try {
			client = (SocketChannel) this.key.channel();
			buffer = ByteBuffer.allocate(8192); // For 8KB byte array
			while (buffer.hasRemaining() && bytesRead != -1) {
				bytesRead = client.read(buffer);
			}
		} catch (Exception e) {
			System.err.println("ReadBatchTask: There was an issue reading data from a socket. Presumably the client has shut down.");
			return null;
		}

		if (bytesRead == -1) { // the client has disconnected from the server
			client.close();
			return null;
		}

		buffer.rewind(); // rewind buffer
		byte[] read = new byte[buffer.remaining()];
		buffer.get(read); // transfer buffer to byte[]
		HashCodeBatchTask task = new HashCodeBatchTask(this.key, read); // create HashCode task
		int ops = this.key.interestOps();
		this.key.interestOps(ops | SelectionKey.OP_READ); // Restore read interestOp to this key
		return task;
	}
}
