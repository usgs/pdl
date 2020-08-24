package gov.usgs.earthquake.aws;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import gov.usgs.earthquake.indexer.Event;
import gov.usgs.earthquake.indexer.EventSummary;
import gov.usgs.earthquake.indexer.IndexerChange;
import gov.usgs.earthquake.indexer.IndexerEvent;


public class IndexerEventJson {

  public JsonObject getJsonObject(final IndexerEvent event) throws Exception {
    JsonArrayBuilder changes = Json.createArrayBuilder();
    for (final IndexerChange change : event.getIndexerChanges()) {
      changes.add(this.getJsonChange(change));
    }
    return Json.createObjectBuilder()
        .add("changes", changes)
        .add("product", new ProductJson().getJsonObject(event.getProduct()))
        .build();
  }

  public JsonObjectBuilder getJsonChange(final IndexerChange change) throws Exception {
    return Json.createObjectBuilder()
        .add("action", change.getType().toString())
        .add("event", this.getJsonEvent(change.getNewEvent()))
        .add("oldEvent", this.getJsonEvent(change.getOriginalEvent()));
  }

  public JsonObjectBuilder getJsonEvent(final Event event) throws Exception {
    if (event == null) {
      return null;
    }

    EventSummary summary = event.getEventSummary();
    return Json.createObjectBuilder()
        .add("id", summary.getId())
        .add("geometry",
            Json.createObjectBuilder()
                .add("type", "Point")
                .add("coordinates",
                    Json.createArrayBuilder()
                        .add(summary.getLongitude())
                        .add(summary.getLatitude())
                        .add(summary.getDepth())))
        .add("properties",
            Json.createObjectBuilder()
                .add("magnitude", summary.getMagnitude())
                .add("net", summary.getSource())
                .add("netid", summary.getSourceCode())
                .add("status", event.isDeleted()));
  }

}
