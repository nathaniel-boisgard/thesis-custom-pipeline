/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.classification;

import at.boisgard.thesis.custompipeline.converter.UtteranceToFeatureVectorConverter;
import at.boisgard.thesis.custompipeline.model.Utterance;
import at.boisgard.thesis.custompipeline.pipeline.CustomPipeline;
import at.boisgard.thesis.custompipeline.pipeline.service.BagOfWordsVectorService;
import at.boisgard.thesis.custompipeline.pipeline.service.CoreNLPService;
import at.boisgard.thesis.custompipeline.pipeline.service.Word2VecService;
import at.boisgard.thesis.custompipeline.provider.UtteranceProvider;
import at.boisgard.thesis.custompipeline.weka.ARFFGenerator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author BUERO
 */
public class DataGenerator {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(DataGenerator.class);
    
    public static final String[] WORD2VECTOR_FEATURES = {"smallest_wv","biggest_wv","avg_wv","dep_root_wv","avg_vb_wv","avg_n3_wv"};
    
    public String modelName;
    
    public ArrayList<Utterance> trainingUtterances;
    public ArrayList<Utterance> testUtterances;
    
    public CustomPipeline customPipeline;
    public ARFFGenerator arffGenerator;
    
    public ArrayList<String> attributes = new ArrayList<>();
    public HashMap<String,String> attributeTypes = new HashMap<>();
    
    public int w2vVectorSize;
    public int bowVectorSize;
      
    public UtteranceProvider utteranceProvider;
      
    public CoreNLPService coreNLPService;
    
    public BagOfWordsVectorService bagOfWordsVectorService;
    
    public Word2VecService word2VecService;
    
    public String outputFilePath;
    
    public String language; 
    
    public String w2vModelName;
    
    public DataGenerator(
            UtteranceProvider utteranceProvider,
            CoreNLPService coreNLPService,
            BagOfWordsVectorService bagOfWordsVectorService,
            Word2VecService word2VecService,
            @Value("${arff.output.file.path:'/tmp'}") String outputFilePath,
            @Value("${language:'en'}") String language,
            @Value("${w2v.model.name:'unknown-w2v'}") String w2vModelName
    ) throws IOException, URISyntaxException{
        
        this.utteranceProvider = utteranceProvider;
        this.coreNLPService = coreNLPService;
        this.bagOfWordsVectorService = bagOfWordsVectorService;
        this.word2VecService = word2VecService;
        
        this.outputFilePath = outputFilePath;
        this.language = language;
        this.w2vModelName = w2vModelName;
        
        trainingUtterances = utteranceProvider.getTrainingUtterances();
        testUtterances = utteranceProvider.getTestUtterances();
        LOGGER.info("Annotating {} training and {} test utterances.",trainingUtterances.size(),testUtterances.size());
        
        w2vVectorSize = word2VecService.getWord2VecVectorSize();
        bowVectorSize = bagOfWordsVectorService.getBoWVectorSize();
        LOGGER.info("Using Word2Vec vectors with d={}, TF-IDF vectors with d={}",w2vVectorSize,bowVectorSize);        
        
        customPipeline = new CustomPipeline(coreNLPService, word2VecService);
        LOGGER.info("Initialized CustomPipeline");
  
        initDataHead();
        LOGGER.info("Initialized data head, using {} feature attributes",attributes.size());
        
        modelName = String.format("thesis_%s_w2v_%s_dim_%d_bowvdim_%d", language,w2vModelName,w2vVectorSize,bowVectorSize);
        arffGenerator = new ARFFGenerator(modelName);
        arffGenerator.attributes = attributes;
        arffGenerator.attributeTypes = attributeTypes;
        LOGGER.info("Initialized ARFF Generarator to create files for model '{}'",modelName); 
    }
    
    public void generate(){
        
        generatedDataFileFromUtterances(trainingUtterances, "training");
        LOGGER.info("Generated {} training instances",trainingUtterances.size());
        generatedDataFileFromUtterances(testUtterances, "test");
        LOGGER.info("Generated {} test instances",testUtterances.size());
    }
    
    public void generatedDataFileFromUtterances(ArrayList<Utterance> utterances,String environment){
        
        String fileName = generateOutputFileName(environment);
        LOGGER.info("Preparing ARFF data to save to '{}'",fileName);
        
        ArrayList<ArrayList<String>> arffData = new ArrayList<>();
        
        utterances.forEach((u) -> {
            arffData.add(convertUtteranceToInstance(u));
        });
        
        arffGenerator.data = arffData;
        arffGenerator.saveFile(fileName);
    }
    
    public String generateOutputFileName(String env){
        
        return String.format("%s/%s_%s.arff",outputFilePath,modelName,env);
    }
    
    public ArrayList<String> convertUtteranceToInstance(Utterance u){
        
        // ANNOTATE UTTERANCE
        LOGGER.info("Annotating Utterance '{}'",u.rawText);
        u = customPipeline.executePipelineOnUtterance(u);
        
        LOGGER.info("Convert Utterance to feature vector");
        // INIT FEATURE VECTOR CONVERTER
        UtteranceToFeatureVectorConverter fC = new UtteranceToFeatureVectorConverter(u);

        // ADDING DATA TO ARFF GENERATOR
        ArrayList<String> instanceData = new ArrayList<>();

        // WORD VECTORS                
        safelyAddVectorToInstanceData(fC.getSmallestOrBiggestVector(true), instanceData, w2vVectorSize);
        safelyAddVectorToInstanceData(fC.getSmallestOrBiggestVector(false), instanceData, w2vVectorSize);
        safelyAddVectorToInstanceData(fC.getUtteranceWordVectorAverage(), instanceData, w2vVectorSize);
        safelyAddVectorToInstanceData(fC.getWordVectorOfDependencyTreeRoot(), instanceData, w2vVectorSize);
        safelyAddVectorToInstanceData(fC.getAverageWordVectorOfVerbs(), instanceData, w2vVectorSize);
        safelyAddVectorToInstanceData(fC.getAverageWordVectorOfFirstNWords(3), instanceData, w2vVectorSize);

        // BOW VECTOR
        safelyAddVectorToInstanceData(bagOfWordsVectorService.getTfIDFVector(u), instanceData, bowVectorSize);

        // NE OCCURENCES
        HashMap<String,Integer> neDistribution = fC.getNamedEntitiesDistribution();
        
        int compNEs = 0;
        int teamNEs = 0;
        int playNEs = 0;
        
        if(neDistribution.get("B-COMP")!=null){
            
            compNEs += neDistribution.get("B-COMP");
        }
        if(neDistribution.get("I-COMP")!=null){
            
            compNEs += neDistribution.get("I-COMP");
        }
        
        if(neDistribution.get("B-TEAM")!=null){
            
            teamNEs += neDistribution.get("B-TEAM");
        }
        if(neDistribution.get("I-TEAM")!=null){
            
            teamNEs += neDistribution.get("I-TEAM");
        }
        
        if(neDistribution.get("B-PLAY")!=null){
            
            playNEs += neDistribution.get("B-PLAY");
        }
        if(neDistribution.get("I-PLAY")!=null){
            
            playNEs += neDistribution.get("I-PLAY");
        }
        
        instanceData.add(String.format("%d", teamNEs));
        instanceData.add(String.format("%d", playNEs));
        instanceData.add(String.format("%d", compNEs));

        // INTENT
        instanceData.add(u.intent);        
        
        return instanceData;
    }
    
    private void initDataHead(){
        
        // ADD W2V VECTORS
        for(String name: WORD2VECTOR_FEATURES){

            for(int i=0;i<w2vVectorSize;i++){

                String attributeName = String.format("%s_%d",name,i);

                attributes.add(attributeName);
                attributeTypes.put(attributeName, "NUMERIC");
            }
        }

        // ADD BOWVECTOR
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
    }
    
    public static void safelyAddVectorToInstanceData(ArrayList<Double> vector, ArrayList<String> instanceData, int dimensions){
        
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
}
