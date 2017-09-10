package com.psldebugger.fp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.psldebugger.fp.gateway.JiraClient;

@SpringBootApplication
@PropertySource("classpath:/settings.properties")
public class App implements ApplicationRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(App.class);	

	@Autowired
	@Lazy	
	Importer importer;

	@Autowired
	@Lazy
	Exporter exporter;
	
	static class DateTimeTypeConverter implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
		  // No need for an InstanceCreator since DateTime provides a no-args constructor
		@Override	
		public JsonElement serialize(DateTime src, Type srcType, JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}
		@Override
		public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
			return new DateTime(json.getAsString());
		}
	}
	
	@Bean
	GsonBuilder getGsonBuilder() {
		
		return new GsonBuilder().registerTypeAdapter(DateTime.class, new DateTimeTypeConverter());
	}
	
	@Bean
	File getDestionationDir(@Value("${destinationDir}") String destinationDir) {
		
		return new File(destinationDir);
	}
	
	@Qualifier("importerJiraClient")
	@Lazy
	@Bean
	JiraClient getImportJiraClient(
			@Value("${importer.jira.url}") String url,
			@Value("${importer.jira.user}") String user,
			@Value("${importer.jira.password}") String password) {
	
		return new JiraClient(url, user, password);		
	}
	
	@Qualifier("exporterJiraClient")
	@Lazy
	@Bean
	public JiraClient getExportJiraClient(
			@Value("${exporter.jira.url}") String url,
			@Value("${exporter.jira.user}") String user,
			@Value("${exporter.jira.password}") String password) {
		
		return new JiraClient(url, user, password);		
	}
	
	private void showUsage() {
	
		logger.error("Program usage:");
		logger.error("JavaMigration.jar import fromKey toKey");
		logger.error("JavaMigration.jar export fromKey toKey");
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
        
        /*		
        logger.info("NonOptionArgs: {}", args.getNonOptionArgs());
        logger.info("OptionNames: {}", args.getOptionNames());
        */       
        
		logger.info("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));
        
        if (args.getSourceArgs().length < 3) {
        	showUsage();
        	return;
        }
    	
    	final String command = args.getNonOptionArgs().get(0);
    	final Integer fromKey = Integer.valueOf(args.getNonOptionArgs().get(1));
    	final Integer toKey = Integer.valueOf(args.getNonOptionArgs().get(2));   	
    	
        if (command == null || fromKey == null || toKey == null) {
        	
        	showUsage();
        	return;
        }
        
        if (command != null && command.equals("import")) {
        
			importer.importJiras(fromKey, toKey, false);
        }
        else
            if (command != null && command.equals("export")) {
                
    			exporter.exportJiras(fromKey, toKey);
            }
	}
	
    public static void main(String[] args) {
  	
        SpringApplication.run(App.class, args);
    }
	

}
