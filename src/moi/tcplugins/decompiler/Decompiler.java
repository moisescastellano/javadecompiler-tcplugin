package moi.tcplugins.decompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

/**
 * @author Moises Castellano 2021-2022
 * https://github.com/moisescastellano/tcmd-java-plugin
 */

public class Decompiler extends WCXPluginAdapter {
	
	static final Logger log = LoggerFactory.getLogger(Decompiler.class);

	private enum ItemType {
		JAVA, THROWABLE, PROPERTIES, ENVIRONMENT
	}
	
	private enum ItemEnum {
		JAVA_FILE(ItemType.JAVA), 
		PROPERTIES_DIR(ItemType.PROPERTIES), PROPERTIES_FILE(ItemType.PROPERTIES),
		ENVIRONMENT_DIR(ItemType.ENVIRONMENT), ENVIRONMENT_FILE(ItemType.ENVIRONMENT), 
		THROWABLE(ItemType.THROWABLE), 
		PACKAGE(ItemType.JAVA), CLASS_ELEMENTS(ItemType.JAVA), 
		FINISH(null);
		
		private static ItemEnum[] vals = values();
		private ItemType type;
		private ItemEnum(ItemType type) {
			this.type = type;
		}
	    public ItemEnum next() {
	        return vals[(this.ordinal()+1) % vals.length];
	    }
	    public ItemType getType() {
	    	return type;
	    }
	}
	
	private class CatalogInfo {
		/**
		 * The name of the archive.
		 */
		private String arcName;
		
		private Class<?> clazz;		
		
		public Throwable blockingThrowable;
		public Throwable throwableToShow;
		
		public ItemEnum itemToShow; 
		public ItemEnum nextItemToShow = ItemEnum.JAVA_FILE; 
		
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
		// private Enumeration<Object> propertiesKeys = properties.keys();
		private Iterator<Object> propertiesKeys = properties.keySet().iterator();
		
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
			catalogInfo.blockingThrowable = e;
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
				if (catalogInfo.itemToShow == ItemEnum.THROWABLE) {
					return save(catalogInfo, new File(fullDestName), catalogInfo.throwableToShow, false);
				}
				if (catalogInfo.itemToShow.getType() == ItemType.PROPERTIES) {
					return save(catalogInfo, new File(fullDestName), catalogInfo.properties, false);
				}
				if (catalogInfo.itemToShow.getType() == ItemType.ENVIRONMENT) {
					return save(catalogInfo, new File(fullDestName), catalogInfo.env, false);
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
		CatalogInfo catalogInfo = (CatalogInfo) archiveData;
		try {
			catalogInfo.itemToShow = catalogInfo.nextItemToShow;
			if (log.isDebugEnabled()) log.debug(this.getClass().getName() + ".readHeader(archiveData, headerData): " + catalogInfo.itemToShow);
			int result = doSwitch(catalogInfo, headerData, catalogInfo.itemToShow);
			if (log.isDebugEnabled()) log.debug(".readHeader: " + (result == SUCCESS ? "SUCCESS " : "FAIL ") + catalogInfo.itemToShow);
			return result;			
		} catch (Throwable t) {
			String s = "showing " + catalogInfo.itemToShow; 
			log.error(s, t);
			headerData.setFileName(s  + ".exception");
			headerData.setUnpSize(s.length());
			catalogInfo.nextItemToShow = catalogInfo.itemToShow.next();
			catalogInfo.throwableToShow = t;
			catalogInfo.itemToShow = ItemEnum.THROWABLE;
			return SUCCESS;
		}
	}
	
	private int doSwitch(CatalogInfo catalogInfo, HeaderData headerData, ItemEnum item) {
		File f = new File(catalogInfo.arcName);
		switch(item) {
		case JAVA_FILE:
			headerData.setFileName(f.getName().substring(0,f.getName().length()-".class".length())+".java" );
			headerData.setUnpSize(f.length());
			catalogInfo.nextItemToShow = item.next();
			return SUCCESS;
		case PROPERTIES_DIR:
			if (getProperty(catalogInfo, headerData, "properties")) {
				return SUCCESS;
			} else {
				catalogInfo.itemToShow = item.next();
				// do not break, let it continue to next case					
			}
		case PROPERTIES_FILE:
			headerData.setFileName("properties.property");
			headerData.setUnpSize(catalogInfo.properties.size());
			catalogInfo.nextItemToShow = catalogInfo.itemToShow.next();
			return SUCCESS;
		case ENVIRONMENT_DIR:
			if (getEnv(catalogInfo, headerData, "environment")) {
				return SUCCESS;
			} else {
				catalogInfo.itemToShow = item.next();
				// do not break, let it continue to next case					
			}
		case ENVIRONMENT_FILE:
			headerData.setFileName("environment.env");
			headerData.setUnpSize(catalogInfo.env.size());
			catalogInfo.nextItemToShow = catalogInfo.itemToShow.next();
			return SUCCESS;
		case THROWABLE:
			if (catalogInfo.blockingThrowable != null) {
				Throwable t = catalogInfo.blockingThrowable;
				String s;
				if (t.getMessage().startsWith("Prohibited package")) {
					s = "reading classes from package java is not allowed.exception";
				} else {
					s = t.getMessage() + ".exception";
				}
				headerData.setFileName(s);
				headerData.setUnpSize(s.length());
				catalogInfo.throwableToShow = catalogInfo.blockingThrowable;
				catalogInfo.nextItemToShow = ItemEnum.FINISH; // skip rest of cases
				return SUCCESS;
			} else {
				catalogInfo.itemToShow = item.next();
				// do not break, let it continue to next case					
			}
		case PACKAGE:
			Class<?> clazz = catalogInfo.clazz;
			// headerData.setFileName(clazz.getPackage() + ".package");
	        int index = clazz.getName().lastIndexOf(".");
	        if (index == -1) {
				headerData.setFileName("this class has no package defined.package");
	        } else {
		        String packaje = clazz.getName().substring(0,index);
				headerData.setUnpSize(packaje.length());		        
				headerData.setFileName(packaje + ".package");
	        }
			catalogInfo.nextItemToShow = catalogInfo.itemToShow.next();
			return SUCCESS;
		case CLASS_ELEMENTS:
			if (classToDirs(catalogInfo, headerData)) {
				return SUCCESS;
			} else {
				catalogInfo.itemToShow = item.next();
				// do not break, let it continue to next case					
			}
		default:
			return E_END_ARCHIVE;
		}
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
		return getVariables(catalogInfo, headerData, dir, catalogInfo.properties, catalogInfo.propertiesKeys, "property");
	}

	private boolean getEnv(CatalogInfo catalogInfo, HeaderData headerData, String dir) {
		return getVariables(catalogInfo, headerData, dir, catalogInfo.env, catalogInfo.envKeys, "env");
	}
	
	private boolean getVariables(CatalogInfo catalogInfo, HeaderData headerData, String dir, Map<?,?> map, Iterator<?> iterator, String ext) {
		headerData.setUnpSize(map.size());
		if (iterator.hasNext()) {
			Object key = iterator.next();
			Object value = map.get(key);
			if (log.isDebugEnabled()) log.debug("variable:" + key + "-" + value);
			String sv = value.toString().replace("\\", "[/]");
			headerData.setFileName(dir + "\\" + key + "=" + sv + "." + ext);
			return true;
		} else {
			return false;
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
	
	private int save(CatalogInfo catalogInfo, File dest, Map<?, ?> properties, boolean overwrite) {
		StringBuffer sb = new StringBuffer(); 
		properties.forEach( (k,v) -> sb.append(k).append("=").append(v).append("\n\r"));
		return save(catalogInfo, dest, sb.toString(), overwrite);
	}

	private int save (CatalogInfo cinfo, final File dest, Throwable t, final boolean overwrite) {
		if (log.isWarnEnabled()) {
			log.warn("saving throwable",t);
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		return save(cinfo, dest, sw.toString(), overwrite);
	}
	
	private int save (CatalogInfo cinfo, final File dest, String contents, final boolean overwrite) {
		if (overwrite) {
			dest.delete();
		}
		if (dest.exists()) {
			return E_ECREATE;
		}
		PrintWriter out = null;
		try {
			out = new PrintWriter(dest);
			out.print(contents);
		} catch (FileNotFoundException fnfe) {
			return E_EOPEN;
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return SUCCESS;
	}
	
	public static void main(String[]args) {
		if (log.isDebugEnabled()) {
			log.debug(Decompiler.class.getName() + ".main");
		}		
	}
}
