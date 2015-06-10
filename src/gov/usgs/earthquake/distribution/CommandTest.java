package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.distribution.Command.CommandTimeout;

import org.junit.Assert;
import org.junit.Test;

public class CommandTest {

	@Test
	public void testTimeout() throws Exception {
		Command command = new Command();
		command.setCommand("sleep 1");
		command.setTimeout(500);

		try {
			command.execute();
			Assert.fail("Command should throw timeout");
		} catch (CommandTimeout ct) {
		}
	}

	@Test
	public void testSuccess() throws Exception {
		Command command = new Command();
		command.setCommand("sleep 1");
		command.setTimeout(1500);

		try {
			command.execute();
		} catch (CommandTimeout ct) {
			Assert.fail("Command should not throw timeout");
		}
	}

	@Test
	public void testOutput() throws Exception {
		Command command = new Command();
		command.setCommand("ls -al");
		command.setTimeout(1000);
		
		command.execute();
		System.err.println(new String(command.getStdout()));
	}

	@Test
	public void testError() throws Exception {
		Command command = new Command();
		command.setCommand("grep 'abc d' notfound");
		command.setTimeout(500);

		command.execute();
		System.err.println(new String(command.getStderr()));
	}
}
