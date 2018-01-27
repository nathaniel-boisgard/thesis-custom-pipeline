/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline;

import at.boisgard.thesis.custompipeline.pipeline.CustomPipeline;
import at.boisgard.thesis.custompipeline.pipeline.service.BagOfWordsVectorService;
import at.boisgard.thesis.custompipeline.pipeline.service.CoreNLPService;
import at.boisgard.thesis.custompipeline.pipeline.service.Word2VecService;
import java.io.IOException;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 *
 * @author BUERO
 */
@SpringBootApplication
public class WebServiceApplication {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(WebServiceApplication.class);
    
    @Autowired
    public CoreNLPService coreNLPService;
    
    @Autowired
    public Word2VecService word2VecService;
    
    @Autowired
    public BagOfWordsVectorService bowVectorSercvice;
    
    
    
    public static void main(String[] args){
        
        SpringApplication.run(WebServiceApplication.class, args);
    }
    
    @Bean
    public CustomPipeline getCustomPipeline() throws URISyntaxException, IOException{
        
        return new CustomPipeline(coreNLPService, word2VecService);
    }
}
