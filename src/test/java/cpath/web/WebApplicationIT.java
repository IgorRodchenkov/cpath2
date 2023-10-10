package cpath.web;

import cpath.service.api.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles({"web"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApplicationIT {

	@Autowired
	private TestRestTemplate template;

	@Autowired
	private Service service;

	@BeforeEach
	public synchronized void init() {
		if(service.getModel() == null) {
			service.init();
		}
	}

	@Test
	public void testGetTypes() {
		String result = template.getForObject("/help/types", String.class);
		assertNotNull(result);
		assertTrue(result.contains("{\"id\":\"types\",\"title\":\"BioPAX classes\""));
	}

//	@Test
//	public void getSearchPathway() {
//		String result = template.getForObject("/search?type={t}&q={q}", String.class, "Pathway", "Gly*");
//		//note: pathway and Pathway both works (both converted to L3 Pathway class)
//		assertNotNull(result);
//		assertTrue(result.contains("Pathway50"));
//	}

	@Test
	public void postSearchPathway() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = """
    		{
    			"type": "pathway",
    			"q": "Gly*"
    		}
				""";
		HttpEntity<String> req = new HttpEntity<>(body, headers);
		String result = template.postForObject("/search", req, String.class);
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}

//	@Test
//	public void fetchToSbgn()  {
//		String result = template.getForObject("/fetch?uri={uri}&format=sbgn",
//				String.class, "http://www.biopax.org/examples/myExample#Pathway50");
//		assertNotNull(result);
//		assertTrue(result.contains("<glyph class=\"process\""));
//	}

	@Test
	public void postFetchToSbgn() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = """
    		{
    			"uri": [
    				"http://www.biopax.org/examples/myExample#Pathway50"
    			],
    			"format": "sbgn"
    		}
				""";
		HttpEntity<String> req = new HttpEntity<>(body, headers);
		String result = template.postForObject("/fetch", req, String.class);
		assertNotNull(result);
		assertTrue(result.contains("<glyph class=\"process\""));
	}

//	@Test
//	public void getFetchQuery() {
//		String result = template.getForObject("/fetch?uri={uri}", String.class, "bioregistry.io/uniprot:P27797");
//		assertNotNull(result);
//		assertTrue(result.contains("<bp:ProteinReference rdf:about=\"bioregistry.io/uniprot:P27797\""));
//	}

	@Test
	public void postFetchQuery() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = """
    		{
    			"uri": [
    				"bioregistry.io/uniprot:P27797"
    			]
    		}
				""";
		HttpEntity<String> req = new HttpEntity<>(body, headers);
		String result = template.postForObject("/fetch", req, String.class);
		assertNotNull(result);
		assertTrue(result.contains("<bp:ProteinReference rdf:about=\"bioregistry.io/uniprot:P27797\""));
	}

}
