/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.pipeline.service;

import at.boisgard.thesis.custompipeline.model.Utterance;
import at.boisgard.thesis.custompipeline.provider.UtteranceProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author BUERO
 */
@Component
public class BagOfWordsVectorService {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(BagOfWordsVectorService.class);
    
    public String trainingFilePath;
    public String testFilePath;
    
    public LinkedHashMap<String,Double> idf;
    
    public BagOfWordsVectorService(
            @Value("${utterance.training.file.path:'FAIL}") String trainingFilePath,
            @Value("${utterance.test.file.path:'FAIL}") String testFilePath
            ){
        
        LOGGER.info("Inititated with files {} and {}",trainingFilePath,testFilePath);
        
        this.testFilePath = testFilePath;
        this.trainingFilePath = trainingFilePath;
        this.idf = new LinkedHashMap<>();
        
        try {
            initVocabulary();
        } catch (IOException ex) {
            LOGGER.error("Could not initialize vocabulary!", ex);
        }
    }
    
    public void initVocabulary() throws IOException{
        
        UtteranceProvider uP = new UtteranceProvider(trainingFilePath,testFilePath);
        
        ArrayList<Utterance> trainUtterances = uP.getTrainingUtterances();
        ArrayList<Utterance> testUtterances = uP.getTestUtterances();
        
        LOGGER.info("Loaded {} resp. {} utterances",trainUtterances.size(),testUtterances.size());
        
        trainUtterances.addAll(testUtterances);
        
        LOGGER.info("Using total of {} utterances to build IDF vector",trainUtterances.size());
        
        int numberOfDocuments = trainUtterances.size();        
        
        for(Utterance u: trainUtterances){
            
            ArrayList<String> temporaryList = new ArrayList<>();
            
            
            for(String word: cleandedAndSplitSentence(u.rawText)){
                
                if(temporaryList.indexOf(word) == -1){
                    
                    temporaryList.add(word);
                }
            }
            
            // SET OCCURENCES IN IDF LIST
            for(String tW: temporaryList){
                
                if(idf.get(tW) == null){
                    
                    idf.put(tW, new Double(1));
                }else{
                    
                    idf.put(tW, idf.get(tW)+1);
                }
            }
        }
        
        // CALCULATE IDF VALUES
        for(Map.Entry<String,Double> idfEntry:idf.entrySet()){
            
            // CALCULATE INVERSE
            Double inverse = Math.log((new Double(numberOfDocuments))/idfEntry.getValue());
            
            idf.put(idfEntry.getKey(), inverse);
        }
    }
    
    public ArrayList<Double> getTfIDFVector(Utterance u){
        
        // BUILD VECTOR
        ArrayList<Double> tfidfVector = new ArrayList<>();
        
        // GET WORDS FROM UTTERANCE
        LinkedHashMap<String,Integer> wordList = new LinkedHashMap<>();
        
        for(String word: cleandedAndSplitSentence(u.rawText)){
                        
            if(wordList.get(word) == null){
                
                wordList.put(word, 1);
            }else{
                
                wordList.put(word, wordList.get(word)+1);
            }
        }
        
        // CREATE VECTOR VALUES: ITERATE OVER ALL IDF WORDS AND CALCULATE VECTOR VALUE (0 IF WORD NOT IN UTTERANCE, TF*IDF IF WORD IN UTTERANCE)
        for(Map.Entry<String,Double> idfEntry:idf.entrySet()){
            
            // CALCULATE VALUE FOR WORD
            Double wordVal = new Double(0);
            
            if(wordList.get(idfEntry.getKey()) != null){
                
                wordVal = (new Double(wordList.get(idfEntry.getKey())))*idfEntry.getValue();
            }
            
            tfidfVector.add(wordVal);
        }
        
        return tfidfVector;
    }
    
    
    public String[] cleandedAndSplitSentence(String sentence){
        
        sentence = sentence.replace("?", " ?").replace(".", " .");
        
        return sentence.split(" ");
    }    
    
    public int getBoWVectorSize(){
        
        Utterance testUtt = new Utterance();
        testUtt.setRawText("test");
        
        return getTfIDFVector(testUtt).size();
    }
}
