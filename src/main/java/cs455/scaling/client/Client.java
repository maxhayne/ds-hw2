package cs455.scaling.client;
import cs455.scaling.util.ClientStatisticsReporter;
import java.net.*; 
import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.channels.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client {

	public static void main(String[] args) throws IOException {

		// Variables for holding command line arguments
		String serverHostName = "";
		Integer serverPort = -1;
		Integer messageRate = -1;

		// Parse command line arguments
		try {
			serverHostName = String.valueOf(args[0]);
			serverPort = Integer.valueOf(args[1]);
			messageRate = Integer.valueOf(args[2]);
		} catch (Exception e) {
			System.err.println(e);
			System.err.print("There was a problem with your command line arguments.\n");
			return;
		}
		
		// Sanity check on serverPort and messageRate
		if (serverPort < 1024 || messageRate < 1) {
			System.out.print("Reconsider the values of your command line arguments.\n");
			return;
		}

		// Creating SocketChannel and binding to specified port and server
		SocketChannel socket;
		try {
			socket = SocketChannel.open( new InetSocketAddress(serverHostName, serverPort) );
		} catch (Exception e) {
			System.err.print("Client could not connect to server.\n");
			return;
		}

		// From here, spawn thread to start sending messages at the specified message rate
		// Every twenty seconds, print out statistics, perhaps using timer like in slides

		// Creating statistics reporter, and timer
		Timer timer = new Timer();
		ClientStatisticsReporter reporter = new ClientStatisticsReporter();

		// Creating client sender and receiver
		ClientSender sender = new ClientSender(socket, reporter, timer);
		ClientReceiver receiver = new ClientReceiver(socket, reporter);
		receiver.start(); // starting receiver

		// Configuring timer for sender
		long waitPerSend = 1000/messageRate;
		timer.scheduleAtFixedRate(sender, 0L, waitPerSend); // Sender
		timer.scheduleAtFixedRate(reporter, 0L, 20000L); // Reporter
		return;
	}
}
