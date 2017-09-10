package com.psldebugger.fp.gateway.dto;

import java.util.List;

import org.joda.time.DateTime;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.User;

public class IssueDTO {

	private String summary;
	private DateTime dueDate;
	private BasicPriority priority;
	private User assignee;			   
	private User reporter;
	private String description;
	List<Comment> comments;
	private Status status;
	private Resolution resolution;
	private IssueType issueType;
	private List<String> labels;
	private List<IssueLink> issueLinks;
	
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}	
	public DateTime getDueDate() {
		return dueDate;
	}
	public void setDueDate(DateTime dueDate) {
		this.dueDate = dueDate;
	}
	public BasicPriority getPriority() {
		return priority;
	}
	public void setPriority(BasicPriority priority) {
		this.priority = priority;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public IssueType getIssueType() {
		return issueType;
	}
	public void setIssueType(IssueType issueType) {
		this.issueType = issueType;
	}
	public User getAssignee() {
		return assignee;
	}
	public void setAssignee(User assignee) {
		this.assignee = assignee;
	}
	public User getReporter() {
		return reporter;
	}
	public void setReporter(User reporter) {
		this.reporter = reporter;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public Resolution getResolution() {
		return resolution;
	}
	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}
	public List<Comment> getComments() {
		
		return comments;
	}
	public List<String> getLabels() {
		return labels;
	}
	public void setLabels(List<String> labels) {
		this.labels = labels;
	}
	public List<IssueLink> getIssueLinks() {
		return issueLinks;
	}
	public void setIssueLinks(List<IssueLink> issueLinks) {
		this.issueLinks = issueLinks;
	}
}
