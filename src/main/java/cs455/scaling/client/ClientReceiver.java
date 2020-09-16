package cs455.scaling.client;
import cs455.scaling.util.ClientStatisticsReporter;
import java.nio.channels.*;
import java.util.*;
import java.nio.*;
import java.io.*;

public class ClientReceiver extends Thread {
	
	private SocketChannel channel;
	private ClientStatisticsReporter reporter;
	private boolean isStopped;

	public ClientReceiver(SocketChannel c, ClientStatisticsReporter r) {
		this.channel = c;
		this.reporter = r;
		this.isStopped = false;
	}

	public synchronized void stopThread() { // function to force thread to exit while loop
		this.isStopped = true;
	}

	public synchronized boolean getIsStopped() { // returns the status of the thread
		return this.isStopped;
	}

	@Override
	public void run() {
		ByteBuffer readBuffer = ByteBuffer.allocate(20);  // For hashes it receives
		while (!this.getIsStopped()) {
			readBuffer.clear(); // clearing to read new hash
			int bytesRead = 0;
			try { // attempting to read the hash from the server
				while (readBuffer.hasRemaining() && bytesRead != -1) {
					bytesRead = channel.read(readBuffer);
				}
			} catch (IOException ioe) {
				System.err.print("ClientReceiver: An issue occurred while reading from the socket.\n");
			}

			if (bytesRead == -1) { // shutting down the connection
				System.out.print("ClientReceiver: Channel has been closed on the server end. Shutting down.\n");
				try {
					this.channel.close(); // close the channel
				} catch (IOException ioe) {
					System.err.print("ClientReceiver: There was an issue closing the socket.\n");
				}
				return;
			}

			readBuffer.rewind(); // rewinding the buffer
			byte[] serverHash = new byte[readBuffer.remaining()]; // filling byte[] with received hash
			readBuffer.get(serverHash);
			boolean found = this.reporter.removeHash(serverHash); // remove hash from linked list if present
			if (!found) { // if not there, print to console that hash had no match
				System.out.print("ClientReceiver: Didn't have record of the hash it received.\n");
			} else {
				//System.out.println("ClientReceiver: Removed a hash from the sentHashes linked list.");
			}
			this.reporter.incrementReceived(); // increment received count
			serverHash = null;
		}

		try { // if while loop is exited, attempt to close the socket channel
			this.channel.close();
		} catch (IOException ioe) {
			System.err.print("ClientReceiver: There was an issue closing the socket.\n");
		}
		return;
	}
}