/*
 * AbstractListener.java
 * 
 */
package gov.usgs.earthquake.product;

import org.junit.Test;
import org.junit.Assert;

import gov.usgs.util.Config;


/**
 * Test the AbstractListener classes acceptance and config methods use of includeActuals.
 * 
 */
public class AbstractListenerTest {
	ProductId id = new ProductId("source", "type", "code");
	ProductId scenario_id = new ProductId("source", "type-scenario", "code"); 
	ProductId development_id = new ProductId("source", "type-devel", "code");
	ProductId internal_id = new ProductId("source", "internal-type", "code");

	@Test
	public void acceptsValidID() {
		AbstractListener listener = new AbstractListener();
		
		// Assume includeActuals = true by default
		//        includeScenarios = false by default
		//        includeDevelopment = false by default
		Assert.assertTrue("Valid Product ID accepted by default listener",
				listener.accept(id));
		Assert.assertFalse("Scenario Product ID refused by default listener",
				listener.accept(scenario_id) );
		Assert.assertFalse("Development Product ID refused by default listener", 
				listener.accept(development_id) );
		Assert.assertFalse("Internal Product ID refused by default listener", 
				listener.accept(internal_id) );
	}
	
	@Test
	public void acceptIncludeActuals() {
		AbstractListener listener = new AbstractListener();

		// Test includeActuals defaults to true.
		Assert.assertTrue("Valid Product ID accepted by default listener",
				listener.accept(id));

		// Test includeActuals set to false
		listener.setIncludeActuals(false);
		Assert.assertFalse("Actual Product ID refused by listener with includeActuals = false",
				listener.accept(id));
	}
	
	@Test
	public void acceptIncludeScenarios() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeScenarios defaults to false
		Assert.assertFalse("Scenario Product ID refused by default listener",
				listener.accept(scenario_id) );
		
		//Test includeScenarios set to true
		listener.setIncludeScenarios(true);
		Assert.assertTrue("Sceneario Product ID accepted by by listener with includeScenario = true", 
				listener.accept(scenario_id));
	}
	
	@Test
	public void acceptIncludeDevelopment() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeDevelopment defaults to false
		Assert.assertFalse("Development Product ID refused by default listener",
				listener.accept(development_id) );
		
		//Test includeDevelopment set to true
		listener.setIncludeDevelopments(true);
		Assert.assertTrue("Development Product ID accepted by by listener with includeDevelopment = true", 
				listener.accept(development_id));
	}
	
	@Test
	public void acceptIncludeInternals() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeInternals defaults to false
		Assert.assertFalse("Internals Product ID refused by default listener", 
				listener.accept(internal_id));
		
		// Test incluceInternals set to true
		listener.setIncludeInternals(true);
		Assert.assertTrue("Internals Product ID accepted by listener with includeInternals = true", 
				listener.accept(internal_id));
		
	}
	
	@Test
	public void acceptIncludeScenarioswithIncludeActualsOff() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeActuals set to false, includeScenarios set to true
		listener.setIncludeActuals(false);
		listener.setIncludeScenarios(true);
		
		// "Normal" id should be refused.
		Assert.assertFalse("Product ID refused by listener with includeActuals = false",
				listener.accept(id));
		// Scenarios id will be accepted.
		Assert.assertTrue("Scenarios ID accepted by listener with includeActuals = false, includeScenarios = true",
				listener.accept(scenario_id));
	}
	
	
	@Test
	public void configuresCorrectly() {
		AbstractListener listener = new AbstractListener();
		Config config;
		
		// Test includeActuals configured to no.
		config = new Config();
		config.setProperty("includeActuals", "no");
		try {
			listener.configure(config);
		} catch(Exception e) {
			System.out.println(e);
			Assert.assertFalse("Exception thrown on configure", true);
		}
		Assert.assertFalse("includeActuals configured to no ", listener.accept(id));
		
		
		// Test includeActuals configured to false.
		config = new Config();
		config.setProperty("includeActuals", "false");
		try {
			listener.configure(config);
		} catch(Exception e) {
			System.out.println(e);
			Assert.assertFalse("Exception thrown on configure", true);
		}
		Assert.assertFalse("includeActuals configured to false ", listener.accept(id));
	}
	
}