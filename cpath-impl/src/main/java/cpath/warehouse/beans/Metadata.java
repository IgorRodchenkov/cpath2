package cpath.warehouse.beans;


import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Provenance;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;

/**
 * Data Provider Metadata.
 */
@Entity
@Table(name="metadata")
public final class Metadata {

    private static final Pattern BAD_ID_PATTERN = Pattern.compile("\\s|-");
	
	// METADATA_TYPE Enum
    public static enum METADATA_TYPE {
        // data types
        PSI_MI(true), // interactions to be converted to BioPAX L3 format
        PSI_MITAB(true), // interactions to be converted to PSI-MI then to BioPAX L3 format
		BIOPAX(true), // pathways and interactions in BioPAX L2 or L3 format
		WAREHOUSE(false), // warehouse data to be converted to BioPAX and used during the merge stage
		MAPPING(false); //extra gene/protein id-mapping data (two column, TSV format: "some id or name" \t "primary uniprot/chebi AC")
        
        private final boolean pathwayData;
        
        private METADATA_TYPE(boolean isPathwayData) {
			this.pathwayData = isPathwayData;
		}
        
        public boolean isNotPathwayData() {
			return !pathwayData;
		}
        
    }

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;
	
	@NotBlank
	@Column(length=40, unique = true, nullable = false)
    public String identifier;
	
	@NotEmpty
	@ElementCollection
	@JoinTable(name="metadata_name")
	@OrderColumn
    private List<String> name;
	
	@Column(nullable=false)
    private String description;
	
    private String urlToData;

	@Column(nullable=false)
    private String urlToHomepage;
	
	@Column(nullable=false)
    private String iconUrl;	
	
	@NotNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
    private METADATA_TYPE type;
	
    private String cleanerClassname;    
    private String converterClassname;

    @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
    @JoinColumn(name="metadata_id")
    private Set<Content> content;

    private String pubmedId;
    private String availability;
    private Integer numPathways;  
    private Integer numInteractions;
    private Integer numPhysicalEntities;    
    
    //extra (jpa transient) fields
    @Transient
    private Long numAccessed;
    @Transient
    private Boolean uploaded;
    @Transient
    private Boolean premerged;
    
    
	/**
	 * Default Constructor.
	 */
	Metadata() {
		content = new HashSet<Content>();
	}

    /**
     * Create a Metadata obj with the specified properties;
     *
     * @param identifier  unique short string, will be used in URIs
     * @param name the not empty list of names: display name (must present), standard name, other names.
     * @param description
     * @param urlToData
     * @param urlToHomepage
     * @param icon
     * @param metadata_type
     * @param cleanerClassname
     * @param converterClassname
     * @param pubmedId
     * @param availability
     * @throws IllegalArgumentException
     */
	public Metadata(final String identifier, final List<String> name, final String description, 
    		final String urlToData, String urlToHomepage, final String urlToLogo, 
    		final METADATA_TYPE metadata_type, final String cleanerClassname, 
    		final String converterClassname, 
    		final String pubmedId, final String availability) 
    {
    	this();
    	setIdentifier(identifier); 
    	if(name == null || name.isEmpty())
    		throw new IllegalAccessError("no names provided");
        setName(name);
        setDescription(description);
        setUrlToData(urlToData);
        setUrlToHomepage(urlToHomepage);
        setIconUrl(urlToLogo);
        setType(metadata_type);
        setCleanerClassname(cleanerClassname);
        setConverterClassname(converterClassname);
        setPubmedId(pubmedId);
        setAvailability(availability);
    }
    
	public Metadata(final String identifier, final String name, final String description, 
    		final String urlToData, String urlToHomepage, final String urlToLogo, 
    		final METADATA_TYPE metadata_type, final String cleanerClassname, 
    		final String converterClassname, 
    		final String pubmedId, final String availability) 
    {
		this(identifier, Arrays.asList(name.split("\\s*;\\s*")), description, urlToData, 
			urlToHomepage, urlToLogo, metadata_type, cleanerClassname, converterClassname,
			pubmedId, availability);
    }

	void setId(Integer id) {
		this.id = id;
	}
    public Integer getId() { return id; }
     

    public Set<Content> getContent() {
		return content;
	}
    void setContent(Set<Content> content) {
		this.content = new HashSet<Content>();
		this.content.addAll(content);
	}

    
    /**
	 * Sets the identifier.
	 * No spaces, dashes, allowed. 
	 * 
	 * @param identifier
	 * @throws IllegalArgumentException if it's null, empty string, or contains spaces or dashes
	 */
    void setIdentifier(String identifier) {
    	// validate the parameter    	
    	if(identifier == null 
    		|| identifier.length() == 0
    		|| BAD_ID_PATTERN.matcher(identifier).find())
    		throw new IllegalAccessError("Bad metadata identifier: " + identifier);
    		
		// copy value
    	this.identifier = identifier;
	}
	
	/**
	 * Data source metadata identifier.
	 * 
	 * It can be also used as filter ('datasource') 
	 * value in cpath2 full-text search queries
	 * (for pathway datasource types only)
	 * 
	 * @return
	 */
    public String getIdentifier() { return identifier; }

	/**
	 * Sets data provider/source name. 
	 * 
	 * Please use a standard name for pathway/interaction data types,
	 * if possible (for warehouse data it's not so important), 
	 * as this will be recommended to use as filter ('datasource') 
	 * value in cpath2 full-text search queries 
	 * 
     * @param name
     * @throws IllegalArgumentException
     */
	public void setName(List<String> name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
		this.name = name;
	}
	
	/**
	 * Gets the data provider/source name.
	 * 
	 * @return
	 */
    public List<String> getName() { return name; }


	public void setDescription(String releaseDate) {
        if (releaseDate == null) {
            throw new IllegalArgumentException("release data must not be null");
        }
        this.description = releaseDate;
	}
    public String getDescription() { return description; }

	public void setUrlToData(String urlToData) {
        this.urlToData = urlToData;
	}
    public String getUrlToData() { return urlToData; }

	public void setUrlToHomepage(String urlToHomepage) {
        this.urlToHomepage = urlToHomepage;
	}
    public String getUrlToHomepage() { return urlToHomepage; } 

    
	public void setType(METADATA_TYPE metadata_type) {
        if (metadata_type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.type = metadata_type;
	}
    public METADATA_TYPE getType() { return type; }

	public void setCleanerClassname(String cleanerClassname) {
		if(cleanerClassname == null || cleanerClassname.trim().length() == 0)
			this.cleanerClassname = null;
		else 
			this.cleanerClassname = cleanerClassname.trim();
	}
    public String getCleanerClassname() { 
    	return (cleanerClassname == null || cleanerClassname.length() == 0) 
    			? null : cleanerClassname; 
    }

	public void setConverterClassname(String converterClassname) {
		if(converterClassname == null || converterClassname.trim().length() == 0)
			this.converterClassname = null;
		else
			this.converterClassname = converterClassname.trim();
	}
    public String getConverterClassname() { 
    	return (converterClassname == null || converterClassname.length() == 0) 
    			? null : converterClassname; 
    }

    
    @Override
    public String toString() {
        return identifier;
    }
    
    
	/**
	 * Full path to the input data file/archive
	 * in the data directory.
	 * 
	 * @see CPathSettings#dataDir()
	 * @return
	 */
    @Transient
    public String getDataArchiveName() {
    	return CPathSettings.getInstance()
    		.dataDir() + File.separator + identifier + ".zip";
    }
    void setDataFile(String filename) {throw new UnsupportedOperationException();}
    

    /**
     * Path to the directory where processed data 
     * (converted/normalized and validation)
     * are saved.
     * 
     * @return
     */
    @Transient
    public String outputDir() {
    	return CPathSettings.getInstance().dataDir() + File.separator + identifier;
    }
    
    
    /**
     * Generate a URI (for a Provenance instance.)
     * 
     * @return
     */
    @Transient
    public String getUri() {
    	return CPathSettings.getInstance().getXmlBase() + identifier;
    }

    
	/**
	 * Creates a new Provenance from this Metadata and sets
	 * if to all Entity class objects in the model. 
	 * 
	 * Removes all other Provenance instances and 
	 * corresponding dataSource property values
	 * from the model.
	 * 
	 * @param model BioPAX model to update
	 */
	public void setProvenanceFor(Model model) {
		Provenance pro = null;
		
		// we create URI from the Metadata identifier and version.
		final String uri = getUri();
		pro = (model.containsID(uri)) 
			? (Provenance) model.getByID(uri)
			: model.addNew(Provenance.class, uri);
		
		// parse/set names
		String displayName = getName().iterator().next();
		pro.setDisplayName(displayName);
		pro.setStandardName(standardName());
		
		if(getName().size() > 2)
			for(int i=2; i < getName().size(); i++)
				pro.addName(getName().get(i));
		
		// add additional info about the current version, source, identifier, etc...
		final String loc = getUrlToData(); 
		pro.addComment("Source " + 
			//skip for a local or empty (default) location
			((loc.startsWith("http:") || loc.startsWith("ftp:")) ? loc : "") 
			+ " type: " + getType() + ", " + getDescription());
		
		// replace for all entities
		for (org.biopax.paxtools.model.level3.Entity ent : model.getObjects(org.biopax.paxtools.model.level3.Entity.class)) {
			for(Provenance ds : new HashSet<Provenance>(ent.getDataSource()))
				ent.removeDataSource(ds);			
			ent.addDataSource(pro);
		}
	}
	
	/**
	 * Returns the standard name (the second one in the name list),
	 * if present, otherwise - returns the first name (display name)
	 * 
	 * @return
	 */
	public String standardName() {
		//also capitalize (can be extremely useful...)
		if(name.size() > 1)
			return StringUtils.capitalize(name.get(1));
		else 
			return StringUtils.capitalize(name.get(0));
	}

	public Integer getNumPathways() {
		return numPathways;
	}

	public void setNumPathways(Integer numPathways) {
		this.numPathways = numPathways;
	}

	public Integer getNumInteractions() {
		return numInteractions;
	}

	public void setNumInteractions(Integer numInteractions) {
		this.numInteractions = numInteractions;
	}

	public Integer getNumPhysicalEntities() {
		return numPhysicalEntities;
	}

	public void setNumPhysicalEntities(Integer numPhysicalEntities) {
		this.numPhysicalEntities = numPhysicalEntities;
	}
	
	public String getPubmedId() {
		return pubmedId;
	}

	public void setPubmedId(String pubmedId) {
		this.pubmedId = pubmedId;
	}

	public String getAvailability() {
		return availability;
	}

	public void setAvailability(String availability) {
		this.availability = availability;
	}
	
	public String getIconUrl() {
		return iconUrl;
	}
	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	@Transient
	public Long getNumAccessed() {
		return numAccessed;
	}
	public void setNumAccessed(Long numAccessed) {
		this.numAccessed = numAccessed;
	}
	
	@Transient
	public Boolean getUploaded() {
		return uploaded;
	}
	public void setUploaded(Boolean uploaded) {
		this.uploaded = uploaded;
	}

	@Transient
	public Boolean getPremerged() {
		return premerged;
	}
	public void setPremerged(Boolean premerged) {
		this.premerged = premerged;
	}

	@Transient
	public boolean isNotPathwayData() {
		return type.isNotPathwayData();
	}	
	public void setNotPathwayData(boolean foo) {
		//a fake to make it bean property (to use in javascript, JSON)
	}
			
	/**
	 * Drops all associated output data files - 
	 * re-creates the output data directory.
	 */
	public void cleanupOutputDir() {
		File dir = new File(outputDir());
		if(dir.exists()) {
			CPathUtils.cleanupDirectory(dir);
		}		
		dir.mkdir();
	}
	
	@Override
	public boolean equals(Object o) {
		return (o instanceof Metadata) && identifier.equals(((Metadata)o).getIdentifier());
	}
	
	@Override
	public int hashCode() {
		return (getClass().getCanonicalName() + identifier).hashCode();
	}

}
