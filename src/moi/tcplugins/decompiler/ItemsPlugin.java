package moi.tcplugins.decompiler;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import moi.tcplugins.VersionCheck;
import moi.tcplugins.decompiler.Decompiler.CatalogInfo;
import moi.tcplugins.decompiler.Decompiler.ItemEnum;
import plugins.wcx.HeaderData;
import plugins.wcx.OpenArchiveData;
import plugins.wcx.WCXPluginAdapter;

public abstract class ItemsPlugin extends WCXPluginAdapter {

	final static Logger log = LoggerFactory.getLogger(ItemsPlugin.class);
	final static int[] pluginClassLoaderVersion = {2,2,0};
	
	@FunctionalInterface
	public interface SaverFunction {
	    int apply(CatalogInfo t, File u) throws Exception;
	}

	protected static class CatalogBase {
		/**
		 * The name of the archive.
		 */
		public String arcName;		
	
		public Throwable blockingThrowable;
		public Throwable throwableToShow;
		
		public ItemEnum itemToShow; 
		public ItemEnum nextItemToShow; 
	}

	@Override
	public Object openArchive(OpenArchiveData archiveData) {
		if (log.isDebugEnabled()) log.debug(".openArchive(archiveData)");
		CatalogInfo catalogInfo = new CatalogInfo();
		catalogInfo.arcName = archiveData.getArcName();			
		try {
			VersionCheck.checkPluginClassLoaderVersion(pluginClassLoaderVersion);
			catalogInfo.nextItemToShow = ItemEnum.first();
			onOpenArchive(catalogInfo);
		} catch (Throwable e) {
			/*
			JFrame frame = new JFrame();
			JOptionPane.showMessageDialog(frame, e.getMessage(), "Error on openArchive", JOptionPane.ERROR_MESSAGE);
			*/
			catalogInfo.blockingThrowable = e;
		}
		return catalogInfo;
	}
	
	protected abstract void onOpenArchive(CatalogInfo catalogInfo) throws Exception;

	@Override
	public int closeArchive(Object archiveData) {
		if (log.isDebugEnabled()) log.debug(".closeArchive(archiveData)");
		return SUCCESS;
	}

	@Override
	public int processFile(Object archiveData, int operation, String destPath, String destName) {
		if (log.isDebugEnabled()) log.debug(this.getClass().getName() + ".processFile(archiveData, operation=["+operation+"], destPath=["+destPath+"];destName=["+destName+"]");
		CatalogInfo catalogInfo = (CatalogInfo) archiveData;
		String fullDestName = (destPath==null?"":destPath) + destName;
		try {
			if (operation == PK_EXTRACT) {
				if (log.isDebugEnabled()) log.debug(".processFile() EXTRACT from:[" + catalogInfo.arcName + "] to: [" + fullDestName + "]");
				return catalogInfo.itemToShow.saver.apply(catalogInfo, new File(fullDestName));
			} else if (operation == PK_TEST) {
				if (log.isDebugEnabled()) log.debug(".processFile() TEST " + (destPath==null?"":destPath) + destName);
			} else if (operation == PK_SKIP) {
				if (log.isDebugEnabled()) log.debug(".processFile() SKIP " + (destPath==null?"":destPath) + destName);
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			try {
				/*
				JFrame frame = new JFrame();
				JOptionPane.showMessageDialog(frame, t.getMessage(), "Error on processFile", JOptionPane.ERROR_MESSAGE);
				*/
				catalogInfo.throwableToShow = t;
				return ItemEnum.THROWABLE.saver.apply(catalogInfo, new File(fullDestName));
			} catch (Throwable t2) {
				log.error(t.getMessage(), t2);
				return E_EWRITE;
			}
		}
		return SUCCESS;
	}

	@Override
	public int readHeader(Object archiveData, HeaderData headerData) {
		CatalogInfo catalogInfo = (CatalogInfo) archiveData;
		if (log.isDebugEnabled()) log.debug(this.getClass().getName() + ".readHeader(archiveData, headerData) " + catalogInfo.itemToShow);
		try {
			catalogInfo.itemToShow = catalogInfo.nextItemToShow;
			if (catalogInfo.itemToShow == ItemEnum.FINISH) {
				return E_END_ARCHIVE;
			}
			while(!catalogInfo.itemToShow.getter.test(catalogInfo, headerData)) {
				catalogInfo.itemToShow = catalogInfo.itemToShow.next();
				if (catalogInfo.itemToShow == ItemEnum.FINISH) {
					return E_END_ARCHIVE;
				}
			}
			catalogInfo.nextItemToShow = catalogInfo.itemToShow.nextIfSuccess.apply(catalogInfo,catalogInfo.itemToShow);
			if (log.isDebugEnabled()) log.debug(".readHeader: nextItem:" + catalogInfo.nextItemToShow);
			return SUCCESS;
		} catch (Throwable t) {
			if (log.isErrorEnabled()) log.error(t.getMessage(), t);
			String msg = t.getMessage().replaceAll("[^a-zA-Z0-9 ]","-");
			headerData.setFileName(msg + ".exception");
			headerData.setUnpSize(msg.length());
			if (catalogInfo.itemToShow == null) {
				catalogInfo.nextItemToShow = ItemEnum.FINISH;
			} else {
				catalogInfo.nextItemToShow = catalogInfo.itemToShow.next();
			}
			catalogInfo.throwableToShow = t;
			catalogInfo.itemToShow = ItemEnum.THROWABLE;
			if (log.isDebugEnabled()) log.debug(".readHeader: SUCCESS after throwable");
			return SUCCESS;
		}
	}

	@Override
	public int getPackerCaps() {
		return /* PK_CAPS_HIDE | PK_CAPS_NEW | */ PK_CAPS_MULTIPLE | PK_CAPS_MEMPACK;
	}



}
