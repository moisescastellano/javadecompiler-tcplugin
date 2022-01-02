
JavaDecompiler plugin - Things To do
=====================================

This is a work in progress. **Help wanted!** - in particular with Visual C++ issues. See contact below.

Check also [this project's issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues) and Java Plugin Interface's [issues page](https://github.com/moisescastellano/tcmd-java-plugin/issues).

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

