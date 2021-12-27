package moi.tcplugins.decompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import plugins.wcx.HeaderData;
import plugins.wcx.OpenArchiveData;
import plugins.wcx.WCXPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import moi.tcplugins.PluginLogger;

/**
 * @author Moises Castellano 2021
 * https://github.com/moisescastellano/tcmd-java-plugin
 */

public class Decompiler extends WCXPluginAdapter {
	
	static final Logger log = LoggerFactory.getLogger(Decompiler.class);
	// final PluginLogger log = new PluginLogger();

	private class CatalogInfo {
		/**
		 * The name of the archive.
		 */
		private String arcName;
		
		private Class<?> clazz;		
		public Throwable throwable;
		public int msgCount; 
		
		private int constructor;
		private int method;
		private int interfaze;
		private int field;
		private int memberClass;
		private int annotation;

		private int declaredConstructor;
		private int declaredMethod;
		private int declaredField;
		private int declaredClass;
		private int declaredAnnotation;
		
		private int everythingCounter;
		private int everythingPublicCounter;
		private int everythingDeclaredCounter;
		
		/* there are problems when the Class.get* methods are called multiple times,
		  as return not always in the same order, which is needed in readHeader, so we store them  */

		private List<AnnotatedElement> constructors = new ArrayList<>();
		private List<AnnotatedElement> methods = new ArrayList<>();
		private List<AnnotatedElement> interfaces = new ArrayList<>();
		private List<AnnotatedElement> fields = new ArrayList<>();
		private List<AnnotatedElement> memberClasses = new ArrayList<>();
		private List<Annotation> annotations = new ArrayList<>();
		
		private List<AnnotatedElement> declaredConstructors = new ArrayList<>();
		private List<AnnotatedElement> declaredMethods = new ArrayList<>();
		private List<AnnotatedElement> declaredFields = new ArrayList<>();
		private List<AnnotatedElement> declaredClasses = new ArrayList<>();
		private List<Annotation> declaredAnnotations = new ArrayList<>();
		
		private List<AnnotatedElement> everything = new ArrayList<>(); 
		private List<AnnotatedElement> everythingPublic = new ArrayList<>(); 
		private List<AnnotatedElement> everythingDeclared = new ArrayList<>();
		
		private Properties properties = System.getProperties();
		private Enumeration<Object> propertiesKeys = System.getProperties().keys();
		
		private Map<String, String> env = System.getenv();
		private Iterator<String> envKeys = env.keySet().iterator();

	}

	@Override
	public Object openArchive(OpenArchiveData archiveData) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".openArchive(archiveData)");
		}
		Path path = Paths.get(archiveData.getArcName());
		CatalogInfo catalogInfo = new CatalogInfo();
		try {
			ByteClassLoader loader = new ByteClassLoader();
			Class<?> clazz = loader.defineClass(null, Files.readAllBytes(path));
			catalogInfo.clazz = clazz;
		} catch (Throwable e) {
			catalogInfo.throwable = e;
		}
		
		catalogInfo.arcName = archiveData.getArcName();			
		
		return catalogInfo;
	}
	
	@Override
	public int closeArchive(Object archiveData) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".closeArchive(archiveData)");
		}
		return SUCCESS;
	}

	@Override
	public int processFile(Object archiveData, int operation, String destPath, String destName) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".processFile(archiveData, operation=["+operation+"], destPath=["+destPath+"];destName=["+destName+"]");
		}
		CatalogInfo catalogInfo = (CatalogInfo) archiveData;
		try {
			if (operation == PK_EXTRACT) {
				String fullDestName = (destPath==null?"":destPath) + destName;
				if (log.isDebugEnabled()) {
					log.debug(this.getClass().getName() + ".processFile() EXTRACT from:[" + catalogInfo.arcName + "] to: [" + fullDestName + "]");
				}
				return decompile(new File(catalogInfo.arcName), new File(fullDestName), false);
			} else if (operation == PK_TEST) {
				if (log.isDebugEnabled()) {
					log.debug(this.getClass().getName() + ".processFile() TEST " + (destPath==null?"":destPath) + destName);
				}
				// return checkFile(new File(fullOriginName), headerData.getFileCRC());
			} else if (operation == PK_SKIP) {
				if (log.isDebugEnabled()) {
					log.debug(this.getClass().getName() + ".processFile() SKIP " + (destPath==null?"":destPath) + destName);
				}
			}
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	@Override
	public int readHeader(Object archiveData, HeaderData headerData) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".readHeader(archiveData, headerData)");
		}
		try {
			CatalogInfo catalogInfo = (CatalogInfo) archiveData;
			if (catalogInfo.msgCount == 0) {
				try {
					File f = new File(catalogInfo.arcName);
					headerData.setFileName(f.getName().substring(0,f.getName().length()-".class".length())+".java" );
					headerData.setUnpSize(f.length());
					log.debug("SUCCESS 3");
					return SUCCESS;
				} catch (Exception e) {
					log.error("setting class name",e);
				} finally {
					catalogInfo.msgCount++;
				}
			}
			if (getProperty(catalogInfo, headerData, "properties")) {
				return SUCCESS;
			}
			if (getEnv(catalogInfo, headerData, "environment")) {
				return SUCCESS;
			}
			catalogInfo.msgCount++;
			log.debug("readHeader :" + catalogInfo.msgCount);
			if (catalogInfo.throwable != null) {
				Throwable t = catalogInfo.throwable;
				log.debug("Error reading class", t);
				switch (catalogInfo.msgCount) {
				case 2:
					headerData.setFileName("\\error found" + "\\" + t.getClass().getSimpleName() + "\\" + t.getMessage());
					log.debug("SUCCESS 1");
					return SUCCESS;
				case 3:
					if (t.getMessage().startsWith("Prohibited package")) {
						headerData.setFileName("reading classes from package java is not allowed");
					} else {
						headerData.setFileName("error creating directories to navigate class");
					}
					log.debug("SUCCESS 2");
					return SUCCESS;
				default:
					return E_END_ARCHIVE;
				} 
			} else {
				Class<?> clazz = catalogInfo.clazz;
				switch (catalogInfo.msgCount) {
				case 2:
					// headerData.setFileName(clazz.getPackage() + ".package");
			        int index = clazz.getName().lastIndexOf(".");
			        if (index == -1) {
						headerData.setFileName("this class has no package defined.package");
			        } else {
				        String packaje = clazz.getName().substring(0,index);
						headerData.setFileName(packaje + ".package");
			        }
					return SUCCESS;
				default:
					if (classToDirs(catalogInfo, headerData)) {
						log.debug("SUCCESS 4");
						return SUCCESS;
					}
					return E_END_ARCHIVE;
				}
			}

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			
		}
		if (log.isErrorEnabled()) {
			log.error(this.getClass().getName() + ".readHeader() E_BAD_DATA");
		}
		return E_BAD_DATA;
	}
	
	private boolean classToDirs(CatalogInfo catalogInfo, HeaderData headerData) {
		
		Class<?> clazz = catalogInfo.clazz;
		
		if (getElement(catalogInfo, headerData, catalogInfo.constructor++, catalogInfo.constructors, ()->clazz.getConstructors(), "constructors", false)) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.method++, catalogInfo.methods, ()->clazz.getMethods(), "methods", false)) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.interfaze++, catalogInfo.interfaces, ()->clazz.getInterfaces(), "interfaces", false)) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.field++, catalogInfo.fields, ()->clazz.getFields(), "fields", false)) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.memberClass++, catalogInfo.memberClasses, ()->clazz.getClasses(), "memberClasses", false)) {
			return true;
		}
		if (getAnnotation(catalogInfo, headerData, catalogInfo.annotation++, catalogInfo.annotations, ()->clazz.getAnnotations(), "annotations")) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.declaredConstructor++, catalogInfo.declaredConstructors, ()->clazz.getDeclaredConstructors(), "declaredConstructors", true)) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.declaredMethod++, catalogInfo.declaredMethods, ()->clazz.getDeclaredMethods(), "declaredMethods", true)) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.declaredField++, catalogInfo.declaredFields, ()->clazz.getDeclaredFields(), "declaredFields", true)) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.declaredClass++, catalogInfo.declaredClasses, ()->clazz.getDeclaredClasses(), "declaredClasses", true)) {
			return true;
		}
		if (getAnnotation(catalogInfo, headerData, catalogInfo.declaredAnnotation++, catalogInfo.declaredAnnotations, ()-> clazz.getDeclaredAnnotations(), "declaredAnnotations")) {
			return true;
		}

		if (getElement(catalogInfo, headerData, catalogInfo.everythingCounter++, catalogInfo.everything, "everything")) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.everythingPublicCounter++, catalogInfo.everythingPublic, "everythingPublic")) {
			return true;
		}
		if (getElement(catalogInfo, headerData, catalogInfo.everythingDeclaredCounter++, catalogInfo.everythingDeclared, "everythingDeclared")) {
			return true;
		}
		return false;
	}

	private boolean getElement(CatalogInfo catalogInfo, HeaderData headerData, int contador, List<AnnotatedElement> elements, String dir) {
		if (contador < elements.size()) {
			AnnotatedElement elem = elements.get(contador);
			headerData.setFileName(dir + "\\" + elem + "." + elem.getClass().getSimpleName().toLowerCase());
			return true;
		} else {
			return false;
		}
	}

	private boolean getElement(CatalogInfo catalogInfo, HeaderData headerData, int contador, List<AnnotatedElement> elements, Supplier<AnnotatedElement[]> s, String dir, boolean declared) {
		
		if (contador > 0 && elements.size() == 0) { // already tried previously, there are no elements
			return false;
		}
		try {
			if (contador == 0) { // only get elements the first time
				elements.addAll(Arrays.asList(s.get()));  // s.get can throw ClassNotFounException
			}
			if (contador < elements.size()) {
				AnnotatedElement elem = elements.get(contador);
				headerData.setFileName(dir + "\\" + elem + "." + elem.getClass().getSimpleName().toLowerCase());
				if (declared) {
					catalogInfo.everythingDeclared.add(elem);
				} else {
					catalogInfo.everythingPublic.add(elem);
				}
				if (!catalogInfo.everything.contains(elem)) {
					catalogInfo.everything.add(elem);
				}
				return true;
			} else {
				return false;
			}
		} catch (Throwable t) {
			headerData.setFileName(dir + "\\error-retrieving-" + dir + "\\" + t.getClass().getSimpleName() + "\\" + t.getMessage());
			return true;
		}
	}
	
	private boolean getAnnotation(CatalogInfo catalogInfo, HeaderData headerData, int contador, List<Annotation> elements, Supplier<Annotation[]> s, String dir) {
		
		if (contador > 0 && elements.size() == 0) { // already tried previously, there are no elements
			return false;
		}
		try {
			if (contador == 0) { // only the first time
				elements.addAll(Arrays.asList(s.get()));  // s.get can throw ClassNotFounException
			}
			if (contador < elements.size()) {
				Annotation elem = elements.get(contador);
				headerData.setFileName(dir + "\\" + elem + ".annotation");
				return true;
			} else {
				return false;
			}
		} catch (Throwable t) {
			headerData.setFileName(dir + "\\error-retrieving\\" + t.getClass().getSimpleName() + "\\" + t.getMessage());
			return true;
		}
	}
	
	private boolean getProperty(CatalogInfo catalogInfo, HeaderData headerData, String dir) {
		
		try {
			if (catalogInfo.propertiesKeys.hasMoreElements()) {
				Object key = catalogInfo.propertiesKeys.nextElement();
				Object value = catalogInfo.properties.get(key);
				log.debug("getProperty:" + key + "-" + value);
				String sv = value.toString().replace("\\", "[/]");
				headerData.setFileName(dir + "\\" + key + "=" + sv + ".property");
				return true;
			} else {
				return false;
			}
		} catch (Throwable t) {
			headerData.setFileName(dir + "\\error-retrieving\\" + t.getClass().getSimpleName() + "\\" + t.getMessage());
			return true;
		}
	}

	private boolean getEnv(CatalogInfo catalogInfo, HeaderData headerData, String dir) {
		
		try {
			if (catalogInfo.envKeys.hasNext()) {
				String key = catalogInfo.envKeys.next();
				String value = catalogInfo.env.get(key);
				log.debug("getEnv:" + key + "-" + value);
				String sv = value.toString().replace("\\", "[/]");
				headerData.setFileName(dir + "\\" + key + "=" + sv + ".env");
				return true;
			} else {
				return false;
			}
		} catch (Throwable t) {
			headerData.setFileName(dir + "\\error-retrieving\\" + t.getClass().getSimpleName() + "\\" + t.getMessage());
			return true;
		}
	}
	
	@Override
	public int getPackerCaps() {
		return /* PK_CAPS_HIDE | PK_CAPS_NEW | */ PK_CAPS_MULTIPLE | PK_CAPS_MEMPACK;
	}

	private int decompile(final File source, final File dest, final boolean overwrite) {
		if (overwrite) {
			dest.delete();
		}
		if (dest.exists()) {
			return E_ECREATE;
		}
		PrintWriter out = null;
		try {
			out = new PrintWriter(dest);
			
			OutputSinkFactory mySink = new PluginSinkFactory(out);

			CfrDriver driver = new CfrDriver.Builder().withOutputSink(mySink).build();
			driver.analyse(Collections.singletonList(""+source));
			
	      
		} catch (FileNotFoundException fnfe) {
			return E_EOPEN;
		} finally {
				if (out != null) {
					out.close();
				}
		}
		return SUCCESS;
	}

	public class PluginSinkFactory implements OutputSinkFactory {
		
		PrintWriter out;
		
		public PluginSinkFactory(PrintWriter out) {
			this.out = out;			
		}
		
		@Override
		public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
			System.out.println("CFR wants to sink " + sinkType + ", and I can choose:");
			collection.forEach(System.out::println);
			if (sinkType == SinkType.JAVA && collection.contains(SinkClass.DECOMPILED)) {
				// I'd like "Decompiled".  If you can't do that, I'll take STRING.
				return Arrays.asList(SinkClass.DECOMPILED, SinkClass.STRING);
			} else {
				// I only understand how to sink strings, regardless of what you have to give me.
				return Collections.singletonList(SinkClass.STRING);
			}
		}

		Consumer<SinkReturns.Decompiled> dumpDecompiled = d -> {
			// System.out.println("Package [" + d.getPackageName() + "] Class [" + d.getClassName() + "]");
			out.println(d.getJava());
		};

		@Override
		public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
			if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
				return x -> dumpDecompiled.accept((SinkReturns.Decompiled) x);
			}
			return ignore -> {};
		}
	};
	
	public static void main(String[]args) {
		if (log.isDebugEnabled()) {
			log.debug(Decompiler.class.getName() + ".main");
		}		
	}
}
