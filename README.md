JavaDecompiler 1.0, a [Total Commander](https://www.ghisler.com/) plugin
====================================

This plugin allows Total Commander to both **decompile** and **navigate** java *.class* files. It is a packer plugin, meaning you can "enter" these files as archives. 

**But... a .class file is not an archive, is it?**

It is not, .class files do NOT contain other files. However this plugin "hacks" the TC packer interface so that class files appear to be archives containing all of:
 - a file "classname.java" which you can view (F3) or copy (F5). This java is the decompiled class file 
 - a list of directories representing all the methods, fields, constructors, member classes and interfaces of the class. This way you can have a very quick view of the class structure
 - a couple more directories show the system properties and environment variables (this info is not directly related to the class, but it could be useful)
 
The plugin uses CFR 0.152 decompiler as a library, so it does not need any extra executables or processes.

Download and resources
----------------------
TO BE CHANGED:
- Download the [latest release in this project](hhttps://github.com/moisescastellano/javadecompiler-tcplugin/blob/main/releases)
- [Plugin page at totalcmd.net](http://totalcmd.net/plugring/diskdircrc.html)
- [Thread for discussing this plugin](https://www.ghisler.ch/board/viewtopic.php?t=75748) at the TC forum
- This is a work in progress, you can help with [things to do](https://github.com/moisescastellano/diskdircrc-tcplugin/blob/main/to-do.md)

Java plugin
----------------------
JavaDecompiler is written in Java, so you need to have installed a [Java Runtime Environment (JRE)](https://www.java.com/en/download/manual.jsp).
Because it uses lambda expressions, it needs **at least Java 8**. It has been tested on JDK1.8.0

This plugin is based on the [Java plugin interface](https://moisescastellano.github.io/tcmd-java-plugin)

Known bugs and things to-do
----------------------
Dates are shown incorrectly (e.g. year shown as 2098). This is an error not in my code but in the java plugin library. Soon to be corrected.

TO BE CHANGED:
Refer to [things to do](https://moisescastellano.github.io/tcmd-java-plugin/to-do.md) for other work in progress.

Contact
----------------------
Author: Moises Castellano 2021

If you have any comment, suggestion or problem regarding this java plugin,
you contact me at:
 - email: moises.castellano (at) gmail.com
 
 - [github project issues page](https://github.com/moisescastellano/javadecompiler-tcplugin/issues)

Please specify the java plugin and the JRE version you are using.

Disclaimer
----------------------
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


License
----------------------
Licensed under under the GNU General Public License v3.0, a strong copyleft license:
https://github.com/moisescastellano/tcmd-java-plugin/blob/main/LICENSE




