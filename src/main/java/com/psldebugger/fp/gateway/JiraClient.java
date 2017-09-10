package com.psldebugger.fp.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Priority;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.LinkIssuesInput;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

public class JiraClient {

	private JiraRestClient restClient;
	
	public void createJiraRestClient() {
	
		final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		URI jiraServerUri = URI.create(url);
		restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, user, password);		
	}
	
	public void closeJiraRestClient() {
		
		try {
			restClient.close();
		} catch (IOException e) {

			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}

	public String url, user, password;

	@Autowired
	public JiraClient(String url, String user, String password) {
		
		this.url = url;
		this.user = user;
		this.password = password;
	}
	
	public Issue getIssue(String issueKey) {
		
		try {
			
			final Issue issue = restClient.getIssueClient().getIssue(issueKey).claim();
			return issue;
		}
		finally {
			
		}		
	}
	
	public boolean downloadAttchment(String jiraKey, Pattern attachmentPattern, String target) {

		try {
			
			final Issue issue = restClient.getIssueClient().getIssue(jiraKey).claim();
			
			for (Attachment attachment: issue.getAttachments()) {
				
				Matcher matcher = attachmentPattern.matcher(attachment.getFilename());
				if (matcher.matches()) {
					
					File targetFile = new File(target);
					if (!targetFile.exists()) {
						
						InputStream inStream = restClient.getIssueClient().getAttachment(attachment.getContentUri()).claim();					
						targetFile.getParentFile().mkdirs();
						try {
							Files.copy(inStream, targetFile.toPath());
							return true;
						} catch (IOException e) {
							
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
					else
						return true;
				}
			}
		}			
		finally {
			
			try {
				restClient.close();
			} catch (IOException e) {

				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
		return false;
	}
	
	public boolean downloadAllAttchments(final Issue issue, File destinationDir) {

		for (Attachment attachment: issue.getAttachments()) {
			
			String fileName = attachment.getFilename();
			fileName = fileName.trim().replaceAll("[/,\\,:,*,?,\",<,>,|]", "_");
			File targetFile = new File(destinationDir + File.separator + fileName);
			if (targetFile.exists())
				targetFile.delete();
			if (!targetFile.exists()) {
				
				InputStream inStream = restClient.getIssueClient().getAttachment(attachment.getContentUri()).claim();
				
				if (!targetFile.getParentFile().exists())
					targetFile.getParentFile().mkdirs();
				
				try {
					Files.copy(inStream, targetFile.toPath());

				} catch (IOException e) {
					
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}
		
		return false;
	}	
	
	public boolean addComments(String jiraKey, String comment) {

		try {
			
			// Copied from here:
			// http://www.bernd-adamowicz.de/489/basic-jira-rest-operations/
			
			final Issue issue = restClient.getIssueClient().getIssue(jiraKey).claim();
			URI issueURI = new URI(issue.getSelf().toString() + "/comment/");
			Comment jiraComment = Comment.valueOf(comment);			
			Promise<Void> promise = restClient.getIssueClient().addComment(issueURI, jiraComment);
	        promise.claim();

		}
		catch (URISyntaxException ex) {
		
			throw new RuntimeException(ex);
		}
		
		return false;
	}
	
	public void displayIssueTypes() {
		
		Promise<Iterable<IssueType>> promise = restClient.getMetadataClient().getIssueTypes();
		Iterable<IssueType> issueTypes = promise.claim();
		
		for (IssueType it : issueTypes) {
			System.out.println("Type ID = " + it.getId() + ", Name = " + it.getName());
		}		
	}

	public Iterable<IssueType> getIssueTypes() {
	
		Promise<Iterable<IssueType>> promise = restClient.getMetadataClient().getIssueTypes();
		return promise.claim();		
	}
	
	public void displayPriorities() {
		
		Promise<Iterable<Priority>> promise = restClient.getMetadataClient().getPriorities();
		Iterable<Priority> priorities = promise.claim();

		for (Priority priority: priorities) {
			System.out.println("Id = " + priority.getId() + ", Name = " + priority.getName());
		}		
	}

	public Iterable<Priority> getPriorities() {

		Promise<Iterable<Priority>> promise = restClient.getMetadataClient().getPriorities();
		return promise.claim();			
	}
	
	public void displayStatuses() {

		Promise<Iterable<Status>> promise = restClient.getMetadataClient().getStatuses();
		Iterable<Status> statuses = promise.claim();

		for (Status status: statuses) {
			System.out.println("Id = " + status.getId() + ", Name = " + status.getName());
		}	
	}
	
	public void displayUser(String name) {
		
        // Invoke the JRJC Client
        Promise<User> userPromise = restClient.getUserClient().getUser(name);
        User user = userPromise.claim();        
        System.out.println(user);
	}
	
	public User getUser(String name) {
	
		try {
	        Promise<User> userPromise = restClient.getUserClient().getUser(name);
	        return userPromise.claim();	        
		}
		catch(Exception ex) {

		}
		
		return null;
	}
	
	public Issue createIssue(IssueInput input) throws IOException {
	
		final IssueRestClient issueClient = restClient.getIssueClient();
		final BasicIssue newBasicIssue = issueClient.createIssue(input).claim();
		final Issue newIssue = issueClient.getIssue(newBasicIssue.getKey()).claim();
		return newIssue;
	}
	
	public void addComments(Issue issue, Iterable<Comment> comments) {
		
		final IssueRestClient issueClient = restClient.getIssueClient();
		final Issue newIssue = issueClient.getIssue(issue.getKey()).claim();
		for (Comment comment: comments) {
			
			String body = (comment.getAuthor().getDisplayName() + " added a comment - " + comment.getUpdateDate().toString("yyyy-MM-dd HH:mm") + 
					"\r\n\r\n" + comment.getBody());
			if (body != null && body.length() > 32766)
				body = body.substring(0, 32766); 
			issueClient.addComment(newIssue.getCommentsUri(), Comment.valueOf(body)).claim();
		}		
	}
	
	public void addComment(Issue issue, String body) {

		final IssueRestClient issueClient = restClient.getIssueClient();		
		issueClient.addComment(issue.getCommentsUri(), Comment.valueOf(body)).claim();
	}
	
	public void attachFile(Issue issue, File fileToAttach) throws FileNotFoundException {
	
		final IssueRestClient issueRestClient = restClient.getIssueClient(); 
		InputStream is = new FileInputStream(fileToAttach);
		issueRestClient.addAttachment(issue.getAttachmentsUri(), is, fileToAttach.getName()).claim();			
	}
	
	public void displayAllowedTransitions(Issue issue) {
	 
	    Promise<Iterable<Transition>> promise = restClient.getIssueClient().getTransitions(issue);
	    Iterator<Transition> iterTransitions = promise.claim().iterator();
	    
		while (iterTransitions.hasNext()) {
	        
	        Transition tr = iterTransitions.next();
	        System.err.println(tr.toString());      
	    }
	}
	
	public void transitionJira(Issue issue, TransitionInput transitionInput) {

		restClient.getIssueClient().transition(issue, transitionInput);
	}
	
	public void linkIssues(LinkIssuesInput linkIssuesInput) {
		
		final IssueRestClient issueClient = restClient.getIssueClient();
		issueClient.linkIssue(linkIssuesInput).claim();
	}
}
