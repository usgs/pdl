package gov.usgs.earthquake.eids;

import java.io.File;

import gov.usgs.earthquake.distribution.Factory;
import gov.usgs.earthquake.distribution.SocketProductReceiver;
import gov.usgs.util.FileUtils;

public class EIDSInputOutputWedgeEndToEnd {

	public static final File TEST_DIRECTORY = new File("testdir");

	// currently this test just blocks so it can be tested interactively.
	// @Test
	public static void main(final String[] args) throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// clean up the created files/directories
				FileUtils.deleteTree(TEST_DIRECTORY);
			}
		});

		System.err
				.println("This starts up an end to end instance of the "
						+ "EIDSInputWedge and EIDSOutputWedge.  Place message into 'polldir'"
						+ " and they should appear in 'outputdir'.  "
						+ "This runs for 1000 seconds, which should be sufficient for most tests.");
		// receiver
		EIDSOutputWedge output = new EIDSOutputWedge();
		output.setDirectory(new File(TEST_DIRECTORY, "outputdir"));
		output.setOutputFormat(EIDSOutputWedge.OUTPUT_TYPE_EQXML);
		Factory factory = new Factory();
		SocketProductReceiver receiver = factory.createSocketProductReceiver(
				11235, 1, new File(TEST_DIRECTORY, "receiverStorage"),
				new File(TEST_DIRECTORY, "receiverIndexFile"));
		receiver.addNotificationListener(output);
		receiver.startup();

		// sender
		new EIDSInputWedge()
				.run(new String[] {
						// poll
						EIDSInputWedge.POLL_ARGUMENT,
						// polldir
						EIDSInputWedge.POLLDIR_ARGUMENT
								+ new File(TEST_DIRECTORY, "polldir")
										.getCanonicalPath(),
						// servers
						EIDSInputWedge.SERVERS_ARGUMENT + "localhost:11235",
						// oldinput
						EIDSInputWedge.STORAGEDIR_ARGUMENT
								+ new File(TEST_DIRECTORY, "oldinput")
										.getCanonicalPath() });

		Thread.sleep(1000000);
	}
}
