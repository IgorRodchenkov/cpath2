import java.io.IOException;
import java.util.*;

import cpath.config.CPathSettings;
import org.biopax.validator.api.beans.Validation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.jpa.Content;
import cpath.jpa.Mapping;
import cpath.jpa.Metadata;
import cpath.jpa.Metadata.METADATA_TYPE;
import cpath.service.CPathService;
import static org.junit.Assert.*;

/**
 *
 * @author rodche
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
		"classpath:META-INF/spring/applicationContext-jpa.xml"})
//@ActiveProfiles("default") //develop
public class RepositoriesAndServiceTest {
	
	@Autowired
	private CPathService service;


	@DirtiesContext
	@Test
	public final void testLogGa() {
		service.track("172.20.10.3","command","search");
		service.track("172.20.10.3","provider","Reactome");
		service.track("172.20.10.3","error","bad request");
		service.track("172.20.10.3","client",null);
	}

	@Test
	@DirtiesContext
	public void testIdMapping() {		
        //capitalization is important in 99% of identifier types (we should not ignore it)
        // we should be able to save it and not get 'duplicate key' exception here
		service.mapping().save(new Mapping("GeneCards", "ZHX1", "UNIPROT", "P12345"));
		service.mapping().save(new Mapping("GeneCards", "ZHX1-C8orf76", "UNIPROT", "Q12345"));
		service.mapping().save(new Mapping("GeneCards", "ZHX1-C8ORF76", "UNIPROT", "Q12345"));
		assertEquals(1, service.mapping()
			.findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCase("GeneCards", "ZHX1-C8ORF76", "UNIPROT").size());

        //check it's saved
        assertEquals(1, service.map("ZHX1-C8orf76", "UNIPROT").size());
        assertEquals(1, service.map("ZHX1-C8ORF76", "UNIPROT").size());
        
        // repeat (should successfully update)- add a Mapping
        service.mapping().save(new Mapping("TEST", "FooBar", "CHEBI", "CHEBI:12345"));
        assertTrue(service.map("FooBar", "UNIPROT").isEmpty());

        Set<String> mapsTo = service.map("FooBar", "CHEBI");
        assertEquals(1, mapsTo.size());
        assertEquals("CHEBI:12345", mapsTo.iterator().next());
		mapsTo = service.map("FooBar", "CHEBI");
		assertEquals(1, mapsTo.size());
		assertEquals("CHEBI:12345", mapsTo.iterator().next());

        //test that service.map(..) method can map isoform IDs despite they're not explicitly added to the mapping db
		service.mapping().save(new Mapping("UNIPROT", "A2A2M3", "UNIPROT", "A2A2M3"));
		assertEquals(1, service.map("A2A2M3-1", "UNIPROT").size());
		assertEquals(1, service.map("A2A2M3", "UNIPROT").size());
        
        Mapping m = new Mapping("PubChem-substance", "14438", "CHEBI", "CHEBI:20");
        service.mapping().save(m);
		assertEquals("SID:14438", m.getSrcId());
		assertEquals(1, service.map("SID:14438", "CHEBI").size());

		//map from a list of IDs to target ID type (UNIPROT)
		List<String> srcIds = new ArrayList<String>();
		//add IDs - both map to the same uniprot ID ()
		srcIds.add("ZHX1");
		srcIds.add("A2A2M3");
		//currently, mapping().find* methods cannot map uniprot isoform IDs (service.map(..) - can do by removing the suffix)
		List<Mapping> mappings = service.mapping().findBySrcIdInAndDestIgnoreCase(srcIds, "UNIPROT");
		assertEquals(2, mappings.size());
		//test new service.map(null, srcIds, "UNIPROT"), which must also support isoform IDs
		srcIds.remove("A2A2M3");
		srcIds.add("A2A2M3-1");
		assertEquals(2, service.map(srcIds, "UNIPROT").size());
	}
	
	@Test
	@DirtiesContext
	public void testImportContent() throws IOException {
        // mock metadata and pathway data
        Metadata md = new Metadata("TEST", "test", "test", "", "",
        		"", METADATA_TYPE.BIOPAX, null, null, null, "free");        
        
        //cleanup previous tests data if any
        md = service.clear(md);
        
        Content content = new Content(md, "test 0"); //space will be replaced
        md.getContent().add(content);
        //add the second pd (for the tests at the end of this method)
        final Content pd = new Content(md, "test1");
        md.getContent().add(pd);
               
        // persist
        service.save(md);
        
        // test pathwaydata content is not accidentally erased
        Iterator<Content> it = md.getContent().iterator();
        content = it.next();
        //we want test0 for following assertions
        if("test1".equals(content.getFilename()))
        	content = it.next();
        assertEquals("test_0",content.getFilename());
        
        //even if we update from the db, data must not be empty
        md = service.metadata().findByIdentifier(md.getIdentifier());
        assertNotNull(md);
        assertEquals("TEST", md.getIdentifier());
        assertEquals(2, md.getContent().size()); 
        it = md.getContent().iterator();
        content = it.next();
        //we want test0 for following assertions
        if("test1".equals(content.getFilename()))
        	content =it.next();
        assertEquals("test_0",content.getFilename());

        // add validation result());  
        for(Content o : md.getContent())
        	o.saveValidationReport(new Validation(null));        
        // update
        service.save(md);
         
        //read the latest state
        md = service.metadata().findByIdentifier("TEST");
        assertNotNull(md);
        Set<Content>  lpd = md.getContent();
        assertFalse(lpd.isEmpty());
        content = lpd.iterator().next();
        assertNotNull(content);  
        
        //cleanup
        md = service.clear(md);
        assertTrue(md.getContent().isEmpty()); 
        md = service.metadata().findByIdentifier("TEST");
        assertTrue(md.getContent().isEmpty());         
	}
}