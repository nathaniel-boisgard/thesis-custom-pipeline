/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.pipeline;

import at.boisgard.thesis.custompipeline.pipeline.service.CoreNLPService;
import at.boisgard.thesis.custompipeline.pipeline.service.Word2VecService;
import java.io.IOException;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author BUERO
 */
public class CustomPipeline {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(CustomPipeline.class);
    
    public CoreNLPService coreNLPService;
    public Word2VecService word2VecService;
    
    public CustomPipeline(CoreNLPService coreNLPService, Word2VecService word2VecService) throws URISyntaxException, IOException{
        
        this.coreNLPService = coreNLPService;
        this.word2VecService = word2VecService;
        
        LOGGER.info("Starting pipeline with {}, {}",coreNLPService,word2VecService);     
    }
    
    public void testPipeline(String input) throws URISyntaxException, IOException{
        
        LOGGER.info("Parsing sentence {}, results: {}",input,word2VecService.annotate(coreNLPService.annotate(input)));
    }
}
