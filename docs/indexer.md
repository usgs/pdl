Indexer
=======


## Overview

The Indexer receives products via PDL and Associates related products into a Catalog of Events.  Related products are associated using Event ID and Location information.  When new information is added, Listeners are notified about how the catalog was changed.


## Concepts

### Product ID

Each PDL Product has a unique identifier called a `Product ID`, which consists of 4 logical values.  The Product ID is used to detect identical information delivered via redundant paths, so it is only processed once.

- `source` (aka Contributor)

  A unique identifier for an entity sending data.

- `type`

  The type of data being sent.

- `code`

  A unique identifier (when combined with `source` and `type`) for a specific product.
  
  > `source`, `type`, and `code` are reused when sending updates to existing inforation (see `updateTime`).

- `updateTime`

  Time when information was created (usually).
  
  Update time is used by PDL as a version number.  Two products with the same `source`, `type`, and `code` (and different `updateTime`) are considered to be two versions of the same product.  The version of the product with the more recent update time **supersedes** any older version of the same product.

### Product Status

Each version of a product has a `status`.  The only reserved status is `DELETE`, which indicates a product has been deleted.  All other statuses indicate a product is being updated.

PDL never removes information, instead an updated product with the status `DELETE` is sent and supersedes any earlier versions of that same product.

### Product Properties

Product properties are used to store basic metadata about a product; each property name has only one value.  The Indexer uses properties to encode Event ID and Location information.

#### Event ID

A unique identifier for an Event.  Products optionally include an Event ID using the following properties:

- `eventsource` (aka Catalog)

  A unique identifier for a collection of events.

- `eventsourcecode`

  A unique identifer (when combined with `eventsource`) for an event.

  Two separate `eventsourcecode`s from the same `eventsource` are considered distinct events and will not automatically associate.

> *CAUTION*: Product IDs are unique identifiers for *data*, while Event IDs are unique identifiers for *events*.  Each event is made up of multiple products, and while the values are often similar they have different meaning.

#### Event Location

The location of an event in space and time.

- `eventtime`

  time of event (ISO8601 date and time with timezone)

- `latitude`

  latitude of event (WGS84 decimal degrees)

- `longitude`

  longitude of event (WGS84 decimal degrees)


## Process


### 1) Store Product

The indexer stores product information in a `FileProductStorage`, typically using a directory.  Contents are stored separate from other metadata like Properties, and Links.


### 2) Use Module to Summarize Product

The indexer uses one or more Modules to summarize products.  Modules calculate `Preferred Weight`, and can optionally add additional summary information from product contents or other sources.

#### Default Indexer Module
The Default Indexer Module is the basis for all other Modules and implements ANSS Authoritative Regions using the following scheme:

| Value | Description
| :---: | -----------
| +1    | Initial Value
| +5    | If product `source` is the same as `eventsource`
| +50   | If product has *Location*, and `eventsource` is authoritative
| +100  | If product has *Location*, and `source` is authoritative


### 3) Search for associated information

#### Find Previous Version of Product

- Using Product ID

#### Find Existing Event

- Using Event ID

- Using Location (if not found using Event ID)

  Within 16 seconds and ~100 km.

  If multiple events are found, the closest event is chosen based on the distance between events in space and time:

  ```
  latDiff = lat1-lat2 / 100km
  lngDiff = lng1-lng2 / 100km
  timeDiff = time1-time2 / 16s
  distance = sqrt(latdiff^2 + lngdiff^2 + timediff^2)
  ```

If no event is found based on the previous criteria, the Indexer does additional searches

- Based on association of previous version of product

- Based on link to product ID embedded in `trump` product

If multiple candidate events are found (based on Location), the closest event is selected the L2-norm of normalized differences in latitude, longitude, and event time


### 4) Associate to Event

If no event was found in step #3, and the incoming product does not have both Event ID and Location, the product is considered **Unassociated** association stops.

If not event was found in step #3, and the incoming product does have both Event ID and Location, a new Event is created and the product is associated.

Otherwise the product is associated to the existing event.


### 5) Re-evaluate Existing Associations

> NOTE: This only occurs if the product associated to an existing event.

#### Check for Event Splits

Now that a product has been added to the event, it's possible that associations within existing information may no longer be valid.

The event is divided into Sub-Events based on Event ID.  Products with no Event ID are grouped with the currently Preferred Event ID.  Associations are compared between all Sub-Events (see Events Associated below) and if a Sub-Event no longer associates it is Split into a separate event.

If events are split this generates an **Event Split** indexer action for any split events, which indicates and event was added to the index because it no longer associates with another event.  An additional **Event Updated** indexer action follows any **Event Split** actions, indicating the event from which the other events split.

#### Check for Event Merges

> NOTE: This only occurs is the product associated to an existing event or created a new event.

##### Check for Events that can now associate

If the product associated to an existing event, it's possible it may cause the event to associate with other existing events.  The Indexer searches for nearby events using the Preferred Location, and checks whether the events can be associated (see Events Associated below).

If events are associated this generates an **Event Merged** indexer action for any merged events, which indicates an event was removed from the index because it merged with another event.  An additional **Event Updated** indexer action follows any **Event Merged** actions, indicating the event to which the other events merged.

##### Check for previously unassociated products

Search for products using the same Event ID that have not previously associated.  This typically occurs when an event is first created, because a product with both Event ID and Location has just been received.

##### Check for merge by `associate` product

If an associate product was just added, attempt to merge with the target event.


### 6) Check for Trump products

Trump products are used to override the automatically calculated Preferred Weight; and can be thought of as similar to playing card games.

#### Version-Specific Trump

A version-specific trump product (type `trump`) changes the preferred weight of a specific version of a product.  This change is made when the product is added to the index.

#### Persistent Trump

A persistent trump product (type `trump-TYPE`, where `TYPE` is the type of another associated product) changes the preferred weight of _all_ versions of a product.  This change is applied when the persistent trump is added to the index, and when a new matching product is added to the index.


### 7) Update Preferred Event Information in Product Index

The Indexer stores a summary of preferred event information including Event ID, Latitude, Longitude, Time, Magnitude, Depth, and Status in the Product Index.  Any events that were updated during the processing of the incoming product are also updated in the Index.

#### Preferred Product

For each type of information associated to an event, the Indexer orders products based on preferred weight.  When two associated products have equal preferred weights, the most recently updated product is considered preferred.

#### Origin Product

The Event ID and Location come from the preferred origin product.  This is either the most preferred `origin` type product, or if there are no origin products, the most preferred product with both Event ID and Location properties.

#### Magnitude Product

This is currently the same as the Origin product.

#### Deleted Events

If an event has any `origin` type products, the event is considered deleted when all `origin` type products are deleted. Otherwise, the event is considered deleted when there is no longer a origin product (product with Event ID and Location).


## Events Associated

This process compares two events to determine whether they can be associated, and is primarily used during *Check For Event Merges* and *Check For Event Splits*.

1. If either event has a `diassociate` product for the other event, the events are NOT associated.

1. If either event has an `associate` product for the other event, the events are associated.

1. If preferred Event IDs are the same, the events are associated.

1. If preferred Event IDs are not the same but are from same Event Source, the events are NOT associated.

1. Compare all associated Event IDs (except deleted Event IDs).  If there are different Event IDs from the same event source, the events are NOT associated.

1. If Events are within 16 seconds and ~100km, the events are associated.

1. Otherwise, the events are NOT associated.

> NOTE: the `associate` and `diassociate` product checks currently only check the preferred event ID of the other event.
