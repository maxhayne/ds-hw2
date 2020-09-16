package cs455.scaling.client;
import cs455.scaling.util.ClientStatisticsReporter;
import java.nio.channels.*;
import java.nio.*;
import java.io.*;
import java.util.*;
import java.util.TimerTask;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ClientSender extends TimerTask { // Sends message every 20 seconds, extends TimerTask
	
	private SocketChannel channel;
	private ClientStatisticsReporter reporter;
	private Random random;
	private Timer timer;
	private byte[] message;

	public ClientSender(SocketChannel c, ClientStatisticsReporter r, Timer t) {
		this.channel = c;
		this.reporter = r;
		this.random = new Random();
		this.timer = t;
		this.message = new byte[8192]; // create byte array for holding all generated hashes
	}

	// Function for generating hash
	private byte[] SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		return hash; 
	}

	public void run() {
		random.nextBytes(this.message); // filling byte[] with random data
		byte[] hash;
		try {
			hash = SHA1FromBytes(this.message); // generating hash based on data
		} catch (NoSuchAlgorithmException nsae) {
			System.err.print("SHA1 does not exist. Client could not send message.\n");
			return;
		}

		this.reporter.addHash(hash); // add hash to sentHashes linked list
		this.reporter.incrementSent(); // incrementing number of sent messages

		try { // attempt to write message to server
			ByteBuffer sendBuffer = ByteBuffer.wrap(this.message);
			while (sendBuffer.hasRemaining()) { // writing to client
				channel.write(sendBuffer);
			}
		} catch (IOException ioe) { // if there is an error, shut down everything
			System.err.print("ClientSender: An issue occurred while writing to the socket. Cancelling the send timer and stat timer.\n");
			this.cancel();
			this.reporter.cancel();
			this.timer.cancel();
			this.timer.purge();
		}
		return;
	}

}