package gov.usgs.earthquake.distribution;

import gov.usgs.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class Command {

	private String[] commandArray = null;
	private String[] envp = null;
	private File dir = null;

	private InputStream stdin = null;
	private ByteArrayOutputStream stdout = new ByteArrayOutputStream();
	private ByteArrayOutputStream stderr = new ByteArrayOutputStream();

	private long timeout = 0;
	private int exitCode = -1;

	/** Empty command constructor */
	public Command() {
	}

	/**
	 * @throws CommandTimeout CommandTimeout
	 * @throws IOException IOException
	 * @throws InterruptedException InterruptedException
	 */
	public void execute() throws CommandTimeout, IOException,
			InterruptedException {
		StreamTransferThread outputTransfer = null;
		StreamTransferThread errorTransfer = null;

		try {
			final Process process = Runtime.getRuntime().exec(commandArray,
					envp, dir);

			final Timer timer;
			if (timeout > 0) {
				timer = new Timer();
				timer.schedule(new TimerTask() {
					public void run() {
						process.destroy();
					}
				}, timeout);
			} else {
				timer = null;
			}

			try {
				OutputStream processStdin = process.getOutputStream();
				if (stdin != null) {
					StreamUtils.transferStream(stdin, processStdin);
				}
				StreamUtils.closeStream(processStdin);

				outputTransfer = new StreamTransferThread(process.getInputStream(),
						stdout);
				outputTransfer.start();
				errorTransfer = new StreamTransferThread(process.getErrorStream(),
						stderr);
				errorTransfer.start();

				// now wait for process to complete
				exitCode = process.waitFor();
				if (exitCode == 143) {
					throw new CommandTimeout();
				}
			} finally {
				// cancel destruction of process, if it hasn't already run
				if (timer != null) {
					timer.cancel();
				}
			}
		} finally {
			try {
				outputTransfer.interrupt();
				outputTransfer.join();
			} catch (Exception e) {
			}
			try {
				errorTransfer.interrupt();
				errorTransfer.join();
			} catch (Exception e) {
			}
		}
	}

	/** @return string[] */
	public String[] getCommand() {
		return commandArray;
	}

	/**	@param command single command */
	public void setCommand(final String command) {
		setCommand(splitCommand(command));
	}

	/** @param commandArray string[] */
	public void setCommand(final String[] commandArray) {
		this.commandArray = commandArray;
	}

	/** @return envp */
	public String[] getEnvp() {
		return envp;
	}

	/** @param envp String[] */
	public void setEnvp(final String[] envp) {
		this.envp = envp;
	}

	/** @return dir */
	public File getDir() {
		return dir;
	}

	/** @param dir File */
	public void setDir(final File dir) {
		this.dir = dir;
	}

	/** @return timeout */
	public long getTimeout() {
		return timeout;
	}

	/** @param timeout long */
	public void setTimeout(final long timeout) {
		this.timeout = timeout;
	}

	/** @return exitCode */
	public int getExitCode() {
		return exitCode;
	}

	/** @param stdin InputStream */
	public void setStdin(final InputStream stdin) {
		this.stdin = stdin;
	}

	/** @return stdout byte[] */
	public byte[] getStdout() {
		return stdout.toByteArray();
	}

	/** @return stderr byte[] */
	public byte[] getStderr() {
		return stderr.toByteArray();
	}

	/**
	 * Split a command string into a command array.
	 *
	 * This version uses a StringTokenizer to split arguments. Quoted arguments
	 * are supported (single or double), with quotes removed before passing to
	 * runtime. Double quoting arguments will preserve quotes when passing to
	 * runtime.
	 *
	 * @param command
	 *            command to run.
	 * @return Array of arguments suitable for passing to
	 *         Runtime.exec(String[]).
	 */
	protected static String[] splitCommand(final String command) {
		List<String> arguments = new LinkedList<String>();
		String currentArgument = null;

		// use a tokenizer because that's how Runtime.exec does it currently...
		StringTokenizer tokens = new StringTokenizer(command);
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();

			if (currentArgument == null) {
				currentArgument = token;
			} else {
				// continuing previous argument, that was split on whitespace
				currentArgument = currentArgument + " " + token;
			}

			if (currentArgument.startsWith("\"")) {
				// double quoted argument
				if (currentArgument.endsWith("\"")) {
					// that has balanced quotes
					// remove quotes and add argument
					currentArgument = currentArgument.substring(1,
							currentArgument.length() - 1);
				} else {
					// unbalanced quotes, keep going
					continue;
				}
			} else if (currentArgument.startsWith("'")) {
				// single quoted argument
				if (currentArgument.endsWith("'")) {
					// that has balanced quotes
					// remove quotes and add argument
					currentArgument = currentArgument.substring(1,
							currentArgument.length() - 1);
				} else {
					// unbalanced quotes, keep going
					continue;
				}
			}

			arguments.add(currentArgument);
			currentArgument = null;
		}

		if (currentArgument != null) {
			// weird, but add argument anyways
			arguments.add(currentArgument);
		}

		return arguments.toArray(new String[] {});
	}

	public static class CommandTimeout extends Exception {

		private static final long serialVersionUID = 1L;

	}

	private static class StreamTransferThread extends Thread {
		private InputStream in;
		private OutputStream out;

		public StreamTransferThread(final InputStream in, final OutputStream out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {
			try {
				StreamUtils.transferStream(in, out);
			} catch (Exception e) {
			}
		}
	}

}