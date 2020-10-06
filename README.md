# Vertigo

Vertigo is an earth science data viewer that reads data from NetCDF files and 
OPeNDAP connections and presents the data as coloured images on a 3D sphere.

# Screenshots

![Vertigo Beta](http://terrenus.ca/download/vertigo/beta/screen_shot.png)

# Packages

Pre-compiled installable packages are available for Windows, Mac, and Linux at 
http://terrenus.ca/download/vertigo/beta/.

# Tutorial

The [Vertigo Beta Install and Demo](https://youtu.be/Iq-7X_PFBO8) video introduces Vertigo 
Beta with a background on its development and future directions, and example installation
procedure and usage.

# Development

#### Required:
* OpenJDK 14 (http://openjdk.java.net)
* OpenJFX 14 (https://openjfx.io)
* Apache ant 1.10

#### Optional:
* install4j 8 (https://www.ej-technologies.com)

#### Build and run:
* Adjust paths in build.xml for JavaFX install directory (both modules and SDK)
* Type `ant` to build
* Type `ant test-netcdf` to test with a demo NetCDF file
* Type `ant test-webmap` to test with a demo tiled web map
* Type `ant test-project` to test with a demo project file

# Support

General comments and questions should be directed to peter@terrenus.ca and 
coastwatch.info@noaa.gov.
