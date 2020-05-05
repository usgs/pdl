package gov.usgs.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class ProcessManager implements Callable<Integer> {

  public String command;
  public long timeout;
  // streams to read/write
  public InputStream stdin = null;
  public ByteArrayOutputStream stdout = new ByteArrayOutputStream();
  public ByteArrayOutputStream stderr = new ByteArrayOutputStream();
  // track result
  public boolean timedOut = false;
  public List<Exception> errors = new ArrayList<Exception>();

  public ProcessManager(final String command, final long timeout) {
    this.command = command;
    this.timeout = timeout;
  }

  public Integer call() {
    final Process process;
    // start process
    try {
      process = Runtime.getRuntime().exec(this.command);
    } catch (IOException e) {
      errors.add(e);
      return -1;
    }
    // schedule timeout
    Timer commandTimer = new Timer();
    if (this.timeout > 0) {
      commandTimer.schedule(new TimerTask() {
        public void run() {
          process.destroy();
        }
      }, this.timeout);
    }
    // io threads
    final Thread stdinThread = new Thread(() -> {
      if (this.stdin != null) {
        try (final OutputStream out = process.getOutputStream()) {
          StreamUtils.transferStream(this.stdin, out);
        } catch (Exception e) {
          this.errors.add(e);
        }
      }
    });
    final Thread stdoutThread = new Thread(() -> {
      try (final InputStream in = process.getInputStream()) {
        StreamUtils.transferStream(in, this.stdout);
      } catch (Exception e) {
        this.errors.add(e);
      }
    });
    final Thread stderrThread = new Thread(() -> {
      try (final InputStream in = process.getErrorStream()) {
        StreamUtils.transferStream(in, this.stderr);
      } catch (Exception e) {
        this.errors.add(e);
      }
    });
    // start io threads
    stdinThread.start();
    stdoutThread.start();
    stderrThread.start();
    // Wait for process to complete
    try {
      process.waitFor();
    } catch (Exception e) {
      errors.add(e);
    }
    // Cancel the timer if it was not triggered
    commandTimer.cancel();
    // join io threads
    try {
      stdinThread.join();
    } catch (Exception e) {
      errors.add(e);
    }
    try {
      stdoutThread.join();
    } catch (Exception e) {
      errors.add(e);
    }
    try {
      stderrThread.join();
    } catch (Exception e) {
      errors.add(e);
    }
    // return exit code
    return process.exitValue();
  }

}
