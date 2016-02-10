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
	ProductId actualId = new ProductId("source", "type", "code");
	ProductId scenarioId = new ProductId("source", "type-scenario", "code"); 
	ProductId developmentId = new ProductId("source", "type-devel", "code");
	ProductId internalId = new ProductId("source", "internal-type", "code");

	@Test
	public void acceptsValidID() {
		AbstractListener listener = new AbstractListener();
		
		// Assume includeActuals = true by default
		//        includeScenarios = false by default
		//        includeDevelopment = false by default
		Assert.assertTrue("Valid Product ID accepted by default listener",
				listener.accept(actualId));
		Assert.assertFalse("Scenario Product ID refused by default listener",
				listener.accept(scenarioId) );
		Assert.assertFalse("Development Product ID refused by default listener", 
				listener.accept(developmentId) );
		Assert.assertFalse("Internal Product ID refused by default listener", 
				listener.accept(internalId) );
	}
	
	@Test
	public void acceptIncludeActuals() {
		AbstractListener listener = new AbstractListener();

		// Test includeActuals defaults to true.
		Assert.assertTrue("Valid Product ID accepted by default listener",
				listener.accept(actualId));

		// Test includeActuals set to false
		listener.setIncludeActuals(false);
		Assert.assertFalse("Actual Product ID refused by listener with includeActuals = false",
				listener.accept(actualId));
	}
	
	@Test
	public void acceptIncludeScenarios() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeScenarios defaults to false
		Assert.assertFalse("Scenario Product ID refused by default listener",
				listener.accept(scenarioId) );
		
		//Test includeScenarios set to true
		listener.setIncludeScenarios(true);
		Assert.assertTrue("Sceneario Product ID accepted by by listener with includeScenario = true", 
				listener.accept(scenarioId));
	}
	
	@Test
	public void acceptIncludeDevelopment() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeDevelopment defaults to false
		Assert.assertFalse("Development Product ID refused by default listener",
				listener.accept(developmentId) );
		
		//Test includeDevelopment set to true
		listener.setIncludeDevelopments(true);
		Assert.assertTrue("Development Product ID accepted by by listener with includeDevelopment = true", 
				listener.accept(developmentId));
	}
	
	@Test
	public void acceptIncludeInternals() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeInternals defaults to false
		Assert.assertFalse("Internals Product ID refused by default listener", 
				listener.accept(internalId));
		
		// Test incluceInternals set to true
		listener.setIncludeInternals(true);
		Assert.assertTrue("Internals Product ID accepted by listener with includeInternals = true", 
				listener.accept(internalId));
		
	}
	
	@Test
	public void acceptIncludeScenarioswithIncludeActualsOff() {
		AbstractListener listener = new AbstractListener();
		
		// Test includeActuals set to false, includeScenarios set to true
		listener.setIncludeActuals(false);
		listener.setIncludeScenarios(true);
		
		// "Normal" id should be refused.
		Assert.assertFalse("Product ID refused by listener with includeActuals = false",
				listener.accept(actualId));
		// Scenarios id will be accepted.
		Assert.assertTrue("Scenarios ID accepted by listener with includeActuals = false, includeScenarios = true",
				listener.accept(scenarioId));
	}
	
	
	@Test
	public void configuresIncludeActualsCorrectly() throws Exception {
		AbstractListener listener = new AbstractListener();
		Config config;
		
		// Test includeActuals configured to no.
		config = new Config();
		config.setProperty("includeActuals", "no");
		listener.configure(config);
		Assert.assertFalse("includeActuals configured to no ", listener.accept(actualId));
		
		// Test includeActuals configured to false.
		config = new Config();
		config.setProperty("includeActuals", "false");
		listener.configure(config);
		Assert.assertFalse("includeActuals configured to false ", listener.accept(actualId));
	}
	
}