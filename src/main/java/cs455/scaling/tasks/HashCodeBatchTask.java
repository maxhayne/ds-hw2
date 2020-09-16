package cs455.scaling.tasks;
import java.net.*; 
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCodeBatchTask implements Task {

	private SelectionKey key;
	private byte[] msg;

	public HashCodeBatchTask(SelectionKey sk, byte[] m) {
		this.key = sk;
		this.msg = m;
	}

	// Method to hash a byte[]
	private byte[] SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		return hash; 
	} 

	public Task doTask() {
		byte[] hash;
		try {
			hash = SHA1FromBytes(msg);
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println("SHA1 does not exist.");
			return null;
		}
		// Return new Write Task based on the hash and the selection key
		WriteBatchTask task = new WriteBatchTask(this.key, hash);
		return task;
	}
}
