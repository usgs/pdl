package gov.usgs.earthquake.indexer;

import java.util.Date;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.util.Config;

import org.junit.Assert;
import org.junit.Test;

public class ProductArchivePolicyTest {

	/**
	 * Using the deprecated min/maxAge property in combination with the new
	 * min/maxProductUpdateTime propertys should throw a
	 * gov.usgs.earthquake.distribution.ConfigurationException.
	 * 
	 * @throws Exception
	 */
	@Test
	public void mixingConfigurationsThrowsException() throws Exception {
		Config config = new Config();
		config.setProperty("minAge", "1");
		config.setProperty("maxProductAge", "1");

		ProductArchivePolicy policy = new ProductArchivePolicy();
		try {
			policy.configure(config);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}

		Config config2 = new Config();
		config2.setProperty("maxAge", "1");
		config2.setProperty("minProductTime", "1");

		ProductArchivePolicy policy2 = new ProductArchivePolicy();
		try {
			policy2.configure(config2);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}

	}

	/**
	 * The deprecated minAge parameter is equivalent to its replacement
	 * maxProductAge. The resulting product index query objects should be equal.
	 * 
	 * @throws Exception
	 */
	@Test
	public void minAgeSameAsMaxProductAge() throws Exception {
		long epsilon = 500L;

		Config minAgeConfig = new Config();
		minAgeConfig.setProperty("minAge", "1");
		ProductArchivePolicy minAgePolicy = new ProductArchivePolicy();
		minAgePolicy.configure(minAgeConfig);

		Config maxProductAgeConfig = new Config();
		maxProductAgeConfig.setProperty("maxProductAge", "1");
		ProductArchivePolicy maxProductAgePolicy = new ProductArchivePolicy();
		maxProductAgePolicy.configure(maxProductAgeConfig);

		ProductIndexQuery minAgeQuery = minAgePolicy.getIndexQuery();
		ProductIndexQuery maxProductAgeQuery = maxProductAgePolicy
				.getIndexQuery();
		
		long timeDiff = Math.abs(minAgeQuery.getMinProductUpdateTime().getTime() -
				maxProductAgeQuery.getMinProductUpdateTime().getTime());

		
		if (timeDiff != 0L) {
			System.out.printf("Queries differed by %d milliseconds.\n",
					timeDiff);
		}

		Assert.assertTrue("queries the same", timeDiff < epsilon);		

	}

	/**
	 * The deprecated maxAge parameter is equivalent to its replacement
	 * minProductAge. The resulting product index query objects should be equal.
	 * 
	 * @throws Exception
	 */
	@Test
	public void maxAgeSameAsMinProductAge() throws Exception {
		long epsilon = 500L;

		Config maxAgeConfig = new Config();
		maxAgeConfig.setProperty("maxAge", "1");
		ProductArchivePolicy maxAgePolicy = new ProductArchivePolicy();
		maxAgePolicy.configure(maxAgeConfig);

		Config minProductAgeConfig = new Config();
		minProductAgeConfig.setProperty("minProductAge", "1");
		ProductArchivePolicy minProductAgePolicy = new ProductArchivePolicy();
		minProductAgePolicy.configure(minProductAgeConfig);

		ProductIndexQuery maxAgeQuery = maxAgePolicy.getIndexQuery();
		ProductIndexQuery minProductAgeQuery = minProductAgePolicy
				.getIndexQuery();

		
		long timeDiff = Math.abs(maxAgeQuery.getMaxProductUpdateTime().getTime() -
				minProductAgeQuery.getMaxProductUpdateTime().getTime());

		if (timeDiff != 0L) {
			System.out.printf("Queries differed by %d milliseconds.\n",
					timeDiff);
		}

		Assert.assertTrue("queries the same", timeDiff < epsilon);		
		
	}

	/**
	 * When configuring min/maxProductAge and min/maxProductUpdateTime, the
	 * relative age setting is ignored in favor of the fixed updateTime setting.
	 * 
	 * @throws Exception
	 */
	@Test
	public void ageAndTimeIgnoresAge() throws Exception {

		Date maxProductTime = new Date(new Date().getTime() - 100000);

		Config config = new Config();
		config.setProperty("maxProductAge", "1");
		config.setProperty("maxProductTime",
				Long.valueOf(maxProductTime.getTime()).toString());

		ProductArchivePolicy policy = new ProductArchivePolicy();
		policy.configure(config);

		ProductIndexQuery query = policy.getIndexQuery();
		Assert.assertEquals("maxProductTime used instead of relative age",
				maxProductTime, query.getMaxProductUpdateTime());
	}

	/**
	 * If min/maxProductAge is used, minProductAge must be LT maxProductAge. An
	 * exception should be thrown if otherwise.
	 * 
	 * @throws Exception
	 */
	@Test
	public void ageSequenceThrowsException() throws Exception {
		Config config = new Config();
		config.setProperty("minProductAge", "1000");
		config.setProperty("maxProductAge", "1");

		ProductArchivePolicy policy = new ProductArchivePolicy();
		try {
			policy.configure(config);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}

	}

	/**
	 * If min/maxProductAge is used, minProductAge must be LT maxProductAge. An
	 * exception should be thrown if otherwise.
	 * 
	 */
	@Test
	public void ageSequenceSuccess() throws Exception {

		Config config2 = new Config();
		config2.setProperty("minProductAge", "1");
		config2.setProperty("maxProductAge", "1000");

		ProductArchivePolicy policy2 = new ProductArchivePolicy();
		try {
			policy2.configure(config2);
			// expected
		} catch (ConfigurationException ce) {
			Assert.fail("exception should NOT have been thrown");
		}

	}

	/**
	 * If min/maxProductTime is used, minProductTime must be LT maxProductTime.
	 * An exception should be thrown if otherwise.
	 * 
	 * @throws Exception
	 */
	@Test
	public void timeSequenceThrowsException() throws Exception {
		Config config = new Config();
		config.setProperty("minProductTime", "1000");
		config.setProperty("maxProductTime", "1");

		ProductArchivePolicy policy = new ProductArchivePolicy();
		try {
			policy.configure(config);
			Assert.fail("exception should have been thrown");
		} catch (ConfigurationException ce) {
			// expected
		}

	}

	/**
	 * If min/maxProductTime is used, minProductTime must be LT maxProductTime.
	 * An exception should be thrown if otherwise.
	 * 
	 */
	@Test
	public void timeSequenceSuccess() throws Exception {

		Config config2 = new Config();
		config2.setProperty("minProductTime", "1");
		config2.setProperty("maxProductTime", "1000");

		ProductArchivePolicy policy2 = new ProductArchivePolicy();
		try {
			policy2.configure(config2);
			// expected
		} catch (ConfigurationException ce) {
			Assert.fail("exception should NOT have been thrown");
		}

	}

}
