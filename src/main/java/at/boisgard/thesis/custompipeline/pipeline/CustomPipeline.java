/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.pipeline;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author BUERO
 */
@Component
public class CustomPipeline {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(CustomPipeline.class);
    
    public CustomPipeline(){
        
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse");
        props.setProperty("ner.model", "C:\\Users\\BUERO\\java\\coreNLP\\ner-model.sport.en.ser.gz");
        
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        
        String input = "When is the next match between Chelsea FC and Manchester United?";
        
        Annotation document = new Annotation(input);
        
        pipeline.annotate(document);
        
        
        
        for(CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class)){
            
            for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)){
                
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                
                LOGGER.info("{}: {},{},{}",word,lemma,pos,ner);
                
            }
        }
    }
}
