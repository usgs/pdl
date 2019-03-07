# ANSS Authoritative Regions

ANSS Authoritative Regions define the boundaries for each regional network.

The regions are published via ScienceBase, and are also available through the Geoserve Web Service in GeoJSON format:

- [View Interactive Map](https://earthquake.usgs.gov/geoserve/) - select the `ANSS Authoritative Regions` layer.
- [Download Regions as GeoJSON](https://earthquake.usgs.gov/ws/geoserve/layers.json?type=anss)
- [ScienceBase - Geoserve Region Data](https://www.sciencebase.gov/catalog/item/5a6f547de4b06e28e9caca43)


# Regions XML

Regions were previously managed in an XML file `regions.xml`,
but have since moved to the geoserve web service.
PDL can generate similar output with network coordinates.

```
java -jar ProductClient.jar gov.usgs.earthquake.geoserve.RegionsXML > regions.xml
```

# Regions KML

PDL can also generate a KML version of network coordinate information:

```
java -jar ProductClient.jar gov.usgs.earthquake.geoserve.RegionsKML > regions.kml
```