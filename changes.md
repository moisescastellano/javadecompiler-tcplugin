JavaDecompiler - history of changes
==========================================

v1.1 - 2022-jan-30
------------------
- You can now also view (F3) or copy (F5) files with extension ".env" and ".property" as they were text files
	- files with extension ".env" contain the environment variables
	- files with extension ".property" contain the system properties
	- new "environment.env" and "properties.property " files in the root dir.
- JavaDecompiler 1.1 is now based on [Java Plugin Interface v2.3](https://github.com/moisescastellano/tcmd-java-plugin)
	- PluginClassLoader was incomplete (missing e.g. findResources implementation). It has now been completed.
- Better exception management when getting/showing elements (e.g. class methods) fails: 
    - if an error occurs, an ".exception" file is shown that you can open (F3) to view the detail.
	- other elements (e.g. decompiled class) can still be shown in that case
- [Troubleshooting guide](https://moisescastellano.github.io/tcmd-java-plugin/troubleshooting) linked and main problems enumerated in main README
- Major code restructure - more legible if you are familiar with lambda expressions
	- it now shares a common extended class ([ItemsPlugin](https://github.com/moisescastellano/javadecompiler-tcplugin/blob/main/src/moi/tcplugins/decompiler/ItemsPlugin.java)) with ThousandTypes plugin
- JavaDecompiler is now hosted at [Github pages](https://moisescastellano.github.io/javadecompiler-tcplugin/)
- Version check (for at least tc-classloader 2.2.0) to avoid javalib problems 
	- Refer to: [In case you have more than one Java plugin installed](https://github.com/moisescastellano/tcmd-java-plugin/blob/main/troubleshooting.md#In-case-you-have-more-than-one-Java-plugin-installed)
- Source code is now available at [Github project](https://github.com/moisescastellano/tcmd-java-plugin)
- Logging is updated to SLF4J (previously was based on deprecated Apache commons-logging implementation).
	- Logging now works for Log4j2 via SLF4J.
	- Logging is disabled by default, any logging implementation has been removed from the plugin itself.
	- Documentation about [how to configure logging for plugins](https://github.com/moisescastellano/tcmd-java-plugin/blob/main/logging.md).
- errormessages.ini: corrected typos on english and german messages, showing some memory dump

v1.0 - 2021-dec-27
------------------
- JavaDecompiler is now available at [Totalcmd.net](http://totalcmd.net/plugring/java_decompiler.html)
- This plugin allows Total Commander to both **decompile** and **navigate** java *.class* files. 
	- It is a packer plugin, meaning you can "enter" these files as archives. 
- This plugin "hacks" the TC packer interface so that class files appear to be archives containing all of:
	- a file "classname.java" which you can view (F3) or copy (F5). This java is the decompiled class file.
	- a list of directories representing all the methods, fields, constructors, member classes and interfaces of the class.
	- a couple more directories show the system properties and environment variables.


