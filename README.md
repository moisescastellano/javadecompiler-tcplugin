JavaDecompiler 1.0, a [Total Commander](https://www.ghisler.com/) plugin
====================================

This plugin allows Total Commander to both **decompile** and **navigate** java *.class* files. It is a packer plugin, meaning you can "enter" these files as archives. 

**But... a .class file is not an archive, is it?**

It is not, .class files do NOT contain other files. However this plugin "hacks" the TC packer interface so that class files appear to be archives containing all of:
 - a file "classname.java" which you can view (F3) or copy (F5). This java is the decompiled class file 
 - a list of directories representing all the methods, fields, constructors, member classes and interfaces of the class. This way you can have a very quick view of the class structure
 - a couple more directories show the system properties and environment variables (this info is not directly related to the class, but it could be useful)
 
 In the screenshot, on the left panel we have entered to a .class file. The sub-directories contain members of the class. Decompiled class is viewed on the right panel.
 
 ![JavaDecompiler screenshot](https://github.com/moisescastellano/javadecompiler-tcplugin/raw/main/screenshots/JavaDecompiler.png)
 
The plugin uses CFR 0.152 decompiler as a library, so it does not need any extra executables or processes.

Download and resources
----------------------
- Download the [latest release in this project](https://github.com/moisescastellano/javadecompiler-tcplugin/blob/main/releases)
- [Plugin page at totalcmd.net](http://totalcmd.net/plugring/java_decompiler.html)
- JavaDecompiler [Github page](https://moisescastellano.github.io/javadecompiler-tcplugin/)
- JavaDecompiler [Github project](https://github.com/moisescastellano/javadecompiler-tcplugin)
- [Thread for discussing this plugin](https://www.ghisler.ch/board/viewtopic.php?t=75793) at the TC forum
- This is a work in progress, you can help with [things to do](https://moisescastellano.github.io/javadecompiler-tcplugin/to-do)

Java plugin - JRE related issues
----------------------
JavaDecompiler is written in Java, so you need to have installed a [Java Runtime Environment (JRE)](https://www.java.com/en/download/manual.jsp).
Because it uses lambda expressions, it needs **at least Java 8**.

This plugin is based on the [Java plugin interface](https://moisescastellano.github.io/tcmd-java-plugin).

Note 1: if you get a _**Java Runtime Environment is not installed on this Computer**_ error, and you have it installed, check the [response to this issue](https://github.com/moisescastellano/javadecompiler-tcplugin/issues/1). In short:
add these 2 properties to the [JVM] section in the _tc_javaplugin.ini_  file, changing the paths to your JRE install dir:
```
JVM_DLL=c:\Program Files\Java\jre1.8.0_311\bin\server\jvm.dll
JVM_HOME=c:\Program Files\Java\jre1.8.0_311
```

Note 2: [**OpenJDK versions have been reported to fail**](https://github.com/moisescastellano/tcmd-java-plugin/issues/2) when the plugin tries to start the JVM. By now, recommendation is to install an Oracle (Sun) JDK/JRE version. Java plugin interface and derived plugins like [JavaDecompiler](https://moisescastellano.github.io/javadecompiler-tcplugin/) and [DiskDirCrc](https://moisescastellano.github.io/diskdircrc-tcplugin/) have been tested on **Oracle (Sun) JRE 1.8**  (jre-8u311-windows-x64.exe).

Issues and things to-do
----------------------
This is a work in progress. **Help wanted!** - in particular with Visual C++ issues.
 - Refer to [things to do](https://github.com/moisescastellano/tcmd-java-plugin/blob/main/to-do.md) for work in progress.
 - Check also the [issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues).
 - Java Plugin Interface's [issues page](https://github.com/moisescastellano/tcmd-java-plugin/issues).

Contact
----------------------

If you have any comment, suggestion or problem regarding this java plugin,
you contact me at:
 - email: moises.castellano (at) gmail.com
 
 - [github project issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues)

Please detail the specific version of: Java plugin interface, Total Commander and JRE that you are using.

Disclaimer
----------------------
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


License
----------------------
Licensed under under the GNU General Public License v3.0, a strong copyleft license:
https://github.com/moisescastellano/tcmd-java-plugin/blob/main/LICENSE




