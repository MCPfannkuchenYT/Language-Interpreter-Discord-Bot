
package de.pfannekuchen.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Utils {

	private static ExecutorService executor = Executors.newFixedThreadPool(3);
	
	/**
	 * Executes and waits for a Process to finish
	 * @param cmd Command to run split by ' '
	 */
	public static String run(String cmd, boolean redirectOut, long timeoutInSeconds, Function<Boolean, Boolean> runAfter) {
		Future<ByteArrayOutputStream> s = executor.submit(() -> {
			try {
				ProcessBuilder builder = new ProcessBuilder(cmd.split(" ")).redirectErrorStream(true);
				Process p = builder.start();
				p.getOutputStream().close();
				
				if (redirectOut) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					copyLarge(p.getInputStream(), baos, new byte[4096]);
					p.waitFor();
					return baos;
				} else {
					deleteLarge(p.getInputStream(), new byte[4096]);
					p.waitFor();
				}
			} catch (Exception e) {
				System.out.println("Task could not finish...");
				e.printStackTrace();
			}
			return null;
		});
		try {
			ByteArrayOutputStream out = s.get(timeoutInSeconds, TimeUnit.SECONDS);
			if (runAfter != null) {
				runAfter.apply(false);
				runAfter = null;
			}
			return out.toString();
		} catch (Exception e) {
			if (runAfter != null) runAfter.apply(true);
			return null;
		}
	}
	
	/**
	 * Clears an Input Stream
	 * @param input In Stream
	 * @param output Out Stream
	 * @param buffer Multi-user Array
	 * @return Returns count of objects copied
	 * @throws IOException Throws whenever stream was corrupted
	 */
	private static long deleteLarge(InputStream input, byte[] buffer) throws IOException {
		long count;
		int n;
		for(count = 0L; -1 != (n = input.read(buffer)); count += (long)n) {
			// hello there...
		}

		return count;
	}
	
	/**
	 * Copies an Input Stream to another one
	 * @param input In Stream
	 * @param output Out Stream
	 * @param buffer Multi-user Array
	 * @return Returns count of objects copied
	 * @throws IOException Throws whenever stream was corrupted
	 */
	public static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
		long count;
		int n;
		for(count = 0L; -1 != (n = input.read(buffer)); count += (long)n) {
			output.write(buffer, 0, n);
		}

		return count;
	}
	
}
