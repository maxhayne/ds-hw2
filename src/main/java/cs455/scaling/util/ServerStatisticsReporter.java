package cs455.scaling.util;
import java.util.concurrent.atomic.*;
import java.nio.channels.*;
import java.util.*;
import java.util.TimerTask;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import cs455.scaling.util.StatisticLock;

public class ServerStatisticsReporter extends TimerTask {
	
	private Selector selector;
	private LinkedList<Double> totals;

	public ServerStatisticsReporter(Selector s) {
		this.selector = s;
		this.totals = new LinkedList<Double>();
	}

	public void run() {
		Set<SelectionKey> keys;
		Iterator<SelectionKey> iter;

		try {
			keys = this.selector.keys();
		} catch (Exception e) { // Probably the selector has been shut down
			System.err.println("SSR: ServerStatisticsReporter could not get keys from selector.");
			return;
		}

		Double processedSum = new Double(0);

		// Iterate through key set and add up total number of processed messages
		// Keep list of processed messages in linked list for std. dev. calculation
		synchronized (keys) { // Need the lock for keys while iterating
			try {
				iter = keys.iterator();
			} catch (Exception e) { // Probably the selector has been shut down
				System.err.println("SSR: ServerStatisticsReporter could not get iterator from key set.");
				return;
			}
			while (iter.hasNext()) {
				try {
					SelectionKey key = iter.next();
					if (key.isValid() == false) {
						continue;
					}
					// If they key is associated with the ServerSocketChannel
					if ((key.interestOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) { // This is the serverSocket
						continue;
					}

					StatisticLock statLock = (StatisticLock)key.attachment();
					Double processed = Double.valueOf(statLock.getProcessedMessages());
					statLock.resetProcessedMessages();
					this.totals.add(processed);
					processedSum += processed;
				} catch (Exception e) {
					// There was a problem with the process of grabbing SelectionKey's processedCount
					System.err.println("SSR: There was a problem collecting and resetting a key's count.");
					continue;
				}
			}
		}

		DecimalFormat two = new DecimalFormat("#0.00"); // formatting to two sig figs to right of decimal
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime time = LocalDateTime.now();

		if (this.totals.size() == 0) {
			String output = "[" + dtf.format(time) + "] Server Throughput: " + two.format(processedSum/20.0) + 
							" messages/s, Active Client Connections: 0" + 
							", Mean Per-Client Throughput: N/A, Std. Dev. Of Per-Client Throughput: N/A";
			System.out.println(output);
			this.totals.clear();
			return;
		}

		Double mean = processedSum/Double.valueOf(this.totals.size())/20.0; // mean messages processed per client per second
		Double squaredError = new Double(0);
		Double size = Double.valueOf(this.totals.size()); // total number of 

		for (int i = 0; i < this.totals.size(); i++) {
			squaredError += Math.pow((mean - (this.totals.get(i)/20.0)), 2);
		}

		Double stdDev = Math.pow((squaredError/size), 0.5);
		String output = "[" + dtf.format(time) + "] Server Throughput: " + two.format(processedSum/20.0) + 
						" messages/s, Active Client Connections: " + this.totals.size() +
						", Mean Per-Client Throughput: " + two.format(mean) + " messages/s, Std. Dev. Of Per-Client Throughput: " + 
						two.format(stdDev) + " messages/s";

		System.out.println(output);
		this.totals.clear();
		return;
	}
}