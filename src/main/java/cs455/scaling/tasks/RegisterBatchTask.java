package cs455.scaling.tasks;
import java.net.*; 
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import cs455.scaling.util.StatisticLock;

public class RegisterBatchTask implements Task {

	private Selector selector;
	private SelectionKey key;

	public RegisterBatchTask(Selector s, SelectionKey sk) {
		this.selector = s;
		this.key = sk;
	}

	public Task doTask() throws IOException {
		// Try to register the new channel
		try {
			SocketChannel client = ((ServerSocketChannel) this.key.channel()).accept();
			client.configureBlocking(false);
			SelectionKey newKey = client.register(this.selector, SelectionKey.OP_READ); // registering connection
			newKey.attach(new StatisticLock());
		} catch (Exception e) {
			// If we enter this catch, either the connection has already been accepted,
			// or the ServerSocketChannel has been removed, in either case, nothing we
			// should do from here, other than return null
		} finally {
			return null;
		}
	}
}
