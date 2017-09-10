package com.psldebugger.fp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.psldebugger.fp.gateway.JiraClient;
import com.psldebugger.fp.gateway.dto.IssueDTO;

@Component
@Lazy
public class Exporter {

	@Value("${importer.jira.prefix}")
	private String IMPORTER_JIRA_PREFIX;

	@Value("${exporter.jira.prefix}")
	private String EXPORTER_JIRA_PREFIX;
	
	@Autowired
    GsonBuilder gsonBuilder;

	@Autowired	
	File destinationDir;
    
	final static Logger logger = LoggerFactory.getLogger(Exporter.class);
	
	@Qualifier("exporterJiraClient")	
	@Autowired
	private JiraClient jiraClient;
	
	/*
	 * This is used when the directories have "PREFIX-NNNN Description" structure.
	 */
	IssueDTO findJira(String issueKey) {
		
		Pattern pattern = Pattern.compile("^VTB[-]\\d+");		
		
		for (String dirName: destinationDir.list()) {
			
			Matcher matcher = pattern.matcher(dirName);
			if (matcher.find() && matcher.group().equals(issueKey)) {
				
				try (FileReader fr = new FileReader(destinationDir.getPath() + File.separator + dirName + File.separator + "jira.json"); ) {
					
					Gson gson = gsonBuilder.create();
					IssueDTO newIssue = gson.fromJson(fr, IssueDTO.class);
					return newIssue;
					
				} catch (IOException ex) {

					ex.printStackTrace();
					logger.error("Failed load " + issueKey + ": " + ex.getMessage());
				}				
			}
		}
		
		return null;
	}
	
	IssueDTO loadJira(Gson gson, File jiraFile) {
		
		try (FileReader fr = new FileReader(jiraFile); ) {
						
			IssueDTO newIssue = gson.fromJson(fr, IssueDTO.class);
			return newIssue;
			
		} catch (IOException ex) {

			ex.printStackTrace();
			logger.error("Failed to load " + jiraFile.getPath() + ": " + ex.getMessage());
		}				
		
		return null;
	}
	
	private void attachFiles(Issue issue, File directory) throws FileNotFoundException {
		
        String files[] = directory.list();
		for (String fileName: files) {
			
			if (fileName.equals("jira.json"))
				continue;
			
			File fileToUpload = new File(directory.getPath() + File.separator + fileName);
			
			if (fileToUpload.length() > 10485760) {
				
				logger.error(issue.getKey() + ": Can't attach " + fileName + " because its size exceeds 10485760 bytes. File size = " + fileToUpload.length());
				continue;
			}
			
			logger.info(issue.getKey() + ": attaching " + fileName);
			jiraClient.attachFile(issue, fileToUpload);
		}			
	}	
	
	@Autowired
	@Lazy
	JiraTranslator jiraTranslator;
	
	void exportJiras(int fromKey, int toKey) {
	
		try {
			
			jiraClient.createJiraRestClient();

			final String prevJiraKey = EXPORTER_JIRA_PREFIX + (fromKey - 1);
			if (fromKey > 1) {
				
				try {
					jiraClient.getIssue(prevJiraKey);					
				}
				catch(Exception ex) {
					
					// Issue is not found
					throw new RuntimeException("Previous jira " + prevJiraKey + " does not exist: " + ex.getMessage());
				}
			}

			final String startJiraKey = EXPORTER_JIRA_PREFIX + fromKey;
			boolean alreadyExists = false;
			try {
				jiraClient.getIssue(startJiraKey);
				alreadyExists = true;				
			}
			catch(Exception ex) {
				
				// This is ok - new issue with this number will be created
			}

			if (alreadyExists)
				throw new RuntimeException("Jira " + startJiraKey + " already exists");

			// Remove dash
			String projectName = (EXPORTER_JIRA_PREFIX.endsWith("-") ? 
					EXPORTER_JIRA_PREFIX.substring(0, EXPORTER_JIRA_PREFIX.length() - 1) : EXPORTER_JIRA_PREFIX);
			
			Gson gson = gsonBuilder.create();
			
			for (int jiraNumber = fromKey; jiraNumber <= toKey; jiraNumber++) {
				
				String jiraKey = IMPORTER_JIRA_PREFIX + jiraNumber;
				logger.info("Cloning jira " + jiraKey);
				
				File jiraDir = new File(destinationDir.getPath() + File.separator + jiraKey);
				File jiraFile = new File(destinationDir.getPath() + File.separator + jiraKey + File.separator + "jira.json");
				if (!jiraFile.exists()) {
					
					// This is to keep the numbering
					jiraTranslator.createDummyJira(projectName);
					continue;
				}
				
				IssueDTO source = loadJira(gson, jiraFile);
				if (source == null)
					throw new RuntimeException("Failed to load " + jiraKey);

				Issue otrIsssue = jiraTranslator.cloneIssue(projectName, source);
				logger.info("New jira " + otrIsssue.getKey() + " is successfully created");
				if (!otrIsssue.getKey().equals(EXPORTER_JIRA_PREFIX + jiraNumber))
					throw new RuntimeException("Incorrect jira number");
				attachFiles(otrIsssue, jiraDir);
				logger.info(otrIsssue.getKey() + " files are attached");
			}
		}
		catch(Exception ex) {
			
			logger.error(ex.getMessage());
		}
		finally {
			
			jiraClient.closeJiraRestClient();			
		}		
	}	
}
