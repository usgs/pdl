/**
 * Extent Indexer Listener
 */

package gov.usgs.earthquake.indexer;

import gov.usgs.util.Config;

public class ExtentIndexerListener extends DefaultIndexerListener implements IndexerListener {

  //Constructor
  //Just uses super configure for now
  public ExtentIndexerListener() {
    super();
  }

  //Sets up object on start
  //Will set up thread and then tell it to chill (probably)
  //For now just using super configure
  //public void configure(Config config) throws Exception {  }

  //Called when indexer does something
  //Will wake our thread up as long as we're not busy processing still
  public void onIndexerEvent(IndexerEvent delta) throws Exception {

  }

  //Max number of tries indexer gives listener to process
  //(For if listener tosses exception during processing)
	public int getMaxTries() {
    return 0;
  }

  //Number of milliseconds given by indexer before timing out
  //Not going to be super long because we will be using a thread
	public long getTimeout() {
    return 0;
  }
   
 }