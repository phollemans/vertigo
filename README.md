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

Pre-compiled installable packages are available for Windows, Mac, and Linux:

| Operating System | MD5 Code |
| :--- | :--- |
| [Windows](http://terrenus.ca/download/vertigo/0.6/vertigo_windows-x64_0_6.exe) | eb5457ff92450e4ec2f563d2f32ab3d1 |
| [macOS](http://terrenus.ca/download/vertigo/0.6/vertigo_macos_0_6.dmg) | 19cc5f9e758c916e7fb47bed55e63775 |
| [Linux (tar.gz)](http://terrenus.ca/download/vertigo/0.6/vertigo_linux_0_6.tar.gz) | 8c2a79f0555261eef3229678c3b5f418 |
| [Linux (deb)](http://terrenus.ca/download/vertigo/0.6/vertigo_linux_0_6.deb) | d2dc78f969aa8dfad96339739869ee79 |

* [vertigo_windows-x64_0_6.exe](http://terrenus.ca/download/vertigo/0.6/vertigo_windows-x64_0_6.exe)
* [vertigo_macos_0_6.dmg](http://terrenus.ca/download/vertigo/0.6/vertigo_macos_0_6.dmg)
* [vertigo_linux_0_6.deb](http://terrenus.ca/download/vertigo/0.6/vertigo_linux_0_6.deb)
* [vertigo_linux_0_6.tar.gz](http://terrenus.ca/download/vertigo/0.6/vertigo_linux_0_6.tar.gz)
* [MD5 codes](http://terrenus.ca/download/vertigo/0.6/md5sums)
* [SHA256 codes](http://terrenus.ca/download/vertigo/0.6/sha256sums)

# Tutorials

The YouTube [CoastWatch Vertigo Project](https://www.youtube.com/watch?v=Iq-7X_PFBO8&list=PL_-bsOLKMYJxlOTJn6E_EUvjBJtSwzYir) playlist contains videos 
on installing and using Vertigo to view data.

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
* Type `ant test-project -Dproject='file:myproject.vrtx'` to test with a 
user-specified project file
* Type `ant test-demo` to test with a built-in demo project file

# Support

General comments and questions should be directed to peter@terrenus.ca and 
coastwatch.info@noaa.gov.
