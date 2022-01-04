
JavaDecompiler plugin - Things To do
=====================================

This is a work in progress. **Help wanted!** - in particular with Visual C++ issues. See contact below.

Check also [this project's issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues) and Java Plugin Interface's [issues page](https://github.com/moisescastellano/tcmd-java-plugin/issues).

Priority issues
---------
There are now a couple (closely related) issues reported regarding this Java plugin interface, which are difficult to solve for myself as they are Visual C++ related, which I am not familiar with.

For some JDK/JRE versions the plugin has a hard time to find or load the Java Virtual Machine.

[The first one](https://github.com/moisescastellano/javadecompiler-tcplugin/issues/1) is the error "**JRE is not installed**" when finding the library, that the plugin searchs in two ways described in the issue.

[The second one](https://github.com/moisescastellano/tcmd-java-plugin/issues/2) is plugin complains "**LoadLibrary Failed / Starting Java Virtual Machine failed**". Specific JREs failing are reported in the issue.

If you are giving a try to the Java plugin interface or any of its plugins, **some working (Oracle JREs) versions** are: jre1.8.0_211, jre-8u311-windows-x64

If you are a developer and can help, the [JVM search code is performed here](https://github.com/moisescastellano/tcmd-java-plugin/blob/main/src/vc-project/Total%20Commander%20Java%20Plugin/java.cpp)

Known bugs
----------
Dates are shown incorrectly (e.g. year shown as 2098). I think this is an error in the original java plugin, to be corrected.

Some classes cannot be navigated
----------
Classes in package java.* cannot be navigated, because the SecurityManager forbids so. This maybe could be avoided somehow.
For other classes getting constructors and other members throws exception. Maybe cannot be avoided, to be reviewed.

Improvements
----------
Packing functionality is not implemented. Usually this would create an archive of the kind that the plugin manages. While creating .class files makes no sense for the plugin, an useful functionality that could be provided is creating a .zip archive with .java decompiled files from a .jar or directory containing .class files. Let me know if this makes sense and would be useful to you.

Contact
----------
If you want to help with the things above, or you have any comment, suggestion or problem regarding this java plugin,
you contact me at:
 - email: moises.castellano (at) gmail.com
 - [Github project issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues)
Please specify the java plugin and the JRE versions you are using.

