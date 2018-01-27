/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.controller;

import at.boisgard.thesis.custompipeline.classification.DataGenerator;
import at.boisgard.thesis.custompipeline.converter.UtteranceToFeatureVectorConverter;
import at.boisgard.thesis.custompipeline.model.AnnotatedWord;
import at.boisgard.thesis.custompipeline.model.ParseResponse;
import at.boisgard.thesis.custompipeline.model.ParseResponseNamedEntity;
import at.boisgard.thesis.custompipeline.model.Utterance;
import at.boisgard.thesis.custompipeline.model.annotation.NamedEntityAnnotation;
import at.boisgard.thesis.custompipeline.pipeline.CustomPipeline;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 *
 * @author BUERO
 */
@Controller
@RequestMapping("/parse")
public class ParseController {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(ParseController.class);
    
    @Autowired
    public CustomPipeline customPipeline;
    
    public SMO classifier;
    public Instances dataset;
    public int vectorSize;
    
    public ArrayList<String> intents;
    
    public ParseController(@Value("${weka.showcase.model.filename:'FAIL'}") String modelFilePath){
    
        try {
            
            classifier = (SMO)SerializationHelper.read(modelFilePath);
            LOGGER.info("Loaded classifier {}",modelFilePath);
        } catch (Exception ex) {
            
            LOGGER.error("Could not load model {}",modelFilePath,ex);
        }        
        
        // INIT WORD VECTOR SIZE
        vectorSize = 300;
        
        ArrayList<Attribute> attributes = new ArrayList<>();
        ArrayList<String> intents = new ArrayList<>();
        intents.add("standings");
        intents.add("squad");
        intents.add("schedule");
        intents.add("result");
        intents.add("player_info");
        intents.add("player_stats");
        
        this.intents = intents;
        
        // WE ARE ADDING 4 WV
        addWVAttributesToList("biggest_wv", attributes);
        addWVAttributesToList("avg_wv", attributes);
        addWVAttributesToList("dep_root_wv", attributes);
        addWVAttributesToList("avg_n3_wv", attributes);
        
        // ADDING CLASS ATTRIBUTE
        attributes.add(new Attribute("intent", intents));
        
        dataset = new Instances("FU", attributes, 0);
        dataset.setClassIndex(dataset.numAttributes()-1);
    }
    
    private void addWVAttributesToList(String prefix, ArrayList<Attribute> attributes){
        
        for(int i=0;i<vectorSize;i++){
            
            Attribute a = new Attribute(String.format("%s_%d",prefix,i));
            attributes.add(a);
        }
    }
    
    @RequestMapping(method=RequestMethod.GET)
    public @ResponseBody ParseResponse parseUtterance(@RequestParam(value="q",required=true,defaultValue="") String question){
        
        Utterance utterance = new Utterance();
        utterance.setRawText(question);
        
        utterance = customPipeline.executePipelineOnUtterance(utterance);
        
        return convertUtteranceToParseResponse(utterance);
    }
    
    private ParseResponse convertUtteranceToParseResponse(Utterance utterance){
        
        ParseResponse parseResponse = new ParseResponse(utterance.getRawText());
        
        ArrayList<ParseResponseNamedEntity> entities = new ArrayList<>();
        
        // GET ENTITIES
        for(AnnotatedWord aW:utterance.annotatedSentence.words){
            
            String nerLabel = ((NamedEntityAnnotation)aW.getAnnotations().get(NamedEntityAnnotation.class)).getEntityClass();
            
            if(!nerLabel.equals("O")){
                
                ParseResponseNamedEntity entity = new ParseResponseNamedEntity(aW.start, aW.end, aW.word, nerLabel);
                entities.add(entity);
                LOGGER.info("Check for NER labels: {} ({})",aW.word,nerLabel);
            }
                       
        }
        
        parseResponse.setEntities(entities);
        
        // CREATE FEATURE VECTOR (BIGGEST, AVG, DEPROOT, AVG N3
        UtteranceToFeatureVectorConverter featureVectorConverter = new UtteranceToFeatureVectorConverter(utterance);
                
        ArrayList<String> arffVector = new ArrayList<>();
        // BIGGEST
        DataGenerator.safelyAddVectorToInstanceData(featureVectorConverter.getSmallestOrBiggestVector(false), arffVector, vectorSize);
        // AVG
        DataGenerator.safelyAddVectorToInstanceData(featureVectorConverter.getUtteranceWordVectorAverage(), arffVector, vectorSize);
        // DEPROOT
        DataGenerator.safelyAddVectorToInstanceData(featureVectorConverter.getWordVectorOfDependencyTreeRoot(), arffVector, vectorSize);
        // AVG FIRST 3
        DataGenerator.safelyAddVectorToInstanceData(featureVectorConverter.getAverageWordVectorOfFirstNWords(3), arffVector, vectorSize);
        
        
        // CREATE INSTANCE
        DenseInstance instance = new DenseInstance(arffVector.size());
                  
        for(int i=0; i<arffVector.size();i++){
            
            instance.setValue(i, Double.parseDouble(arffVector.get(i)));
        }
        instance.setDataset(dataset);
        
        try {
            double label = classifier.classifyInstance(instance);
            
            String intentLabel = intents.get((int)label);
            LOGGER.info("Classified instance as {}",intentLabel);
            
            parseResponse.setIntent(intentLabel);
        } catch (Exception ex) {
            
            LOGGER.error("Could not classify instance :(",ex);
        }
        
        return parseResponse;
    }
    
}
