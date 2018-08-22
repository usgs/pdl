package gov.usgs.earthquake.indexer;

import java.util.Date;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.util.Config;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the ArchivePolicy class.
 * 
 * @author jmfee
 */
public class ArchivePolicyTest {

	/**
	 * New explicit configuration parameters (minEventAge/maxEventAge) are
	 * incompatible with (minAge/maxAge), and by policy cannot be mixed.
	 */
	@Test
	public void mixingConfigurationsThrowsException() throws Exception {
		Config config = new Config();
		config.setProperty("minAge", "1");
		config.setProperty("maxEventAge", "1");

		ArchivePolicy policy = new ArchivePolicy();
		try {
			policy.configure(config);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}

		Config config2 = new Config();
		config2.setProperty("maxAge", "1");
		config2.setProperty("minEventTime", "1");

		ArchivePolicy policy2 = new ArchivePolicy();
		try {
			policy2.configure(config2);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}
	}

	/**
	 * maxEventAge is the new property for minAge.
	 * 
	 * @throws Exception
	 */
	@Test
	public void minAgeSameAsMaxEventAge() throws Exception {
		long epsilon = 500L;

		Config minAgeConfig = new Config();
		minAgeConfig.setProperty("minAge", "1");
		ArchivePolicy minAgePolicy = new ArchivePolicy();
		minAgePolicy.configure(minAgeConfig);

		Config maxEventAgeConfig = new Config();
		maxEventAgeConfig.setProperty("maxEventAge", "1");
		ArchivePolicy maxEventAgePolicy = new ArchivePolicy();
		maxEventAgePolicy.configure(maxEventAgeConfig);

		ProductIndexQuery minAgeQuery = minAgePolicy.getIndexQuery();
		ProductIndexQuery maxEventAgeQuery = maxEventAgePolicy.getIndexQuery();

		long timeDiff = Math.abs(minAgeQuery.getMinEventTime().getTime() -
				maxEventAgeQuery.getMinEventTime().getTime());

		if (timeDiff != 0L) {
			System.out.printf("Queries differed by %d milliseconds.\n",
					timeDiff);
		}

		Assert.assertTrue("queries the same", timeDiff < epsilon);
	}

	/**
	 * minEventAge is the new property for maxAge.
	 * 
	 * @throws Exception
	 */
	@Test
	public void maxAgeSameAsMinEventAge() throws Exception {
		long epsilon = 500L;

		Config maxAgeConfig = new Config();
		maxAgeConfig.setProperty("maxAge", "1");
		ArchivePolicy maxAgePolicy = new ArchivePolicy();
		maxAgePolicy.configure(maxAgeConfig);

		Config minEventAgeConfig = new Config();
		minEventAgeConfig.setProperty("minEventAge", "1");
		ArchivePolicy minEventAgePolicy = new ArchivePolicy();
		minEventAgePolicy.configure(minEventAgeConfig);

		ProductIndexQuery maxAgeQuery = maxAgePolicy.getIndexQuery();
		ProductIndexQuery minEventAgeQuery = minEventAgePolicy.getIndexQuery();

		long timeDiff = Math.abs(maxAgeQuery.getMaxEventTime().getTime() -
				minEventAgeQuery.getMaxEventTime().getTime());

		if (timeDiff != 0L) {
			System.out.printf("Queries differed by %d milliseconds.\n",
					timeDiff);
		}

		Assert.assertTrue("queries the same", timeDiff < epsilon);
	}

	/**
	 * When time is present, it is used instead of age.
	 * 
	 * @throws Exception
	 */
	@Test
	public void ageAndTimeIgnoresAge() throws Exception {
		// set this back in time a ways
		Date maxEventTime = new Date(new Date().getTime() - 100000);

		Config config = new Config();
		config.setProperty("maxEventAge", "1");
		config.setProperty("maxEventTime",
				Long.valueOf(maxEventTime.getTime()).toString());

		ArchivePolicy policy = new ArchivePolicy();
		policy.configure(config);

		ProductIndexQuery query = policy.getIndexQuery();
		Assert.assertEquals("maxEventTime used instead of relative age",
				maxEventTime, query.getMaxEventTime());
	}

	/**
	 * If min/maxEventAge is used, minEventAge must be LT maxEventAge. An
	 * exception should be thrown if otherwise.
	 * 
	 * @throws Exception
	 */
	@Test
	public void ageSequenceThrowsException() throws Exception {
		Config config = new Config();
		config.setProperty("minEventAge", "1000");
		config.setProperty("maxEventAge", "1");

		ArchivePolicy policy = new ArchivePolicy();
		try {
			policy.configure(config);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}
	}

	/**
	 * If min/maxEventAge is used, minEventAge must be LT maxEventAge. An
	 * exception should be thrown if otherwise.
	 * 
	 */
	@Test
	public void ageSequenceSuccess() throws Exception {
		Config config2 = new Config();
		config2.setProperty("minEventAge", "1");
		config2.setProperty("maxEventAge", "1000");

		ArchivePolicy policy2 = new ArchivePolicy();
		try {
			policy2.configure(config2);
			// expected
		} catch (ConfigurationException ce) {
			Assert.fail("exception should NOT have been thrown");
			// expected
		}
	}

	/**
	 * If min/maxEventTime is used, minEventTime must be LT maxEventTime. An
	 * exception should be thrown if otherwise.
	 * 
	 * @throws Exception
	 */
	@Test
	public void timeSequenceThrowsException() throws Exception {
		Config config = new Config();
		config.setProperty("minEventTime", "1000");
		config.setProperty("maxEventTime", "1");

		ArchivePolicy policy = new ArchivePolicy();
		try {
			policy.configure(config);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}
	}

	/**
	 * If min/maxEventTime is used, minEventTime must be LT maxEventTime. An
	 * exception should be thrown if otherwise.
	 * 
	 */
	@Test
	public void timeSequenceSuccess() throws Exception {
		Config config2 = new Config();
		config2.setProperty("minEventTime", "1");
		config2.setProperty("maxEventTime", "1000");

		ArchivePolicy policy2 = new ArchivePolicy();
		try {
			policy2.configure(config2);
			// expected
		} catch (ConfigurationException ce) {
			Assert.fail("exception should NOT have been thrown");
			// expected
		}
	}

}
