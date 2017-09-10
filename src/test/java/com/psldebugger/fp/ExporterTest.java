package com.psldebugger.fp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExporterTest {

	
    @Autowired
    private Exporter exporter;

    
    @Test
    public void contexLoads() throws Exception {
        assertThat(exporter).isNotNull();
    }
	
}
