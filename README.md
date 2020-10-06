# Vertigo

Vertigo is an earth science data viewer that reads data from NetCDF files and 
OPeNDAP connections and presents the data as coloured images on a 3D sphere.

# Development

REQUIRED:
* OpenJDK 14 (http://openjdk.java.net)
* OpenJFX 14 (https://openjfx.io)
* Apache ant 1.10

OPTIONAL:
* install4j 8 (https://www.ej-technologies.com)

BUILD AND RUN:
* Adjust paths in build.xml for JavaFX install directory (both modules and SDK)
* Type `ant`
* Type `ant test-netcdf` to test with a demo NetCDF file
* Type `ant test-webmap` to test with a demo tiled web map
* Type `ant test-project` to test with a demo project file

# Support

General comments and questions should be directed to coastwatch.info@noaa.gov.
