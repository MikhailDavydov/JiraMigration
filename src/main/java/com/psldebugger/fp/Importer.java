package com.psldebugger.fp;

import java.io.File;
import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.psldebugger.fp.gateway.JiraClient;

@Component
@Lazy
public class Importer {

	@Value("${importer.jira.prefix}")
	private String IMPORTER_JIRA_PREFIX;
	
	@Autowired
	private File destinationDir;
	
	@Autowired
	private GsonBuilder gsonBuilder;
	
	final static Logger logger = LoggerFactory.getLogger(Importer.class);

	@Qualifier("importerJiraClient")	
	@Autowired	
	private JiraClient jiraClient;

	void importJiras(int fromKey, int toKey, boolean addDescription) {
		
		if (!destinationDir.exists())
			destinationDir.mkdirs();

		String jiraKey = null;
		
		try {
			
			jiraClient.createJiraRestClient();
			
			for (int jiraNumber = fromKey; jiraNumber <= toKey; jiraNumber++) {

				jiraKey = IMPORTER_JIRA_PREFIX + jiraNumber;		
				Issue jira = null;
				
				try {
					
					jira = jiraClient.getIssue(jiraKey);
				}
				catch (RestClientException ex) {
					
					logger.error(jiraKey != null ? jiraKey + ": " + ex.getMessage() : ex.getMessage());
					Optional<Integer> status = ex.getStatusCode();
					String msg = ex.getMessage();					
					if (status != null && status.get().equals(404) && msg != null && msg.contains("Issue Does Not Exist"))
						continue;
					else
						throw ex;
				}
				
				logger.info("Importing " + jira.getKey() + " \"" + jira.getSummary() + "\"");

				String summary = jira.getSummary();
				if (addDescription)
					summary = summary.trim().replaceAll("[/,\\,:,*,?,\",<,>,|]", "_");
							
				File jiraDir = new File(destinationDir.getPath() + File.separator + jiraKey + (addDescription ? " " + summary : ""));
				if (!jiraDir.exists())
					jiraDir.mkdirs();
				
				try (FileWriter fw = new FileWriter(jiraDir.getPath() + File.separator + "jira.json"); ) {
					
					Gson gson = gsonBuilder.setPrettyPrinting().create();
					gson.toJson(jira, fw);					
				}
				
				jiraClient.downloadAllAttchments(jira, jiraDir);			
			}			
		}
		catch (Exception ex) {
			
			logger.error(jiraKey != null ? jiraKey + ": " + ex.getMessage() : ex.getMessage());			
		}		
		finally {
			
			jiraClient.closeJiraRestClient();			
		}		
	}	
}
