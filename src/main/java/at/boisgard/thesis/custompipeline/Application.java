/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline;

import at.boisgard.thesis.custompipeline.converter.UtteranceToFeatureVectorConverter;
import at.boisgard.thesis.custompipeline.pipeline.CustomPipeline;
import at.boisgard.thesis.custompipeline.pipeline.service.CoreNLPService;
import at.boisgard.thesis.custompipeline.pipeline.service.Word2VecService;
import at.boisgard.thesis.custompipeline.provider.UtteranceProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import at.boisgard.thesis.custompipeline.model.Utterance;
import java.util.logging.Level;

/**
 *
 * @author BUERO
 */
@ComponentScan
@SpringBootApplication
public class Application {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    
    @Autowired
    public CoreNLPService coreNLPService;
    
    @Autowired
    public Word2VecService word2VecService;
    
    @Autowired
    public UtteranceProvider utteranceProvider;
    
    @PostConstruct
    public void runPipeline(){
       
        try {
            
            ArrayList<Utterance> utterances = utteranceProvider.getLimitedSetOfUtterances(0.002);
            CustomPipeline customPipeline = new CustomPipeline(coreNLPService, word2VecService);
            
            LOGGER.info("Loaded {} Utterance instances",utterances.size());
            
            utterances = customPipeline.executePipelineOnUtteranceList(utterances);
            
            for(Utterance u: utterances){
                
                UtteranceToFeatureVectorConverter fC = new UtteranceToFeatureVectorConverter(u);
                LOGGER.info("Text: '{}', Intent: '{}'",u.getRawText(),u.getIntent());
                LOGGER.info("Smallest word vector (d={}): {} ",fC.getSmallestOrBiggestVector(true).size(),fC.getSmallestOrBiggestVector(true));
                LOGGER.info("Biggest word vector (d={}): {}",fC.getSmallestOrBiggestVector(false).size(),fC.getSmallestOrBiggestVector(false));
                LOGGER.info("Average word vector (d={}): {}",fC.getUtteranceWordVectorAverage().size(),fC.getUtteranceWordVectorAverage());                
                LOGGER.info("Dependency tree root word vector (d={}): {}",fC.getWordVectorOfDependencyTreeRoot().size(),fC.getWordVectorOfDependencyTreeRoot());
                LOGGER.info("Average verb word vector (d={}): {}",fC.getAverageWordVectorOfVerbs().size(),fC.getAverageWordVectorOfVerbs());
                LOGGER.info("Average word vector of first 3 words of sentence (d={}): {}",fC.getAverageWordVectorOfFirstNWords(3).size(),fC.getAverageWordVectorOfFirstNWords(3));
                LOGGER.info("NER distribution: {}",fC.getNamedEntitiesDistribution());
                
            }
                
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Unable to execute pipeline", ex);
        }
        
    }
    
    public static void main(String[] args){
        
        SpringApplication.run(Application.class, args).close();
    }
    
}
