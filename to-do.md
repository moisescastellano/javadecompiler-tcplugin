
JavaDecompiler plugin - Things To do
=====================================

This is a work in progress. **Help wanted!** - in particular with Visual C++ issues. See contact below.

Main issues are found in Java Plugin Interface's [issues page](https://github.com/moisescastellano/tcmd-java-plugin/issues).

Check also [this project's issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues).

Solutions for most problems are found in the [Troubleshooting guide](https://moisescastellano.github.io/tcmd-java-plugin/troubleshooting)

For some of these issues, changes in code have to be done:

  - error ["**JRE is not installed**"](https://github.com/moisescastellano/javadecompiler-tcplugin/issues/1) : when finding the library, that the plugin searchs in two ways described in the issue.

  - plugin complains ["**LoadLibrary Failed / Starting Java Virtual Machine failed**"](https://github.com/moisescastellano/tcmd-java-plugin/issues/2) : specific JREs failing are reported in the issue.

If you are giving a try to the Java plugin interface or any of its plugins, **some working (Oracle JREs) versions** are: jre1.8.0_211, jre-8u311-windows-x64

If you are a developer and can help, the [JVM search code is performed here](https://github.com/moisescastellano/tcmd-java-plugin/blob/main/src/vc-project/Total%20Commander%20Java%20Plugin/java.cpp)

Known bugs
----------
Dates are shown incorrectly (e.g. year shown as 2098). I think this is an error in the original java plugin, to be corrected.

Some classes cannot be navigated
----------
  - Classes in package java.* cannot be navigated, because the SecurityManager forbids so. This maybe could be avoided somehow.
  - For other classes, in particular classes extending other user (non-JRE) classes, getting constructors and other members throws exception. Maybe cannot be avoided, to be reviewed.

Improvements
----------
Packing functionality is not implemented. Usually this would create an archive of the kind that the plugin manages. While creating .class files makes no sense for the plugin, an useful functionality that could be provided is creating a .zip archive with .java decompiled files from a .jar or directory containing .class files. Let me know if this makes sense and would be useful to you.

Contact
----------
If you want to help with the things above, or you have any comment, suggestion or problem regarding this java plugin,
you contact me at:

 - [Thread for discussing this plugin](https://www.ghisler.ch/board/viewtopic.php?t=75793) at the TC forum
 - [Github project issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues)
 - email: moises.castellano (at) gmail.com

Please detail the specific (including if 32 or 64-bit) version of: Java plugin interface, Total Commander and JRE that you are using.

