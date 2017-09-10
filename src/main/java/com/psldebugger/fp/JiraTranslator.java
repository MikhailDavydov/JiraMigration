package com.psldebugger.fp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Priority;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.LinkIssuesInput;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.psldebugger.fp.gateway.JiraClient;
import com.psldebugger.fp.gateway.dto.IssueDTO;

/*
 * TODO:
 * 1. Create dummy jira when the directory to import is not found.
 * 2. Spring cache.
 * 
 * 
 */

@EnableCaching
@Component
@Lazy
public class JiraTranslator {
	
	@Autowired
	@Qualifier("exporterJiraClient")	
	private JiraClient jiraClient;
	
	public JiraTranslator() {
		
		initIssueTypeIdMap();
		initPriorityIdMap();
	}
	
	Map<Long, Long> issueTypeIdMap = new HashMap<Long, Long>();
	void initIssueTypeIdMap() {
		
		// Type ID = 1, Name = Bug
		// Type ID = 10104, Name = Test Defect
		issueTypeIdMap.put(1L, 10104L);
						
		// Type ID = 2, Name = New Feature
		// Type ID = 10100, Name = Change request
		issueTypeIdMap.put(2L, 10100L);
		
		// Type ID = 3, Name = Task
		// Type ID = 10103, Name = Service Request
		issueTypeIdMap.put(3L, 10103L);
		
		// Type ID = 6, Name = Incident
		// Type ID = 10102, Name = Production Incident
		issueTypeIdMap.put(6L, 10102L);
		
		// Type ID = 7, Name = Request for Information
		// Type ID = 10103, Name = Service Request
		issueTypeIdMap.put(7L, 10103L);
				
		// Type ID = 14, Name = Development		
		// Type ID = 10100, Name = Change request		
		issueTypeIdMap.put(14L, 10100L);
	}

	private Iterable<IssueType> issueTypes;
	
	@Cacheable("issueTypes")
	public IssueType translateIssueType(long issueTypeId, boolean configuration) {
	
		// Type ID = 10100, Name = Change request
		long mappedIssueTypeId = 10100L;
		
		// Type ID = 6, Name = Incident
		if (issueTypeId == 6L)
			// Type ID = 10101, Name = Configuration Request
			// Type ID = 10100, Name = Change request
			mappedIssueTypeId = (configuration ? 10101L : 10100L);
		else {

			if (issueTypeIdMap.containsKey(issueTypeId))
				mappedIssueTypeId = issueTypeIdMap.get(issueTypeId); 			
		}
		
		if (issueTypes == null)
			issueTypes = jiraClient.getIssueTypes();
		
		for (IssueType it : issueTypes) {
			
			if (it.getId().equals(mappedIssueTypeId))
				return it;
		}
		
		return null;		
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
	
	Map<Long, Long> priorityIdMap = new HashMap<Long, Long>();
	void initPriorityIdMap() {
		
		priorityIdMap.put(1L, 1L);
		priorityIdMap.put(2L, 1L);
		priorityIdMap.put(3L, 2L);
		priorityIdMap.put(4L, 4L);
		priorityIdMap.put(5L, 5L);
		priorityIdMap.put(6L, 1L);
		priorityIdMap.put(7L, 1L);						
		priorityIdMap.put(8L, 3L);
		priorityIdMap.put(9L, 4L);		
	}
	
	Map<BasicPriority, BasicPriority> priorityCache = new HashMap<BasicPriority, BasicPriority>();
	
	@Cacheable("priorities")
	public BasicPriority translatePriority(long priorityId) {
		
		// Default OTR priority is { Id = 3, Name = Medium }
		long mappedPriorityId = 3L;
		if (priorityIdMap.containsKey(priorityId))
			mappedPriorityId = priorityIdMap.get(priorityId); 
			
		Iterable<Priority> priorities = jiraClient.getPriorities();
		for (Priority it : priorities) {
			
			if (it.getId().equals(mappedPriorityId))
				return it;				
		}
		
		return null;	
	}

	public long translateStatus(Status status) {
		
		if (status.getId() <= 6)
			return status.getId();

		// Id = 10102, Name = On Hold
		return 10102;
	}
	
	@Cacheable("users")	
	public BasicUser translateUser(BasicUser fisUser) {

		String fullName = fisUser.getDisplayName();
		String[] names = fullName.split(", ");
		boolean reverse = true;
		if (names.length < 2)
			return null;
		
		String firstName = names[(reverse ? 1:0)].toLowerCase();
		String lastName = names[(reverse ? 0:1)].toLowerCase();
		
		User user = jiraClient.getUser(lastName + "." + firstName);
        if (user != null)
        	return user;
        	
		return null;
	}
	
	Issue createDummyJira(String projectKey) {
		
		try {
			// Type ID = 10100, Name = Change request
			final IssueInputBuilder builder = new IssueInputBuilder(projectKey, 10100L, "Dummy jira to keep numbering");
			final IssueInput input = builder.build();			
			return jiraClient.createIssue(input);
		} catch (IOException ex) {

			ex.printStackTrace();
			throw new RuntimeException(ex.getMessage());
		}
	}	
	
	// Transition{id=11, name=Cancel Request, fields=[Field{id=resolution, isRequired=true, type=resolution}, Field{id=labels, isRequired=false, type=array}]}
	final TransitionInput cancelRequest = new TransitionInput(11);
	
	// Transition{id=21, name=On Hold, fields=[Field{id=attachment, isRequired=false, type=array}, Field{id=labels, isRequired=false, type=array}]}
	final TransitionInput onholdRequest = new TransitionInput(21);
	
	// Transition{id=201, name=Start Progress, fields=[Field{id=summary, isRequired=true, type=string}, Field{id=issuetype, isRequired=true, type=issuetype}, Field{id=priority, isRequired=false, type=priority}, Field{id=reporter, isRequired=true, type=user}, Field{id=customfield_10100, isRequired=false, type=user}, Field{id=customfield_10102, isRequired=false, type=date}, Field{id=customfield_10103, isRequired=false, type=date}, Field{id=description, isRequired=false, type=string}, Field{id=attachment, isRequired=false, type=array}, Field{id=customfield_10112, isRequired=false, type=number}, Field{id=customfield_10113, isRequired=false, type=number}, Field{id=customfield_10106, isRequired=false, type=date}, Field{id=customfield_10105, isRequired=false, type=date}, Field{id=customfield_10104, isRequired=false, type=date}, Field{id=customfield_10110, isRequired=false, type=date}, Field{id=customfield_10111, isRequired=false, type=string}, Field{id=components, isRequired=false, type=array}, Field{id=customfield_10101, isRequired=false, type=string}, Field{id=customfield_10005, isRequired=false, type=array}, Field{id=customfield_10001, isRequired=false, type=any}, Field{id=labels, isRequired=false, type=array}, Field{id=assignee, isRequired=false, type=user}]}
	final TransitionInput startProgress = new TransitionInput(201);
	
	@Value("${importer.jira.prefix}")
	private String IMPORTER_JIRA_PREFIX;

	@Value("${exporter.jira.prefix}")
	private String EXPORTER_JIRA_PREFIX;
	
	public Issue cloneIssue(String projectKey, IssueDTO source) throws IOException {
		
		final IssueType translatedIssueType = translateIssueType(source.getIssueType().getId(), source.getSummary().startsWith("Configuration"));
		final IssueInputBuilder builder = new IssueInputBuilder(projectKey, translatedIssueType.getId(), source.getSummary());
		final BasicPriority translatedPriority = translatePriority(source.getPriority().getId());		
		
		builder.setPriority(translatedPriority);
		builder.setDescription(source.getDescription());

		if (source.getLabels() != null && source.getLabels().size() > 0) {
			builder.setFieldInput(new FieldInput(IssueFieldId.LABELS_FIELD, source.getLabels()));
		}
		
		long sourceStatus = source.getStatus().getId();
		// In Progress
		if (sourceStatus == 3L) {

			User fisAssignee = source.getAssignee();
			BasicUser otrAssignee = translateUser(fisAssignee);
			if (otrAssignee != null)
				builder.setAssignee(otrAssignee);				
		}		
			
		final IssueInput input = builder.build();
		Issue newIssue = jiraClient.createIssue(input);
		
		// 5L - Resolved
		// 6L - Closed
		// 10006L - Rejected
		// 10020L - Reviewed
		if (sourceStatus == 6L || sourceStatus == 5L || sourceStatus == 10006L || sourceStatus == 10020L)
			jiraClient.transitionJira(newIssue, cancelRequest);
		else
			// In Progress
			if (sourceStatus == 3L)
				jiraClient.transitionJira(newIssue, startProgress);
			else
				// On Hold
				if (sourceStatus == 10004L)
					jiraClient.transitionJira(newIssue, onholdRequest);				
        
		jiraClient.addComments(newIssue, source.getComments());
		
		// VTB-1920 has issueLinks		
		if (source.getIssueLinks() != null && source.getIssueLinks().size() > 0) {
			
			for (IssueLink issueLink: source.getIssueLinks()) {

				String targetKey = issueLink.getTargetIssueKey();
				if (targetKey.startsWith(IMPORTER_JIRA_PREFIX)) {
					
					int currentNumber = Integer.valueOf(newIssue.getKey().substring(EXPORTER_JIRA_PREFIX.length()));
					int relatedNumber = Integer.valueOf(targetKey.substring(IMPORTER_JIRA_PREFIX.length()));
					if (relatedNumber < currentNumber) {
					
						String issueLinkType = "Relates";	//issueLink.getIssueLinkType().getDescription();
						LinkIssuesInput linkIssuesInput = new LinkIssuesInput(newIssue.getKey(), EXPORTER_JIRA_PREFIX + relatedNumber, issueLinkType); 
						jiraClient.linkIssues(linkIssuesInput);
						continue;
					}
				}
				else
					jiraClient.addComment(newIssue, issueLink.getIssueLinkType().getDescription() + " " + issueLink.getTargetIssueKey());
			}
		}		
		
		return newIssue;		
	}
}
