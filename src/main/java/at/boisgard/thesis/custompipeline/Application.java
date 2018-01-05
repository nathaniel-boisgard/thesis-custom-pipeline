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
import at.boisgard.thesis.custompipeline.weka.ARFFGenerator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
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
            
            ArrayList<Utterance> utterances = utteranceProvider.getLimitedSetOfUtterances(1);
            CustomPipeline customPipeline = new CustomPipeline(coreNLPService, word2VecService);
            
            LOGGER.info("Loaded {} Utterance instances",utterances.size());
            
            // CREATE ARFF FILE
            // smallest wV, biggest wV, average wV, dependency tree root wV, 
            // average verb wV, average wV of first 3 words, number of B-TEAM NEs, 
            // number of B-PLAY NEs, number of B-COMP NEs, intent

            
            // VECTOR FEATURES
            String[] vectorFeatures = {"smallest_wv","biggest_wv","avg_wv","dep_root_wv","avg_vb_wv","avg_n3_wv"};

            ArrayList<String> attributes = new ArrayList<>();
            HashMap<String,String> attributeTypes = new HashMap<>();

            // WE KNOW wV HAVE 300 DIM
            for(String name: vectorFeatures){

                for(int i=1;i<301;i++){

                    String attributeName = String.format("%s_%d",name,i);
                    
                    attributes.add(attributeName);
                    attributeTypes.put(attributeName, "NUMERIC");
                }
            }
            
            // ADD NER FEATURE NAMES
            attributes.add("number_team_ne");
            attributeTypes.put("number_team_ne", "NUMERIC");
            attributes.add("number_play_ne");
            attributeTypes.put("number_play_ne", "NUMERIC");
            attributes.add("number_comp_ne");
            attributeTypes.put("number_comp_ne", "NUMERIC");
            
            // ADD INTENT ATTRIBUTE
            attributes.add("intent");
            
            StringBuilder intentAttributeBuilder = new StringBuilder();
            
            intentAttributeBuilder.append("{");
            for(String intent:UtteranceToFeatureVectorConverter.ALLOWED_INTENTS){
                
                intentAttributeBuilder.append(intent);
                if(Arrays.asList(UtteranceToFeatureVectorConverter.ALLOWED_INTENTS).indexOf(intent)<UtteranceToFeatureVectorConverter.ALLOWED_INTENTS.length-1){
                    intentAttributeBuilder.append(",");
                }
                        
            }
            intentAttributeBuilder.append("}");
            attributeTypes.put("intent", intentAttributeBuilder.toString());
            
            ARFFGenerator aRFFGenerator = new ARFFGenerator("thesis_en",attributes,attributeTypes,"C:\\Users\\BUERO\\Dropbox\\test.arff");
            
            LOGGER.info("Converting Utterances to feature vectors");
            for(Utterance u: utterances){
                
                // ANNOTATE UTTERANCE
                LOGGER.info("Annotating Utterance '{}'",u.rawText);
                u = customPipeline.executePipelineOnUtterance(u);
                
                LOGGER.info("Creating feature vector");
                // INIT FEATURE VECTOR CONVERTER
                UtteranceToFeatureVectorConverter fC = new UtteranceToFeatureVectorConverter(u);
                                
                // ADDING DATA TO ARFF GENERATOR
                ArrayList<String> instanceData = new ArrayList<>();
                
                // WORD VECTORS
                ArrayList<Double> smallestWV = fC.getSmallestOrBiggestVector(true);
                ArrayList<Double> biggestWV = fC.getSmallestOrBiggestVector(false);
                ArrayList<Double> avgWV = fC.getUtteranceWordVectorAverage();
                ArrayList<Double> depRootWV = fC.getWordVectorOfDependencyTreeRoot();
                ArrayList<Double> avgVerbWV = fC.getAverageWordVectorOfVerbs();
                ArrayList<Double> avgN3WV = fC.getAverageWordVectorOfFirstNWords(3);
                
                for(int i=0;i<300;i++){
                    
                    Double d = new Double(0);
                    
                    try{
                        
                        d = smallestWV.get(i);
                    }catch(IndexOutOfBoundsException e){
                        
                    }                    
                    instanceData.add(String.format(Locale.ROOT, "%.5f", d));
                }
                for(int i=0;i<300;i++){
                    
                    Double d = new Double(0);
                    
                    try{
                        
                        d = biggestWV.get(i);
                    }catch(IndexOutOfBoundsException e){
                        
                    }                    
                    instanceData.add(String.format(Locale.ROOT, "%.5f", d));
                }
                for(int i=0;i<300;i++){
                    
                    Double d = new Double(0);
                    
                    try{
                        
                        d = avgWV.get(i);
                    }catch(IndexOutOfBoundsException e){
                        
                    }                    
                    instanceData.add(String.format(Locale.ROOT, "%.5f", d));
                }
                for(int i=0;i<300;i++){
                    
                    Double d = new Double(0);
                    
                    try{
                        
                        d = depRootWV.get(i);
                    }catch(IndexOutOfBoundsException e){
                        
                    }                    
                    instanceData.add(String.format(Locale.ROOT, "%.5f", d));
                }
                for(int i=0;i<300;i++){
                    
                    Double d = new Double(0);
                    
                    try{
                        
                        d = avgVerbWV.get(i);
                    }catch(IndexOutOfBoundsException e){
                        
                    }                    
                    instanceData.add(String.format(Locale.ROOT, "%.5f", d));
                }
                for(int i=0;i<300;i++){
                    
                    Double d = new Double(0);
                    
                    try{
                        
                        d = avgN3WV.get(i);
                    }catch(IndexOutOfBoundsException e){
                        
                    }                    
                    instanceData.add(String.format(Locale.ROOT, "%.5f", d));
                }
                
                
                // NE OCCURENCES
                HashMap<String,Integer> neDistribution = fC.getNamedEntitiesDistribution();
                instanceData.add(String.format("%d", neDistribution.get("B-TEAM")!=null?neDistribution.get("B-TEAM"):0));
                instanceData.add(String.format("%d", neDistribution.get("B-PLAY")!=null?neDistribution.get("B-PLAY"):0));
                instanceData.add(String.format("%d", neDistribution.get("B-COMP")!=null?neDistribution.get("B-COMP"):0));
                
                // INTENT
                instanceData.add(u.intent);
        
                LOGGER.info("Adding feature vector to ARFF file");
                aRFFGenerator.streamInstanceToFile(instanceData);
                
                LOGGER.debug("Text: '{}', Intent: '{}'",u.getRawText(),u.getIntent());
                LOGGER.debug("Smallest word vector (d={}): {} ",fC.getSmallestOrBiggestVector(true).size(),fC.getSmallestOrBiggestVector(true));
                LOGGER.debug("Biggest word vector (d={}): {}",fC.getSmallestOrBiggestVector(false).size(),fC.getSmallestOrBiggestVector(false));
                LOGGER.debug("Average word vector (d={}): {}",fC.getUtteranceWordVectorAverage().size(),fC.getUtteranceWordVectorAverage());                
                LOGGER.debug("Dependency tree root word vector (d={}): {}",fC.getWordVectorOfDependencyTreeRoot().size(),fC.getWordVectorOfDependencyTreeRoot());
                LOGGER.debug("Average verb word vector (d={}): {}",fC.getAverageWordVectorOfVerbs().size(),fC.getAverageWordVectorOfVerbs());
                LOGGER.debug("Average word vector of first 3 words of sentence (d={}): {}",fC.getAverageWordVectorOfFirstNWords(3).size(),fC.getAverageWordVectorOfFirstNWords(3));
                LOGGER.debug("NER distribution: {}",fC.getNamedEntitiesDistribution());
            }
            
            LOGGER.info("Close ARFF file");
            aRFFGenerator.closeStreamingWriter();
                
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Unable to execute pipeline", ex);
        }
        
    }
    
    public static void main(String[] args){
        
        SpringApplication.run(Application.class, args).close();
    }
    
}
