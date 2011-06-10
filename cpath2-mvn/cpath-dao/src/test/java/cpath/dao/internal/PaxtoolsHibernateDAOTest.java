// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009-2011 Memorial Sloan-Kettering Cancer Center
 ** and University of Toronto.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.dao.internal;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHitType;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
public class PaxtoolsHibernateDAOTest {

    static Log log = LogFactory.getLog(PaxtoolsHibernateDAOTest.class);
    static PaxtoolsDAO paxtoolsDAO;
    static SimpleIOHandler exporter;

	/* test methods will use the same data (read-only, 
	 * with one exception: testImportingAnotherFileAndTestInitialization
	 * imports the same data again...)
	 */
    static {
    	DataServicesFactoryBean.createSchema("cpath2_testpc");
		// init the DAO (it loads now because databases are created above)
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-pcDAO.xml");
		paxtoolsDAO = (PaxtoolsDAO) context.getBean("pcDAO");
		
		// load some data into the test storage
		log.info("Loading BioPAX data (importModel(file))...");
		try {
	    	/* import two files to ensure it does not fail
	    	 * (suspecting a "duplicate entry for the key (rdfid)" exception) */
			paxtoolsDAO.importModel(new File(PaxtoolsHibernateDAOTest.class.getResource("/test.owl").getFile()));
			paxtoolsDAO.importModel(new File(PaxtoolsHibernateDAOTest.class.getResource("/test2.owl").getFile()));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		exporter = new SimpleIOHandler(BioPAXLevel.L3);
    }
    
	
    @Test
	public void testImportingAnotherFileAndTestInitialization() throws IOException {
		assertTrue(paxtoolsDAO.containsID("urn:miriam:uniprot:P46880"));
		assertTrue(paxtoolsDAO.containsID("http://www.biopax.org/examples/myExample2#Protein_A"));
		assertTrue(paxtoolsDAO.containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(paxtoolsDAO.containsID("http://www.biopax.org/examples/myExample#Protein_B"));
		assertTrue(paxtoolsDAO.containsID("urn:biopax:UnificationXref:Taxonomy_562"));
		
		BioPAXElement bpe = paxtoolsDAO.getByID("urn:biopax:UnificationXref:Taxonomy_562");
		assertTrue(bpe instanceof UnificationXref);
		
		BioPAXElement e = paxtoolsDAO
				.getByID("http://www.biopax.org/examples/myExample2#Protein_A");
		assertTrue(e instanceof Protein);
		
		e = paxtoolsDAO // try to initialize
		.getByID("http://www.biopax.org/examples/myExample2#Protein_A");
		paxtoolsDAO.initialize(bpe);
		Protein p = (Protein) e;
				
		assertTrue(p.getEntityReference() != null);
		assertEquals("urn:miriam:uniprot:P46880", p.getEntityReference().getRDFId());
		
		// this would fail (lazy collections)
		//assertEquals(4, p.getEntityReference().getEntityReferenceOf().size());
			
		// but when -
		e = paxtoolsDAO // try to initialize
		.getByID("urn:miriam:uniprot:P46880");
		paxtoolsDAO.initialize(e);
		assertTrue(e instanceof ProteinReference);
		ProteinReference pr = (ProteinReference) e;
		assertNotNull(pr.getOrganism());
		// however, with using getByIdInitialized, next line would fail -
		// pr.getEntityReferenceOf().size();
		// assertEquals(4, pr.getEntityReferenceOf().size());
		
		// different approach works!
		pr = (ProteinReference) paxtoolsDAO.getByID("urn:miriam:uniprot:P46880");
		//pr.getEntityReferenceOf().size() would fail here, but...
		// initialize(bpe) can be called at any time (it's bidirectional, though not recursive)
		paxtoolsDAO.initialize(pr);
		// should pass now :)
		assertEquals(4, pr.getEntityReferenceOf().size());
		assertEquals(2, pr.getName().size());
		//pr.getOrganism().getXref().size(); // would fail, hehe... but
		BioSource bs = pr.getOrganism();
		assertNotNull(bs);
		paxtoolsDAO.initialize(bs);
		assertTrue(bs.getXref().size() > 0);
	}
    
    
	@Test
	public void testSimple() throws Exception {
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe = paxtoolsDAO
			.getByID("http://www.biopax.org/examples/myExample#Protein_A");
		assertTrue(bpe instanceof Protein);
		
		bpe = paxtoolsDAO.getByID("urn:biopax:UnificationXref:UniProt_P46880");
		assertTrue(bpe instanceof UnificationXref);
	}

	
	@Test // protein reference's xref's getXrefOf() is not empty
	public void testGetXReferableAndXrefOf() throws Exception {
		ProteinReference pr = (ProteinReference) paxtoolsDAO
			.getByID("urn:miriam:uniprot:P46880");
		paxtoolsDAO.initialize(pr);
		assertTrue(pr instanceof ProteinReference);
		assertFalse(pr.getXref().isEmpty());
		Xref x = pr.getXref().iterator().next();		
		paxtoolsDAO.initialize(x);
		assertEquals(1, x.getXrefOf().size());
		System.out.println(x.getRDFId() + " is xrefOf " + 
				x.getXrefOf().iterator().next().toString()
		);
	}
	
	
	
	@Test // getXrefOf() returns empty set, but it's not a bug!
	public void testGetXrefAndXrefOf() throws Exception {
		/* 
		 * getByID normally returns an object with lazy collections, 
		 * which is usable only within the session/transaction,
		 * which is closed after the call :) So 'initialize' is required - 
		 */
		BioPAXElement bpe = paxtoolsDAO.getByID("urn:biopax:UnificationXref:UniProt_P46880");
		paxtoolsDAO.initialize(bpe);
		assertTrue(bpe instanceof UnificationXref);
		
		OutputStream out = new ByteArrayOutputStream();
		paxtoolsDAO.exportModel(out, bpe.getRDFId());
		//System.out.println("Export single Xref (incomplete BioPAX):");
		//System.out.println(out.toString());
		
		// check if it has xrefOf values...
		Set<XReferrable> xrOfs = ((UnificationXref) bpe).getXrefOf();
		assertFalse(xrOfs.isEmpty());
	}
	
	
	@Test
	public void testGetByID() throws Exception {		
		// get a protein
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe =  paxtoolsDAO.getByID(
				"http://www.biopax.org/examples/myExample#Protein_A");
		paxtoolsDAO.initialize(bpe);
		assertTrue(bpe instanceof Protein);
		assertEquals("glucokinase A", ((Protein)bpe).getDisplayName());
		EntityReference er = ((Protein)bpe).getEntityReference();
		assertNotNull(er);
		paxtoolsDAO.initialize(er);
		assertEquals(1, er.getXref().size());
		
		OutputStream out = new ByteArrayOutputStream();
		paxtoolsDAO.exportModel(out, bpe.getRDFId());
		//System.out.println("Export single protein (incomplete BioPAX):");
		//System.out.println(out.toString());
	}
	
	
	@Test
	public void testGetValidSubModel() throws Exception {	
		Model m =  paxtoolsDAO.getValidSubModel(
			Collections.singleton("http://www.biopax.org/examples/myExample#Protein_A"));
		System.out.println("Clone the protein and export model:");
		assertTrue(m.containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(m.containsID("urn:miriam:uniprot:P46880"));
		assertTrue(m.containsID("urn:biopax:UnificationXref:UniProt_P46880"));
		
		OutputStream out = new FileOutputStream(
				getClass().getClassLoader().getResource("").getPath() 
					+ File.separator + "testGetValidSubModel.out.owl");
		exporter.convertToOWL(m, out);
	}

	@Test
	public void testSerchForIDs() throws Exception {
		List<SearchHitType> elist = paxtoolsDAO.findElements("P46880", UnificationXref.class);
		assertFalse(elist.isEmpty());
		assertTrue(elist.size()==1);
	}

	
	@Test
	public void testFind() throws Exception {
		List<SearchHitType> list = paxtoolsDAO.findElements("P46880", BioPAXElement.class);
		assertFalse(list.isEmpty());
		Set<String> m = new HashSet<String>();
		for(SearchHitType e : list) {
			m.add(e.getUri());
		}
		//assertTrue(list.contains("urn:biopax:UnificationXref:UniProt_P46880"));
		assertTrue(m.contains("urn:biopax:UnificationXref:UniProt_P46880"));
		
		System.out.println("find by 'P46880' returned: " + list.toString());
		
		/* 'P46880' is used only in the PR's RDFId and in the Uni.Xref,
		 * but the find method (full-text search) must NOT match in rdf ID:
		 */
		list = paxtoolsDAO.findElements("P46880", ProteinReference.class);
		System.out.println("find by 'P46880', " +
			"filter by ProteinReference.class, returned: " + list.toString());
		
		assertTrue(list.isEmpty());
		
		list = paxtoolsDAO.findElements("glucokinase", ProteinReference.class);
		assertEquals(1, list.size());
		assertTrue(list.get(0).getUri().equals("urn:miriam:uniprot:P46880"));
	}

}
