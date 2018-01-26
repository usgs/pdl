#! /usr/bin/env python
"""
Script to convert regions.xml to KML.

Reads xml from stdin, writes kml to stdout.


Usage:
        cat regions.xml | ./regions_to_kml.py > regions.kml

@author jmfee@usgs.gov
@version 2018-01-26
"""


import sys
import xml.parsers.expat as expat


KML_TEMPLATE = """\
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
<Document id="regions_xml">
    <name>ANSS Authoritative Regions</name>
    <Style id="RegionStyle">
        <LabelStyle>
        <color>00000000</color>
        <scale>0.000000</scale>
        </LabelStyle>
        <LineStyle>
        <color>ff0000ff</color>
        <width>2.000000</width>
        </LineStyle>
        <PolyStyle>
        <color>00ffffff</color>
        <outline>1</outline>
        </PolyStyle>
    </Style>
    <Folder id="regions">
        <name>{{UPDATED}}</name>
        {{REGIONS}}
  </Folder>
</Document>
</kml>
"""

PLACEMARK_TEMPLATE = """
<Placemark id="region_{{ID}}">
    <name>{{ID}}</name>
    <styleUrl>#RegionStyle</styleUrl>
    <MultiGeometry><Polygon><outerBoundaryIs><LinearRing><coordinates>
    {{COORDS}}
    </coordinates></LinearRing></outerBoundaryIs></Polygon></MultiGeometry>
</Placemark>
"""

def format_regions_kml(regions_xml):
    placemarks = []
    for region in regions_xml["regions"]:
        placemark = PLACEMARK_TEMPLATE
        placemark = placemark.replace("{{ID}}", region["code"])
        placemark = placemark.replace("{{COORDS}}",
                # coords in lon,lat order
                '\n    '.join([','.join(c) for c in region["coords"]]))
        placemarks.append(placemark)
    kml = KML_TEMPLATE
    kml = kml.replace("{{REGIONS}}", ''.join(placemarks))
    kml = kml.replace("{{UPDATED}}", regions_xml["updated"])
    return kml
    

def parse_regions(file_like):
    """Parse Regions XML from a file-like object."""
    regions = {
        "updated": None,
        "regions": []
    }
    # use dict to workaround python2 closure limitations
    region = {}
    def start_element(name, attrs):
        if name == "region":
            region["region"] = {
                "code": attrs["code"],
                "coords": []
            }
        elif name == "coordinate":
            region["region"]["coords"].append([
                attrs["longitude"],
                attrs["latitude"],
            ])
        elif name == "update":
            regions["updated"] = attrs["date"]
    def end_element(name):
        if name == "region" and len(region["region"]["coords"]) > 0:
            regions["regions"].append(region["region"])
    p = expat.ParserCreate()
    p.StartElementHandler = start_element
    p.EndElementHandler = end_element
    p.ParseFile(file_like)
    return regions



if __name__ == "__main__":
    regions_xml = parse_regions(sys.stdin)
    print(format_regions_kml(regions_xml))
