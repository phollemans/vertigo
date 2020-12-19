Vertigo Project v0.6 beta\
Copyright (c) 2020 National Oceanic and Atmospheric Administration\
All rights reserved\
Author: Peter Hollemans

## Introduction

The Vertigo Project uses JavaFX 3D rendering to display earth science datasets
provided by the NOAA CoastWatch Program (https://coastwatch.noaa.gov) and
others.  Datasets are accessed in real-time from NOAA data servers.

The Vertigo Project has a number of goals:

* To evaluate the JavaFX technology for creating interactive data viewing applications for
NOAA CoastWatch users.

* To experiment with fast download and rendering of datasets from network servers.

* To create a simple interface for the ERDDAP and THREDDS data services,
allowing for easy dataset discovery, visualization, and access.

## Usage

### Datasets

The Vertigo Project application launches with a demo project that shows a
number of datasets and areas on the left side.  Select a dataset to view,
or double-click an area to shift the view.  The globe shows the new dataset
and can automatically select the date and time closest to the last timestep
being viewed.  The timestep can be manually selected as well using the
`Date and time` box in the controls.  Some datasets have multiple levels,
which can also be selected.

### View Controls

The mouse and/or trackpad are used to control the view position:

* Scroll up to zoom in, or down to zoom out
* Click and drag on the globe to rotate it
* Double-click the left mouse button to zoom in and rotate to that location
* Double-click the right mouse button to zoom out and rotate to that location
* Pinch and zoom on a trackpad or touchscreen to zoom in and out
* Click the arrows at the top-right to rotate in increments
* Use the zoom slider to set the zoom level

There are also several `View` menu shortcuts for:

* Controlling the view position and zoom
* Setting the view size to standard aspect ratios
* Entering and exiting full screen mode
* Showing and hiding the graticule

### Snapshots

The `File` menu contains a snapshot option -- use this to save an image of the
current display.  The default format is JPEG, but different formats can be written by
changing the file ending to `tif`, `bmp`, `gif`, or `png`.

## Future Features

The Vertigo Project has a list of features to implement for future versions,
including:

* Ability for the user to add and edit the dataset specifications
* Saving of Vertigo project files from the UI
* Improvements in network access speed
* Verification of rendering with different data projections
* Multiple datasets rendered on top of one another
* THREDDS data discovery and catalog search
* Creation and saving of animations to MP4 and GIF files

## Source Code

The source code for the Vertigo Project is available on Github at:

https://github.com/phollemans/vertigo

along with screenshots, a video tutorial, and pre-compiled install packages
for Windows, Mac, and Linux.

## Feedback

Please direct feedback to `peter@terrenus.ca` and `coastwatch.info@noaa.gov`.

---
Last updated: Dec 16, 2020

