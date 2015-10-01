# 🍈 Cantaloupe

*Extensible [IIIF Image API 2.0](http://iiif.io) image server in Java*

Home: [https://github.com/medusa-project/cantaloupe]
(https://github.com/medusa-project/cantaloupe)

# Features

* Simple
* Self-contained
* Easy to install and get working
* Pluggable resolvers for filesystem and HTTP sources
* Pluggable processors to support a wide variety of source images
* Lean and efficient with no app server or Servlet stack overhead
* Configurable caching
* Optional authentication

# Requirements

The only hard requirement is Java 7+. Particular processors may have additional
requirements; see the Processors section below.

# Running

First, copy the file `cantaloupe.properties.sample` in the distribution to
`cantaloupe.properties` and edit as desired.

Then, from the command prompt:

`$ java -Dcantaloupe.config=/path/to/cantaloupe.properties -jar Cantaloupe-x.x.x.jar`

It is now ready for use at: `http://localhost:{http.port}/iiif`

To see information about an image, visit:
`http://localhost:{http.port}/iiif/{image filename}/info.json`

To see the image itself, try:
`http://localhost:{http.port}/iiif/{image filename}/full/full/0/default.jpg`

# Upgrading

Upgrading is usually just a matter of downloading a new version and running it.
Sometimes there are backwards-incompatible changes to the configuration file
structure, though, so check below to see if there is anything more to be done.

## 1.0-beta2 to 1.0-beta3

* Rename the `cache` key in the configuration to `cache.server`.
* Add the `cache.client.*` keys from the sample configuration.
* Add the `http.auth.basic*` keys from the sample configuration.
* Add the `KakaduProcessor` keys from the sample configuration.

## 1.0-beta1 to 1.0-beta2

* Add the `cache` and `FilesystemCache.*` keys from the sample configuration.
* Add the `FilesystemResolver.path_separator` and `HttpResolver.path_separator`
  keys from the sample configuration.

# Technical Overview

Cantaloupe features a configurable processing pipeline based on:

* Resolvers, that locate source images;
* Processors, that transform them according to the parameters in an image
  request; and
* Caches, that cache generated image tiles.

## Resolvers

Resolvers locate a source image based on the identifier in an IIIF URL. In
Java-speak, they take in an identifier and return an `ImageInputStream` and/or
`File` object from which the corresponding image can be read by a `Processor`.

### FilesystemResolver

FilesystemResolver maps an identifier from an IIIF URL to a filesystem path,
for retrieving images on a local or attached filesystem.

For images with extensions that are missing or unrecognized, this resolver will
check the "magic number" to determine type, which will add some overhead. It
is therefore slightly more efficient to serve images with extensions.

#### Prefix and Suffix

The `FilesystemResolver.path_prefix` and `path_suffix` configuration options
are optional and intended to make URLs shorter, more stable, and hide
potentially sensitive information. For example, with these options set as such:

    FilesystemResolver.path_prefix = /usr/local/images/
    FilesystemResolver.path_suffix =

An identifier of `image.jpg` in the URL will resolve to
`/usr/local/images/image.jpg`.

#### Path Separator

The IIIF Image API 2.0 specification mandates that identifiers in URLs be
percent-encoded, which changes slashes to `%2F`. Some reverse-proxy servers
automatically decode `%2F` into `/` before passing on the request. This will
cause HTTP 404 errors in Cantaloupe.

If you are unable to configure your reverse proxy to stop decoding slashes,
the `FilesystemResolver.path_separator` configuration option will enable you to
use an alternate string as a path separator in an identifier. (Be sure to
supply the non-percent-encoded string, and then percent-encode it in URLs if it
contains any URL-unsafe characters.)

### HttpResolver

HttpResolver maps an identifier from an IIIF URL to an HTTP resource. Image
access via HTTP is likely to be slower than via a filesystem, though this
varies depending on the relative performance of both, as well as the particular
source image format and processor.

It is preferable to use this resolver with source images with recognizable file
extensions. For images with an extension that is missing or unrecognizable, it
will issue an HTTP HEAD request to the server to check the `Content-Type`
header. If the type cannot be inferred from that, an HTTP 415 response will be
returned.

#### Prefix and Suffix

The `HttpResolver.url_prefix` and `url_suffix` configuration options are
optional and intended to make URLs shorter, more stable, and hide potentially
sensitive information. For example, with these options set as such:

    HttpResolver.url_prefix = http://example.org/images/
    HttpResolver.url_suffix =

An identifier of `image.jpg` in the URL will resolve to
`http://example.org/images/image.jpg`.

#### Path Separator

The IIIF Image API 2.0 specification mandates that identifiers in URLs be
percent-encoded, which changes slashes to `%2F`. Some proxy servers
automatically decode `%2F` into `/` before passing on the request. This will
cause HTTP 404 errors in Cantaloupe.

If you are unable to configure your proxy to stop decoding slashes, the
`HttpResolver.path_separator` configuration option will enable you to use an
alternate string as a path separator in an identifier. (Be sure to supply the
non-percent-encoded string, and then percent-encode it in URLs if it contains
any URL-unsafe characters.)

## Processors

Cantaloupe can use different image processors for different source formats.
(Assignments are made in the config file.) Currently, the available processors
are:

* ImageIoProcessor
* GraphicsMagickProcessor
* ImageMagickProcessor
* KakaduProcessor
* JaiProcessor

All processors can work with FilesystemResolver, and all but KakaduProcessor
can work with HttpResolver.

In terms of format support, a distinction is made between the concepts of
source format and output format, and furthermore, available output formats may
differ depending on the source format.

Supported source formats depend on the processor, and maybe installed
libraries/delegates, etc., as well. Lists of these are displayed on the
landing page, at `/iiif`. A list of output formats supported *for a given
source format* is contained within the response to an information request
(`/iiif/{identifier}/info.json`).

### ImageIoProcessor

ImageIoProcessor uses the Java ImageIO framework to load and process images in
a native-Java way. This is a good processor to get started with as it has no
external dependencies and works out-of-the-jar.

ImageIO, as its name implies, is simply an I/O interface that does not care 
about image formats, and therefore the list of formats supported by this
processor varies depending on the codec JARs available in the classpath.

ImageIoProcessor buffers entire source images in RAM, and is therefore
memory-intensive. Large amounts of RAM and fast storage help. To avoid
OutOfMemoryErrors, it's a good idea to allow for a very large heap size. (To
allow 16GB, add the `-Xmx16g` flag to the launch command.)

### GraphicsMagickProcessor

GraphicsMagickProcessor uses [im4java](http://im4java.sourceforge.net) to
fork out to the [GraphicsMagick](http://www.graphicsmagick.org) executable
(`gm`). As such, GraphicsMagick must be installed.

GraphicsMagick produces high-quality output and supports all of the IIIF
transforms and all IIIF output formats (assuming the necessary libraries are
installed; see [Supported Formats](http://www.graphicsmagick.org/formats.html)).

GraphicsMagickProcessor is a good fallback processor, as it supports a wide
range of source formats and is generally faster than ImageMagickProcessor.

### ImageMagickProcessor

ImageMagickProcessor, like GraphicsMagickProcessor, also uses
[im4java](http://im4java.sourceforge.net) to wrap [ImageMagick]
(http://www.imagemagick.org/) commands. As such, ImageMagick must be installed.

ImageMagick produces high-quality output and supports all of the IIIF
transforms and all IIIF output formats, assuming the necessary delegates are
installed. It also supports a wide array of source formats.

ImageMagick is not known for being particularly fast or efficient, but it is
generally quite usable. If you can find or compile a "Q8" version of
ImageMagick, its memory use will be halved.

### KakaduProcessor

KakaduProcessor uses the `kdu_expand` and `kdu_jp2info` binaries -- part of the
[Kakadu](http://www.kakadusoftware.com) toolkit -- to efficiently process
JPEG2000 source images. This processor performs well even with large JP2s.

Although it does support some other operations, `kdu_expand` is mainly a
decompression tool, and Cantaloupe uses only this aspect of it, doing the rest
of the IIIF operations (scaling, rotation, etc.) using the Java 2D API.

Kakadu is not free and the binaries are not included with Cantaloupe. It is
your responsibility to obtain them by legal means and to comply with the terms
of use.

### JaiProcessor

Java Advanced Imaging (JAI) is a powerful low-level imaging framework
developed by Sun until around 2006. JaiProcessor uses an updated fork called
[JAI-EXT](https://github.com/geosolutions-it/jai-ext).

JaiProcessor's main theoretical advantage is its ability to exploit JAI's
internal tiling engine, which makes it capable of region-of-interest decoding
with some formats.

JaiProcessor can read and write the same formats as ImageIoProcessor.

*Note: JaiProcessor is very much experimental at this time. Stand by with
CTRL+C to prevent your CPU from melting.*

### Which Processor Should I Use?

1. Don't use JaiProcessor
2. Use KakaduProcessor for JP2s
3. For all of the rest of the source formats you would like to serve, it's a
good idea to experiment with different processors, comparing their performance,
resource usage, and output quality. Benchmark results, if you can contribute
them, are welcome.

## Resolver-Processor Compatibility

| | FilesystemResolver | HttpResolver |
| --- | --- | --- |
|GraphicsMagickProcessor | Yes | Yes |
|ImageIoProcessor | Yes | Yes |
|ImageMagickProcessor | Yes | Yes |
|JaiProcessor | Yes | Yes |
|KakaduProcessor | Yes | NO |

## A Word About Memory Usage

Consider the case of an image server that is asked to render tiles for a
4096x4096 pixel source image that, at 24 bits per pixel, totals 50MB in
decompressed size. Multiple clients are simultaneously requesting numerous
downscaled tiles to display the initial zoom level. Request handlers do not
communicate with each other, so each one is loading this large image and
operating on it independently.

Furthermore, some processors are less efficient, and have to create new images
internally at each intermediate step in the processing pipeline (cropping,
scaling, rotating, etc.). Each one occupies precious memory.

It's easy to see where RAM becomes a very major consideration here. It is
completely normal to see transient spikes of hundreds of MB of memory use on
the Java heap in response to a single zooming-image-viewer request. The JVM
will accommodate by increasing the heap size, which typically never shrinks
during the lifetime of an application. From the operating system's perspective,
the process is bloating up to multiple gigabytes in size, but there is nothing
wrong -- most of this is actually unused heap space.

The smaller the available heap space, the larger the source images, and the
larger the number of simultaneous requests, the greater the likelihood of
OutOfMemoryErrors. In production, it is highly recommended to use the `-Xmx`
flag to increase the maximum heap size to the largest amount possible -- for
example, `-Xmx16g` for 16GB. And, of course, do use caching aggressively.

## Caches

There is currently one cache, FilesystemCache, which caches generated images
and (parts of) information requests into a filesystem directory. The location
of this directory is configurable, as is the "time-to-live" of the cache files.
Expired files are replaced the next time they are requested.

Cantaloupe does not cache entire information response representations -- only
image dimensions, which are the only expensive part to generate. This means it
is possible to change other variables that might affect the contents of the
representation (like processors) without having to flush the cache.

### Flushing the Cache

#### Expired Images Only

Start Cantaloupe with the `-Dcantaloupe.cache.flush_expired` option:

`$ java -Dcantaloupe.config=... -Dcantaloupe.cache.flush_expired -jar Cantaloupe-x.x.x.jar`

#### All Images

Start Cantaloupe with the `-Dcantaloupe.cache.flush` option:

`$ java -Dcantaloupe.config=... -Dcantaloupe.cache.flush -jar Cantaloupe-x.x.x.jar`

# Authentication

Cantaloupe supports HTTP Basic authentication via the `http.auth.basic*` keys
in the configuration file.

# Client-Side Caching

The HTTP/1.1 `Cache-Control` response header is configurable via the
`cache.client.*` keys in the configuration file.

# Notes on Source Formats

(Feel free to add in your own notes, and send a pull request. Controlled
benchmarks are also welcome.)

## JPEG

It should be possible to use TurboJPEG (high-level API over [libjpeg-turbo]
(http://www.libjpeg-turbo.org)) with ImageIoProcessor to hardware-accelerate
JPEG coding. To do so, include the [TurboJPEG plugin JAR]
(https://github.com/geosolutions-it/imageio-ext/wiki/TurboJPEG-plugin) in your
classpath, and compile libjpeg-turbo with Java support (using the `--with-java`
configuration option). (The author has not tried this.)

## JPEG2000

KakaduProcessor is by far the most efficient processor for this format.

GraphicsMagick can read/write JPEG2000 using JasPer, and ImageMagick using
OpenJPEG. Both of these are extremely slow.

Years ago, Sun published [platform-native JAI JPEG2000 accelerator JARs]
(http://download.java.net/media/jai/builds/release/1_1_3/INSTALL.html) for
Windows, Linux, and Solaris, which improved performance from dreadful to merely
poor. It is unknown whether these are still available or whether they would
still work on modern platforms. If so, they ought to work with ImageIoProcessor.

## TIFF

GraphicsMagickProcessor and ImageMagickProcessor can both handle TIFF if the
necessary delegate or plugin is installed. (See the landing page, at `/iiif`.)

TIFF is currently disabled in ImageIoProcessor because it is too buggy.

# Custom Development

## Custom Resolvers

A custom resolver needs to implement one or both of the
`e.i.l.cantaloupe.resolver.FileResolver` or
`e.i.l.cantaloupe.resolver.StreamResolver`
interfaces. Then, to use it, set `resolver` in your properties file to its name.

See one of the existing resolvers for examples.

## Custom Image Processors

Custom processors can be added by implementing one or both of the
`e.i.l.cantaloupe.processor.FileProcessor` and
`e.i.l.cantaloupe.processor.StreamProcessor`interfaces. See the interface
documentation for details and the existing implementations for examples.

Another option might be to add an ImageIO format plugin instead of a custom
processor, and then use ImageIoProcessor with it.

## Custom Caches

Custom caches can be added by implementing the `e.i.l.cantaloupe.cache.Cache`
interface. See the interface documentation for details and the existing
implementations for examples.

## Custom Configuration

Feel free to add new configuration keys to the properties file. They should
be in the form of `NameOfMyClass.whatever`. They can then be accessed via
`e.i.l.cantaloupe.Application.getConfiguration()`.

## Contributing Code

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

# Feedback

Ideas, suggestions, feature requests, bug reports, and other kinds of feedback
are welcome; [contact the author](mailto:alexd@illinois.edu), or submit an
issue or pull request.

# License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
