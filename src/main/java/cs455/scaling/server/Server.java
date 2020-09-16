package cs455.scaling.server;
import cs455.scaling.util.ServerStatisticsReporter;
import java.net.*; 
import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.channels.*;

public class Server {
	
	public static void main(String[] args) throws IOException {

		Integer serverPortNum = -1;
		Integer threadPoolSize = -1;
		Integer batchSize = -1;
		Integer batchTime = -1;

		// Parsing command line arguments
		try {
			serverPortNum = Integer.valueOf(args[0]);
			threadPoolSize = Integer.valueOf(args[1]);
			batchSize = Integer.valueOf(args[2]);
			batchTime = Integer.valueOf(args[3]);
		} catch (Exception e) {
			System.err.println(e);
			System.err.println("Server: There was a problem with your command line arguments. Plase see README.");
			return;
		}

		// Sanity checks on all four inputs
		if (serverPortNum < 1024 || threadPoolSize < 1 || batchSize < 1 || batchTime < 1 || batchTime > 1000) {
			System.out.println("Reconsider the values of your command line arguments.");
			return;
		}

		Selector selector = Selector.open();
		ThreadPoolManager manager = new ThreadPoolManager(selector, threadPoolSize, batchSize, batchTime);

		manager.startPool(); // starting all threads in the pool
		manager.start(); // starting the thread pool manager
				
		InetAddress inetAddress = InetAddress.getLocalHost();
		String localhost = inetAddress.getHostName();
		//System.out.println(localhost);

		// Starting and configuring the ServerSocketChannel
		ServerSocketChannel serverSocket;
		try {
			serverSocket = ServerSocketChannel.open();
			serverSocket.socket().bind( new InetSocketAddress(localhost, serverPortNum) ); // random port
			serverSocket.configureBlocking(false);
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			manager.stopPool();
			return;
		}

		// Creating timer for statistics collection
		Timer timer = new Timer();
		ServerStatisticsReporter reporter = new ServerStatisticsReporter(selector);
		timer.scheduleAtFixedRate(reporter, 0L, 20000L); 
		return;
	}
}
