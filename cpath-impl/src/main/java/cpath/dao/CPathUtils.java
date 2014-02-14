/**
 * 
 */
package cpath.dao;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import cpath.config.CPathSettings;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

/**
 * @author rodche
 *
 */
public final class CPathUtils {
	// logger
    private static Logger LOGGER = LoggerFactory.getLogger(CPathUtils.class);
	
    // some bits for metadata reading
    private static final int METADATA_IDENTIFIER_INDEX = 0;
    private static final int METADATA_NAME_INDEX = 1;
    private static final int METADATA_DESCRIPTION_INDEX = 2;
    private static final int METADATA_DATA_URL_INDEX = 3;
    private static final int METADATA_HOMEPAGE_URL_INDEX = 4;
    private static final int METADATA_ICON_URL_INDEX = 5;
    private static final int METADATA_TYPE_INDEX = 6;
	private static final int METADATA_CLEANER_CLASS_NAME_INDEX = 7;
	private static final int METADATA_CONVERTER_CLASS_NAME_INDEX = 8;
    private static final int NUMBER_METADATA_ITEMS = 9;
	
	// used in unzip method
	private static final int BUFFER = 2048;

	// used for md5sum display
	static final byte[] HEX_CHAR_TABLE = {
		(byte)'0', (byte)'1', (byte)'2', (byte)'3',
		(byte)'4', (byte)'5', (byte)'6', (byte)'7',
		(byte)'8', (byte)'9', (byte)'a', (byte)'b',
		(byte)'c', (byte)'d', (byte)'e', (byte)'f'
    }; 
    
	// LOADER can handle file://, ftp://, http://  PROVIDER_URL resources
	private static final ResourceLoader LOADER = new DefaultResourceLoader();
		
	
	private CPathUtils() {
		throw new AssertionError("Not instantiable");
	}

	
    /**
     * Deletes a directory and all files there.
     * 
     * @param path
     * @return
     */
    public static boolean deleteDirectory(File path) {
    	cleanupDirectory(path);
        return( path.delete() );
    }
 
    
    /**
     * Empties the directory.
     * 
     * @param path
     * @return
     */
    public static void cleanupDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
            	 cleanupDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
    }
    
    
    /**
     *  For the given url, returns a collection of Metadata Objects.
     *
     * @param url String
     * @return Collection<Metadata>
     */
    public static Collection<Metadata> readMetadata(final String url)
    {
        // order of lines/records in the Metadata table does matter (since 2013/03);
		// so List is used here instead of HashSet
		List<Metadata> toReturn = new ArrayList<Metadata>();

        // check args
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }

        // get data from service
        BufferedReader reader = null;
        try {
            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(
            	LOADER.getResource(url).getInputStream(), "UTF-8"));

            // are we ready to read?
            while (reader.ready()) 
            {
                // grab a line
                String line = reader.readLine();
                if("".equals(line.trim()))
                	continue;
                else if(line.trim().startsWith("#")) {
					LOGGER.info("readMetadata(), line: " + line);
                	continue; //ignore/skip parsing
                }
                	
                /* for now, assume line is delimited into 9 columns by '\t' (tab);
                 * empty strings in the middle (the result of using \t\t) and 
                 * trailing empty string after the last tabulation (i.e., Converter 
                 * class name, if any), will be added to the tokens array as well.
                 */
                String[] tokens = line.split("\t",-1);
                
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("readMetadata(), token size: " + tokens.length);
					for (String token : tokens) {
						LOGGER.debug("readMetadata(), token: " + token);
					}
				}

                assert tokens.length == NUMBER_METADATA_ITEMS : "readMetadata(): " +
                		"wrong number of columns, " + tokens.length + " instead of "
                		+ NUMBER_METADATA_ITEMS + ", in the metadata record: " + line;


				// get metadata type
				Metadata.METADATA_TYPE metadataType = Metadata.METADATA_TYPE.valueOf(tokens[METADATA_TYPE_INDEX]);

				// get icon data from service
				LOGGER.info("readMetadata(): fetching icon data from: " 
					+ tokens[METADATA_ICON_URL_INDEX]);
				byte[] iconData = null;
				try {
					InputStream stream = LOADER.getResource(tokens[METADATA_ICON_URL_INDEX]).getInputStream();
					BufferedImage image = ImageIO.read(stream);
					if(image != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(image, "gif", baos);
						baos.flush();
						iconData = baos.toByteArray();
					}
				} catch (IOException e) {
					LOGGER.error("readMetadata(): Cannot load image from " 
							+  tokens[METADATA_ICON_URL_INDEX] + ". Skipping. " + e);
				}
					
				if (iconData == null) { 
					LOGGER.info("readMetadata(): missing or unaccessible " +
						"data (icon) to create Metadata bean: iconData.");
					iconData = new byte[]{};
				}
					
				LOGGER.debug("readMetadata(): make a Metadata bean.");

                // create a metadata bean
                Metadata metadata = new Metadata(
                		tokens[METADATA_IDENTIFIER_INDEX], 
                		tokens[METADATA_NAME_INDEX],
                		tokens[METADATA_DESCRIPTION_INDEX], 
                		tokens[METADATA_DATA_URL_INDEX],
                        tokens[METADATA_HOMEPAGE_URL_INDEX], 
                        iconData, 
                        metadataType,
						tokens[METADATA_CLEANER_CLASS_NAME_INDEX],
						tokens[METADATA_CONVERTER_CLASS_NAME_INDEX]);
           
                
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("readMetadata(): adding Metadata: "
					+ "identifier=" + metadata.getIdentifier() 
					+ "; name=" + metadata.getName()
					+ "; date/comment=" + metadata.getDescription()
					+ "; location=" + metadata.getUrlToData()
					+ "; icon=" + tokens[METADATA_ICON_URL_INDEX]
					+ "; type=" + metadata.getType()
					+ "; cleaner=" + metadata.getCleanerClassname() 
					+ "; converter=" + metadata.getConverterClassname());
				}
					
				// add metadata object toc collection we return
				toReturn.add(metadata);
            } 
        }
        catch (java.io.UnsupportedEncodingException e) {
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            closeQuietly(reader);
        }

        return toReturn;
    }


    /**
     *  For the given Metadata, unpacks and reads the corresponding 
     *  data file(s), creating new {@link PathwayData} objects; adds 
     *  them to the metadata's pathwayData collection.
     *
	 * @param metadata Metadata
     * @throws RuntimeException if an IO error occurs
     */
    public static void readPathwayData(final Metadata metadata) 
    {
		Collection<PathwayData> pathwayDataCollection = new HashSet<PathwayData>();
		String url = metadata.origDataLocation();
		
		try {
		
		BufferedInputStream bis = new BufferedInputStream(LOADER.getResource(url).getInputStream());
		
		// pathway data is either owl, zip (multiple files allowed!) or gz (single data entry only)
		if(url.toLowerCase().endsWith(".zip")) {
			LOGGER.info("getProviderPathwayData(): extracting data from zip archive.");
			pathwayDataCollection = readZipContent(metadata, new ZipInputStream(bis));
		} else { 
			// expected content: one-file BioPAX or PSI-MI (compressed or not)
			InputStream is;
			if(url.toLowerCase().endsWith(".gz")) {
				LOGGER.info("getProviderPathwayData(): extracting data from gzip archive.");
				is = new GZIPInputStream(bis);
			} else {
				LOGGER.info("getProviderPathwayData(): returning as is (supposed to be RDF+XML)");
				is = bis;
			}
			
			byte[] bytes = readContent(is);
			
			if(bytes.length > 0) {
				//create a new base output filename (for future pathway data results)
				int idx = url.lastIndexOf('/'); //-1 (not found) is no problem
				String filename = url.substring(idx+1); //removed orig. path
				if(filename.isEmpty())
					filename = metadata.getIdentifier();
				PathwayData pathwayData = new PathwayData(metadata, filename);
				pathwayData.setData(bytes);
				pathwayDataCollection.add(pathwayData);
			}
		} 
		
		} catch (IOException e) {
			throw new RuntimeException("readPathwayData failed reading from: " 
					+ metadata.getIdentifier() , e);
		}
		
		if(pathwayDataCollection != null && !pathwayDataCollection.isEmpty())
			metadata.getPathwayData().addAll(pathwayDataCollection);
		else
			LOGGER.warn("readPathwayData: no data found for " + metadata);
    }

	
    /*
     * Reads the input stream content.
     * 
     * @param inputStream plain text (uncompressed) data stream
     * @param useLineSeparator if true, adds back newline symbols
     * @return
     * @throws IOException
     */
    private static byte[] readContent(final InputStream inputStream, boolean useLineSeparator) 
    	throws IOException 
    {
        BufferedReader reader = null;
		StringBuilder sbuff = new StringBuilder();
		final String NEWLINE = System.getProperty ( "line.separator" );
        try {
            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            // are we ready to read?
            while (reader.ready()) {
                sbuff.append(reader.readLine());
                // NEWLINE is critical for the cleaner/converter
                if(useLineSeparator)
                	sbuff.append(NEWLINE);
			}
        } finally { closeQuietly(reader); }

        String fetchedData = sbuff.toString();
		
		return fetchedData.getBytes("UTF-8");
	}

    
    private static byte[] readContent(final InputStream inputStream) 
        	throws IOException 
    {
    	return readContent(inputStream, true);
    }
        
    /*
     * Given a zip stream, unzips it into individual 
     * files and creates PathwayData objects from each
     * 
     * @param metadata
     * @return
     * @throws IOException
     */
    private static Collection<PathwayData> readZipContent(final Metadata metadata, final ZipInputStream zis) 
    		throws IOException 
    {
    	Collection<PathwayData> toReturn = new HashSet<PathwayData>();
    	
        try {
			// interate over zip entries
			ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) 
            {
            	String entryName = entry.getName();
           		LOGGER.info("Processing zip entry: " + entryName);

				// write file to buffered outputstream
				int count;
				byte data[] = new byte[BUFFER];
				// use string builder to get over heap issue when
				// converting from byte[] to string in PathwayData constructor
				// we'll continue using bos to easily get digest 
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				BufferedOutputStream dest = new BufferedOutputStream(bos, BUFFER);
				while ((count = zis.read(data, 0, data.length)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();

				String content = bos.toString("UTF-8");
				// quick fix: skip undesired entries
				if(entry.isDirectory() 
						|| !( content.contains("RDF")
								&& content.contains("biopax.org/release/biopax")
							)
					) 
				{
            		LOGGER.info("Skipping not BioPAX (owl) zip entry: " 
            			+ entryName);
					continue;
				}
				
				// create pathway data object
				LOGGER.info("unzip(), adding pathwaydata entry: " 
					+ entryName + " of " + metadata.getIdentifier());
				PathwayData pathwayData = new PathwayData(metadata, entryName);
				pathwayData.setData(content.getBytes());
				
				// add object to return collection
				toReturn.add(pathwayData);
            }
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            closeQuietly(zis);
        }
        
        return toReturn;
    }

    
   /**
    * Close the InputStream quietly.
    * @param is
    */
    private static void closeQuietly(final InputStream is) {
    	try{is.close();}catch(Exception e){LOGGER.warn("is.close() failed." + e);}
    }

    /**
     * Close the OutputStream quietly.
     * @param os
     */
     private static void closeQuietly(final OutputStream os) {
         try{os.close();}catch(Exception e){LOGGER.warn("os.close() failed." + e);}
     }
    
   /**
    * Close the reader quietly.
    * @param reader
    */
    private static void closeQuietly(final Reader reader) {
    	try{reader.close();}catch(Exception e){LOGGER.warn("reader.close() failed." + e);}
    }
    
    
    /**
     * Compresses (gzip) and writes data 
     * to the file.
     * 
     * @param file
     * @param bytes
     * @throws RuntimeException when there was an IO problem
     */
    public static void zwrite(String file, byte[] bytes) {
    	if(!file.endsWith(".gz"))
    		LOGGER.warn("zwrite: file ext. is not '.gz'");
    	
    	GZIPOutputStream os = null;
    	try {
    		os = new GZIPOutputStream(new FileOutputStream(file));
    		os.write(bytes);
    		os.flush();
    	} catch (IOException e) {
    		throw new RuntimeException("zwrite: failed", e);
    	} finally { closeQuietly(os); }
    }

    
    /**
     * Reads content of gzip-compressed file.
     * 
     * @param file
     * @return
     * @throws RuntimeException when there was an IO problem
     */
    public static byte[] zread(String file) {
    	if(!file.endsWith(".gz"))
    		LOGGER.warn("zread: file ext. is not '.gz'");
    	
    	try {  	
    		return readContent(new GZIPInputStream(new FileInputStream(file)), false);
    	} catch (IOException e) {
    		LOGGER.error("zread: failed", e);
    		return null;
    	}   	
    }


	public static void cleanupIndexDir(String db) {
		cleanupDirectory(new File(CPathSettings.homeDir() + File.separator + db));
		LOGGER.info("Emptied the index dir:" + db);
	}

	
	/**
	 * Replaces the URI of a BioPAX object
	 * using java reflection. Normally, one should avoid this;
	 * please use when absolutely necessary and with great care. 
	 * 
	 * @param model
	 * @param el
	 * @param newRDFId
	 */
	public static  void replaceID(Model model, BioPAXElement el, String newRDFId) {
		if(el.getRDFId().equals(newRDFId))
			return; // no action required
		
		model.remove(el);
		try {
			Method m = BioPAXElementImpl.class.getDeclaredMethod("setRDFId", String.class);
			m.setAccessible(true);
			m.invoke(el, newRDFId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		model.add(el);
	}
	
	
	/**
	 * Loads the BioPAX model from a Gzip archive 
	 * previously created by the same cpath2 instance.
	 * 
	 * @return big BioPAX model
	 */
	public static Model importFromTheArchive() {
		//TODO ? an option to load other archives (e.g., Reactome only, for testing)
		final String archive = CPathSettings.biopaxExportFileName("All"); 
		
		Model model = null;

		try {
			//read from ..All.BIOPAX.owl.gz archive
			LOGGER.info("Loading the BioPAX Model from " + archive);
			model = (new SimpleIOHandler(BioPAXLevel.L3))
					.convertFromOWL(new GZIPInputStream(new FileInputStream(archive)));
		} 
		catch (IOException e) {
			LOGGER.error("Failed to import model from " + archive, e);
		}

		return model;
	}
	
	
	/**
	 * Downloads a file (content) from Internet
	 * and saves in the cpath2 home directory. The content
	 * can be anything, but only single-file GZIP archives 
	 * can be optionally expanded with this method, before saved.
	 * 
	 * @param url remote URL
	 * @param localFileName name or relative path and name
	 * @param unpack if true, expands the archive
	 * @param replace
	 * @return bytes saved or 0 if existed before file weren't replaced
	 * @throws IOException
	 */
	public static long download(String url, String localFileName, 
			boolean unpack, boolean replace) throws IOException {
		
		File localFile = new File(CPathSettings.homeDir() 
				+ File.separator + localFileName);
		
		if(localFile.exists() && !replace) {
			LOGGER.info("Keep existing " + localFileName);
			return 0L;
		}
		
		Resource resource = LOADER.getResource(url);
        long size = 0; 
        if(resource.isReadable()) {
        	size = resource.contentLength();
        	LOGGER.info(url + " content length= " + size);
        }       
        if(size < 0) 
        	size = 100 * 1024 * 1024 * 1024;
        
        //downoad to a tmp file
        ReadableByteChannel source = Channels.newChannel(resource.getInputStream());      
       	File tmpf = File.createTempFile("cpath2_", ".download");
       	tmpf.deleteOnExit();
        FileOutputStream dest = new FileOutputStream(tmpf);        
        size = dest.getChannel().transferFrom(source, 0, size);
        dest.close();
        LOGGER.info(size + " bytes downloaded from " + url);
        
        if(unpack) {
        	GZIPInputStream ginstream = new GZIPInputStream(new FileInputStream(tmpf));
        	FileOutputStream outstream = new FileOutputStream(localFile);
        	byte[] buf = new byte[1024]; 
        	int len;
        	while ((len = ginstream.read(buf)) > 0) 
        		outstream.write(buf, 0, len);
        	ginstream.close();
        	outstream.close();
        } else {
        	if(replace)
        		if(localFile.exists() && !localFile.delete())
            		throw new RuntimeException("Failed to delete old " 
            			+ localFile.getAbsolutePath());
        	if(!tmpf.renameTo(localFile))
        		throw new RuntimeException("Failed to move " 
        			+ tmpf.getAbsolutePath() + " to " + localFile.getAbsolutePath());
        }
              
        return size;
	}
		
}