/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.pipeline.service;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author BUERO
 */
@Component
public class CoreNLPService {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(CoreNLPService.class);
    
    public StanfordCoreNLP pipeline;
    
    public CoreNLPService(@Value("${corenlp.properties:'coreNLP-english.properties'}") String propertiesFilename) throws IOException{
        
        LOGGER.debug("Initiating coreNLP service with properties loaded from {}",propertiesFilename);
        
        Properties props = new Properties();        
        props.load(IOUtils.readerFromString(propertiesFilename));
        
        pipeline = new StanfordCoreNLP(props);
    }
    
    public Annotation annotate(String input){
        
        LOGGER.debug("Trying to annotate '{}'",input);
        
        Annotation document = new Annotation(input);
        pipeline.annotate(document);
        
        return document;
    }
}
