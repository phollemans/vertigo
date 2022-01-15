# Vertigo Project

The CoastWatch Vertigo Project is an earth science data viewing application that reads
data from NetCDF files and OPeNDAP connections and presents the data as coloured 
images on a 3D sphere.  The goal of the project is to create a way to discover, view, 
and use earth science data that is easy and intuitive.

# Screenshots

![Vertigo Screenshot - Chlorophyll](http://terrenus.ca/download/vertigo/0.6/screenshot_01.png)
![Vertigo Screenshot - Air Temperature](http://terrenus.ca/download/vertigo/0.6/screenshot_02.png)
![Vertigo Screenshot - True Color](http://terrenus.ca/download/vertigo/0.6/screenshot_03.png)
![Vertigo Screenshot - Geo Color](http://terrenus.ca/download/vertigo/0.6/screenshot_04.png)
![Vertigo Screenshot - Sea Surface Temperature](http://terrenus.ca/download/vertigo/0.6/screenshot_05.png)
![Vertigo Screenshot - Ice Cover](http://terrenus.ca/download/vertigo/0.6/screenshot_06.png)

# Packages

Pre-compiled installable packages starting with version 0.7 are available for 
Windows, Mac, and Linux under [Releases](https://github.com/phollemans/vertigo/releases). Packages are also 
available from the Terrenus [archive](http://terrenus.ca/download/vertigo).

# Videos 

The YouTube [CoastWatch Vertigo Project](https://www.youtube.com/playlist?list=PL_-bsOLKMYJxlOTJn6E_EUvjBJtSwzYir) playlist contains videos 
on installing and using Vertigo to view data, and on Vertigo development.

# Running

To run Vertigo, either install one of the packages listed above which may require
administrator access to your machine, or follow the instructions below for installing OpenJDK and setting `JAVA_HOME`, then skip directly to Step (4) to run Vertigo using the provided script.  

# Building

#### Required software:

* OpenJDK 14.0.2 (https://jdk.java.net/archive) -- This can be installed either system-wide
or in a local home directory.

#### Optional software:

* install4j 8 (https://www.ej-technologies.com) -- This is to create and sign installable 
packages.

#### Steps:

1) Download the project ZIP file, or clone the repository using Git.

2) Set `JAVA_HOME` to the base JDK directory, for example:
    - Linux / macOS: `export JAVA_HOME=${HOME}/jdk-14.0.2`
    - Windows: `set JAVA_HOME=C:\Users\%USERNAME%\jdk-14.0.2`

3) To build a runnable distribution file with all dependencies included, use either the 
`distTar` or `distZip` Gradle tasks.  By default the distribution is built only for the 
current platform -- to build for another platform specify 
`-Pplatform=win`, `-Pplatform=mac`, or `-Pplatform=linux`.  After building, the 
distribution file is available in the `vertigo/build/distributions` directory.  For 
example:
    - Linux / macOS: `./gradlew distTar`
    - Windows: `.\gradlew distZip`

4) Alternatively, to compile and run Vertigo directly on the current platform:
    - Linux / macOS: `./gradlew run`
    - Windows: `.\gradlew run`

# Support

General comments and questions should be directed to peter@terrenus.ca and 
coastwatch.info@noaa.gov.
