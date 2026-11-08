# macrolib

Java input event remapping and macro library for Linux. This library is a Java port of the macro system in [Gnome15](https://github.com/Gnome15/gnome15), of which I was the original author. 

It was stripped down and converted to Java, modernising and adding some new features so it could be made part of my related project, [Snake](https://github.com/bithatch/snake) which has some similarities, but is for Razer keyboards.

## Configuring your project

The library is currently available in Maven OSS Snapshots repository, so configure your project according to the
build system you use. 

Requires Java 9 or higher (due to modularity requirements). 

### Maven

```xml
	<dependency>
		<groupId>uk.co.bithatch</groupId>
		<artifactId>macrolib</artifactId>
		<version>1.0-SNAPSHOT</version>
	</dependency>
```

## Try It

There is no further documentation just yet, I am focussed on the primary user of this library, the application [Snake](https://github.com/bithatch/snake).


