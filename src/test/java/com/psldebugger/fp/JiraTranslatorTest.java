package com.psldebugger.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.User;

import com.psldebugger.fp.gateway.JiraClient;

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
public class JiraTranslatorTest {

    @Autowired
    private JiraTranslator translator;
    
	@Value("${exporter.jira.prefix}")
	private String EXPORTER_JIRA_PREFIX;
	
	@Qualifier("exporterJiraClient")	
	@Autowired
	private JiraClient jiraClient;	
	
    @Before
    public void init() {
    	
    	jiraClient.createJiraRestClient();    	
    }
    
    @After
    public void close() {
    	
    	jiraClient.closeJiraRestClient();    	
    }    
    
    //@Test
    public void createDummyJiraTest() {
    	
    	String projectName = EXPORTER_JIRA_PREFIX.substring(0, EXPORTER_JIRA_PREFIX.length() - 1);
    	Issue newIssue = translator.createDummyJira(projectName);
    	assertThat(newIssue).isNotNull();
    	assertThat(newIssue.getSummary().toLowerCase().contains("dummy")).isTrue();
    }
    
	/*
		Type ID = 71, Name = Change Request
		Type ID = 43, Name = Configuration Change
		Type ID = 57, Name = Service Request
		Type ID = 6, Name = Incident
		Type ID = 3, Name = Task	
		Type ID = 8, Name = Emergency Change
		Type ID = 10, Name = Query
		Type ID = 14, Name = Development
		Type ID = 59, Name = Support
		Type ID = 58, Name = Services
		Type ID = 47, Name = Documentation Request
		Type ID = 64, Name = Communication
		Type ID = 32, Name = Release
		Type ID = 12600, Name = Project Region Defect
		Type ID = 13, Name = Resource
		Type ID = 20, Name = Sick leave
		Type ID = 41, Name = Certification
		Type ID = 53, Name = Operational
		Type ID = 70, Name = Remediation
	 */
    
    @Test
    public void testIssueTypeMapping() {
    	
    	// Type ID = 6, Name = Incident
    	IssueType mappedType1 = translator.translateIssueType(6L, false);
    	assertThat(mappedType1.getName().equals("Change request")).isTrue();
    	
    	// Type ID = 6, Name = Incident    	
    	IssueType mappedType2 = translator.translateIssueType(6L, true);
    	assertThat(mappedType2.getName().equals("Configuration Request")).isTrue();

    	// Type ID = 6, Name = Incident    	
    	IssueType mappedType3 = translator.translateIssueType(6L, false);
    	assertThat(mappedType3.getName().equals("Change request")).isTrue();
    	assertThat(mappedType1.equals(mappedType3));
    	
    	// Type ID = 14, Name = Development
    	IssueType mappedType4 = translator.translateIssueType(14L, false);
    	assertThat(mappedType4.getName().equals("Change request")).isTrue();
    	
    	// Type ID = 1, Name = Bug
    	IssueType mappedType5 = translator.translateIssueType(1L, false);
    	assertThat(mappedType5.getName().equals("Test Defect")).isTrue();
    	
    	// Type ID = 6, Name = Incident    	
    	IssueType mappedType6 = translator.translateIssueType(6L, true);
    	assertThat(mappedType6.getName().equals("Configuration Request")).isTrue();    	
    }
    
	/*
	FIS:
	Id = 1, Name = Blocker
	Id = 2, Name = Critical
	Id = 3, Name = Major	
	Id = 4, Name = Minor
	Id = 5, Name = Trivial
	Id = 6, Name = Emergency
	Id = 7, Name = Urgent
	Id = 8, Name = Medium
	Id = 9, Name = Low
	
	OTR:
	Id = 1, Name = Highest
	Id = 2, Name = High
	Id = 3, Name = Medium
	Id = 4, Name = Low
	Id = 5, Name = Lowest	
	
	 */
    
    @Test
    public void translatePriorityTest() {
    	
    	// Id = 1, Name = Blocker
    	BasicPriority mappedPriority1 = translator.translatePriority(1);
    	assertThat(mappedPriority1.getId().equals(1L)).isTrue();
    	
    	// Id = 1, Name = Blocker
    	BasicPriority mappedPriority2 = translator.translatePriority(1);
    	assertThat(mappedPriority2.getId().equals(1L)).isTrue();
    	
    	// Id = 8, Name = Medium
    	BasicPriority mappedPriority3 = translator.translatePriority(8);
    	assertThat(mappedPriority3.getId().equals(3L)).isTrue();
    	
    	// Id = 9, Name = Low
    	BasicPriority mappedPriority4 = translator.translatePriority(9);
    	assertThat(mappedPriority4.getId().equals(4L)).isTrue();    	
    }
    
    @Test
    public void translateUserTest() throws URISyntaxException {
    	

    	Map<String, URI> avatarUris = new HashMap<String, URI>();
    	avatarUris.put("48x48", new URI("https://jira.fishosted.com/secure/useravatar?avatarId\u003d10118"));
    	   	
    	User me = new User(null, "lc44795", "Davydov, Mikhail", "Mikhail.Davydov@fisglobal.com", null,
    			avatarUris, null);

    	BasicUser otrMe = translator.translateUser(me);
    	assertThat(otrMe.getName().equals("davydov.mikhail"));
    	
    	BasicUser otrMe2 = translator.translateUser(me);
    	assertThat(otrMe2.getName().equals("davydov.mikhail"));    	
    }
}
