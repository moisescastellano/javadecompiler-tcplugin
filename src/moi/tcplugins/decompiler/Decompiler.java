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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import plugins.wcx.HeaderData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Moises Castellano 2021-2022
 * https://github.com/moisescastellano/tcmd-java-plugin
 */

public class Decompiler extends ItemsPlugin {
	
	final static Logger log = LoggerFactory.getLogger(Decompiler.class);
	
	public Decompiler() {
		setItems();
	}

	protected enum ItemEnum {
		JAVA_FILE(false), PROPERTIES_DIR(true), PROPERTIES_FILE(false),	ENVIRONMENT_DIR(true), ENVIRONMENT_FILE(false), 
		THROWABLE(true), PACKAGE(false), CLASS_ELEMENTS(true), FINISH(false);
		
		private static ItemEnum[] vals = values();

		BiPredicate<CatalogInfo, HeaderData> getter; // returns whether getting the item was successful
		BiFunction<CatalogInfo, ItemEnum, ItemEnum>  nextIfSuccess; // returns the next element if getter was sucessful
		SaverFunction saver; // saves the item
		
		private ItemEnum(boolean multiple) {
			this.nextIfSuccess = multiple ? (c,i)->i: (c,i)->i.next();
		}
	    public static ItemEnum first() {
	        return vals[0];
	    }
	    public ItemEnum next() {
	        return vals[(this.ordinal()+1)];
	    }
	    public void set(BiPredicate<CatalogInfo, HeaderData> getter, SaverFunction saver) {
	    	this.getter = getter;
	    	this.saver = saver;
	    }
	    public void setNextIfSuccess(BiFunction<CatalogInfo, ItemEnum, ItemEnum> nextIfSuccess) {
	    	this.nextIfSuccess = nextIfSuccess;
	    }
	}

	private void setItems() {
		
		SaverFunction propertiesSaver = (c,f)->save(c, f, c.properties);
		SaverFunction environmentSaver = (c,f)->save(c, f, c.env);
		SaverFunction javaSaver = (c,f)->decompile(new File(c.arcName), f);
		
		ItemEnum.THROWABLE.set((c,h)->getBlockingThrowable(c,h), (c,f)->save(c, f, c.throwableToShow));
		ItemEnum.THROWABLE.setNextIfSuccess((c,i)->ItemEnum.FINISH);
		
		ItemEnum.JAVA_FILE.set((c,h) -> getJavaFile(new File(c.arcName), h), javaSaver);
		ItemEnum.PROPERTIES_DIR.set((c,h) -> getVariables(c, h, "properties", c.properties, c.propertiesKeys, "property"), propertiesSaver);
		ItemEnum.PROPERTIES_FILE.set((c,h) -> getFile(h, "properties.property", c.properties.size()), propertiesSaver);
		ItemEnum.ENVIRONMENT_DIR.set((c,h)->getVariables(c,h,"environment",c.env, c.envKeys, "env"), environmentSaver);
		// ItemEnum.HELP_FILE.set((c,h)->getHelpFile(c,h), sameOnSuccess, helpFileSaver);
		ItemEnum.ENVIRONMENT_FILE.set((c,h)-> getFile(h, "environment.env", c.env.size()), environmentSaver);
		ItemEnum.PACKAGE.set((c,h)->getPackage(c,h), javaSaver);
		ItemEnum.CLASS_ELEMENTS.set((c,h)->classToDirs(c, h), javaSaver);
		// ItemEnum.FINISH.set(null,null,null);
	}

	static class CatalogInfo extends CatalogBase {
		
		Class<?> clazz;		

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
	
	protected void onOpenArchive(CatalogInfo catalogInfo) throws Exception {
		ByteClassLoader loader = new ByteClassLoader();
        Path path = Paths.get(catalogInfo.arcName);
		Class<?> clazz = loader.defineClass(null, Files.readAllBytes(path));
		catalogInfo.clazz = clazz;
	}

	private boolean getBlockingThrowable(CatalogInfo catalogInfo, HeaderData headerData) {
		if (catalogInfo.blockingThrowable == null) {
			return false;
		}
		Throwable t = catalogInfo.blockingThrowable;
		String s;
		if (t.getMessage().startsWith("Prohibited package")) {
			s = "reading classes from package java is not allowed.exception";
		} else {
			s = t.getMessage().replaceAll("[^a-zA-Z0-9 ]","-") + ".blocking.exception";
		}
		headerData.setFileName(s);
		headerData.setUnpSize(s.length());
		catalogInfo.throwableToShow = t;
		return true;
	}
	
	private boolean getJavaFile(File f, HeaderData h) {
		h.setFileName(f.getName().substring(0, f.getName().length()-".class".length())+".java" );
		h.setUnpSize(f.length());
		return true;
	}
	
	private boolean getFile(HeaderData h, String fileName, long unpSize) {
		h.setFileName(fileName);
		h.setUnpSize(unpSize);
		return true;
	}

	private boolean getPackage(CatalogInfo catalogInfo, HeaderData headerData) {
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
		return true;
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
	
	private int decompile(final File source, final File dest) {
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
	
	private int save(CatalogInfo catalogInfo, File dest, Map<?, ?> properties) {
		StringBuffer sb = new StringBuffer(); 
		properties.forEach( (k,v) -> sb.append(k).append("=").append(v).append("\n\r"));
		return save(catalogInfo, dest, sb.toString());
	}

	private int save (CatalogInfo cinfo, final File dest, Throwable t) {
		if (log.isWarnEnabled()) {
			log.warn("saving throwable",t);
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		return save(cinfo, dest, sw.toString());
	}
	
	private int save (CatalogInfo cinfo, final File dest, String contents) {
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
