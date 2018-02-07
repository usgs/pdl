#! /usr/bin/env node
/**
 * Script to generate regions.xml style coordinates for regions.xml
 * from the geoserve web service, which is the authoritative source
 * for authoritative polygon information.
 * 
 * Prints Network code, followed by coordinates for polygon.
 * Assumes geoserve web service outputs coordinates clockwise,
 * and reverses to print counter-clockwise as regions originally
 * appeared to be output.
 * 
 * Run:
 *         node get_geoserve_regions.js
 *
 * @author jmfee@usgs.gov
 * @version 2018-01-25
 */
"use strict";
const https = require("https");


// URL to fetch geoserve authoritative regions
const AUTHORITATIVE_URL = "https://earthquake.usgs.gov/ws/geoserve/layers.json?type=authoritative";



/**
 * Fetch a URL (https of course) and return a promise.
 * @param {String} url url to fetch
 * @returns {Promise} that resolves with response body,
 *                    or rejects with stream error.
 */
function getUrl(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            let body = "";
            res.on("data", (data) => {
                body += data;
            });
            res.on("error", (e) => {
                reject(e);
            });
            res.on("end", () => {
                resolve(body);
            });
        });
    });
}

/**
 * Round a number to the specified precision.
 * @param {Number} num number to round
 * @param {Integer} decimals number of decimals to round
 */
function round(num, decimals) {
    let scale = Math.pow(10, decimals);
    let rounded = Math.round(num * scale) / scale;
    return rounded;
}

/**
 * Output region name and coordinate elements.
 * Assumes coordinates are of type Polygon.
 * @param {Object} region authoritative region geojson feature.
 */
function showRegion(region) {
    process.stderr.write(region.properties.network + "\n");
    // polygon outer ring is 0th ring in coordinates
    let coords = region.geometry.coordinates[0];
    // geoserve outputs counterclockwise
    coords = coords.reverse();

    coords.forEach((coord) => {
        let latitude = round(coord[1], 4).toFixed(4);
        let longitude = round(coord[0], 4).toFixed(4);
        process.stderr.write(`\t\t\t<coordinate latitude="${latitude}" longitude="${longitude}"/>\n`);
    });
    process.stderr.write("\n");
}

// fetch and format the regions
getUrl(AUTHORITATIVE_URL).then((data) => {
    return JSON.parse(data);
}).then((data) => {
    let regions = data.authoritative.features;
    // sort by network code
    regions.sort(function(a, b) {
        let anet = a.properties.network;
        let bnet = b.properties.network;
        return (anet < bnet) ? -1 : (anet === bnet) ? 0 : 1;
    });
    regions.forEach(showRegion);
}).catch((err) => {
    process.stderr.write(err);
});
