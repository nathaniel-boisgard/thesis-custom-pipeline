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
import at.boisgard.thesis.custompipeline.pipeline.service.BagOfWordsVectorService;
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
import org.springframework.beans.factory.annotation.Value;

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
    public BagOfWordsVectorService bowVectorSercvice;
    
    @Autowired
    public UtteranceProvider utteranceProvider;
    
    @Value("${arff.output.file.path:'aux.output.arff'}")
    public String outputFilePath;
    
    @Value("${app.env:1}")
    public int appEnvironment; 
    
    @PostConstruct
    public void runPipeline(){
       
        try {
            
            LOGGER.info("Preparing ARFF file {}",outputFilePath);            
            ArrayList<Utterance> utterances = new ArrayList<>();
            
            if(appEnvironment == ApplicationConfiguration.APPLICATION_TRAINING_ENV){
                
                utterances = utteranceProvider.getTrainingUtterances();
            }else{
                
                utterances = utteranceProvider.getTestUtterances();
            }
                
            CustomPipeline customPipeline = new CustomPipeline(coreNLPService, word2VecService);
            
            LOGGER.info("Loaded {} Utterance instances",utterances.size());
            
            /** 
             * TRY CREATING ARFF FILE
             * 
             * smallest wV, biggest wV, average wV, dependency tree root wV, 
             * average verb wV, average wV of first 3 words, tf-idf weighed BoW vector, 
             * number of B-TEAM NEs, number of B-PLAY NEs, number of B-COMP NEs, 
             * intent
             *  
             */
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
            
            // ADD BOWVECTOR
            // GET CURRENT SIZE
            Utterance testUtt = new Utterance();
            testUtt.setRawText("test");
            int bowVectorSize = bowVectorSercvice.getTfIDFVector(testUtt).size();
            LOGGER.info("Bag of words vector size is {}",bowVectorSize);
            for(int i=0;i<bowVectorSize;i++){
                
                String attributeName = String.format("bowv_%d",i);
                
                attributes.add(attributeName);
                attributeTypes.put(attributeName, "NUMERIC");
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
            
            ARFFGenerator aRFFGenerator = new ARFFGenerator("thesis_en");
            aRFFGenerator.attributes = attributes;
            aRFFGenerator.attributeTypes = attributeTypes;
            
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
                safelyAddVectorToInstanceData(fC.getSmallestOrBiggestVector(true), instanceData, 300);
                safelyAddVectorToInstanceData(fC.getSmallestOrBiggestVector(false), instanceData, 300);
                safelyAddVectorToInstanceData(fC.getUtteranceWordVectorAverage(), instanceData, 300);
                safelyAddVectorToInstanceData(fC.getWordVectorOfDependencyTreeRoot(), instanceData, 300);
                safelyAddVectorToInstanceData(fC.getAverageWordVectorOfVerbs(), instanceData, 300);
                safelyAddVectorToInstanceData(fC.getAverageWordVectorOfFirstNWords(3), instanceData, 300);
     
                // BOW VECTOR
                safelyAddVectorToInstanceData(bowVectorSercvice.getTfIDFVector(u), instanceData, bowVectorSize);
                
                // NE OCCURENCES
                HashMap<String,Integer> neDistribution = fC.getNamedEntitiesDistribution();
                instanceData.add(String.format("%d", neDistribution.get("B-TEAM")!=null?neDistribution.get("B-TEAM"):0));
                instanceData.add(String.format("%d", neDistribution.get("B-PLAY")!=null?neDistribution.get("B-PLAY"):0));
                instanceData.add(String.format("%d", neDistribution.get("B-COMP")!=null?neDistribution.get("B-COMP"):0));
                
                // INTENT
                instanceData.add(u.intent);
        
                LOGGER.info("Adding feature vector to ARFF file");
                aRFFGenerator.data.add(instanceData);
            }
            
            LOGGER.info("Saving Weka ARFF file");
            aRFFGenerator.saveFile(outputFilePath);
                
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Unable to execute pipeline", ex);
        }
        
    }
    
    public void safelyAddVectorToInstanceData(ArrayList<Double> vector, ArrayList<String> instanceData, int dimensions){
        
        for(int i=0;i<dimensions;i++){
              
            // DEFAULT IS 0
            Double d = new Double(0);

            // IF VECTOR DIMENSION IS SET, USE IT
            try{

                d = vector.get(i);
            }catch(IndexOutOfBoundsException e){

            }                    
            instanceData.add(String.format(Locale.ROOT, "%.5f", d));
        }
    }
    
    public static void main(String[] args){
        
        SpringApplication.run(Application.class, args).close();
    }
    
}
