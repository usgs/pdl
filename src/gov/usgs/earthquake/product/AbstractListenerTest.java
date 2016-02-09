/*
 * AbstractListener.java
 * 
 */
package gov.usgs.earthquake.product;

import org.junit.Test;
import org.junit.Assert;


/**
 * Test the AbstractListener classes acceptance method.
 * 
 */
public class AbstractListenerTest {
	@Test
	public void acceptsValidID() {
		AbstractListener listener = new AbstractListener();
		ProductId id = new ProductId("source", "type", "code");
		ProductId scenario_id = new ProductId("source", "type-scenario", "code"); 
		ProductId development_id = new ProductId("source", "type-devel", "code");
		ProductId internal_id = new ProductId("source", "internal-type", "code");
		
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
		
		// Set includeActuals to false
		// Leave includeScenarios and includeDevelopment defaulted to false.
		listener.setIncludeActuals(false);
		Assert.assertFalse("Actual Product ID refused by listener with Actuals = false",
				listener.accept(id));
		Assert.assertFalse("Scenario Product ID refused by listener with Actuals = false, Scenarios = default",
				listener.accept(scenario_id));
		Assert.assertFalse("Development Product ID refused by listener with Actuals = false, development=default",
				listener.accept(development_id));
		Assert.assertFalse("Internal Product ID refused by listener with Actuals = false, internals=default",
				listener.accept(internal_id));
		
		// Set includeScenarios to true while includeActuals set to false
		listener.setIncludeScenarios(true);
		Assert.assertFalse("Actual Product Id refused by listener with Actuals = false, Scenarios = true",
				listener.accept(id));
		Assert.assertTrue("Scenario Product ID accepted by listener with Actuals = false, Scenarios = true",
				listener.accept(scenario_id));
		
		// Set includeDevelopments to true while includeActuals set to false
		listener.setIncludeDevelopments(true);
		Assert.assertFalse("Actual Product ID refused by listener with Actuals = false, Development = true",
				listener.accept(id));
		Assert.assertTrue("Development Product ID accepted by listener with Actuals = false, Development = true", 
				listener.accept(development_id));
		
		// Set includeInternals to true while includeActuals set to false
		listener.setIncludeInternals(true);
		Assert.assertFalse("Actual Product ID refused by listener with Actuals = false, Internal = true",
				listener.accept(id));
		Assert.assertTrue("Internal Product ID accepted by listener with Actuals = false, Internal = true", 
				listener.accept(internal_id));
	}
	
}