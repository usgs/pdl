package gov.usgs.earthquake.aws;

import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.earthquake.distribution.ExecutorListenerNotifier;

public class TestListenerNotifier extends ExecutorListenerNotifier {

  // override queueSize for testing
  public Integer queueSize = null;

  public TestListenerNotifier(DefaultNotificationReceiver receiver) {
		super(receiver);
	}

  @Override
  public Integer getMaxQueueSize() {
    if (queueSize != null) {
      return queueSize;
    }
    return super.getMaxQueueSize();
  }

}
