/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.converter;

import at.boisgard.thesis.custompipeline.model.AnnotatedSentence;
import at.boisgard.thesis.custompipeline.model.AnnotatedWord;
import at.boisgard.thesis.custompipeline.model.Utterance;
import at.boisgard.thesis.custompipeline.model.annotation.DependencyTreeAnnotation;
import at.boisgard.thesis.custompipeline.model.annotation.NamedEntityAnnotation;
import at.boisgard.thesis.custompipeline.model.annotation.PartOfSpeechAnnotation;
import at.boisgard.thesis.custompipeline.model.annotation.WordVectorAnnotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert an annotated sentence to a ARFF feature vector usable by Weka or similar
 * machine learning tools
 * 
 * @author BUERO
 */
public class UtteranceToFeatureVectorConverter {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(UtteranceToFeatureVectorConverter.class);
    
    public static final String[] ALLOWED_INTENTS = {"standings","squad","schedule","result","player_info","player_stats"};
    public static final String[] VERB_POS_TAGS = {"VB","VBD","VBG","VBN","VBP","VBZ"};
    
    public AnnotatedSentence annotatedSentence;
    public String intent;
    
    public UtteranceToFeatureVectorConverter(Utterance utterance){
        
        this.annotatedSentence = utterance.annotatedSentence;
        this.intent = utterance.intent;
    }
    
    public ArrayList<Double> getWordVectorOfAnnotatedWord(AnnotatedWord word){
        
        return ((WordVectorAnnotation)word.getAnnotations().get(WordVectorAnnotation.class)).vector;
    }
    
    public String getNamedEntityRecognitionLabelOfAnnotatedWord(AnnotatedWord word){
        
        return ((NamedEntityAnnotation)word.getAnnotations().get(NamedEntityAnnotation.class)).getEntityClass();
    }
    
    public String getPartOfSpeechTagOfAnnotatedWord(AnnotatedWord word){
        
        return ((PartOfSpeechAnnotation)word.getAnnotations().get(PartOfSpeechAnnotation.class)).getPosTag();
    }
    
    public ArrayList<Double> getSmallestOrBiggestVector(boolean searchForSmallest){
        
        ArrayList<Double> target = new ArrayList<>();
        
        for(AnnotatedWord w:annotatedSentence.words){
            
            ArrayList<Double> wV = getWordVectorOfAnnotatedWord(w);
            
            if(wV != null && target.size() < 1 && wV.size() > 0){
                
                target = wV;
            }else if(wV != null && wV.size() > 0){
                
                if(searchForSmallest){
                    
                    if(getVectorMagnitude(wV)<getVectorMagnitude(target)){
                    
                        target = wV;
                    }
                }else{
                    
                    if(getVectorMagnitude(wV)>getVectorMagnitude(target)){
                    
                        target = wV;
                    }
                }
                
            }
        }
        
        return target;
    } 
    
    public ArrayList<Double> getUtteranceWordVectorAverage(){
        
        ArrayList<ArrayList<Double>> vectors = new ArrayList<>();
        
        for(AnnotatedWord w:annotatedSentence.words){
            
            ArrayList<Double> wV = getWordVectorOfAnnotatedWord(w);
            
            if(wV != null && wV.size()>0){
                
                vectors.add(wV);
            }
        }
        
        return calculateVectorAverage(vectors);
    }
    
    public ArrayList<Double> calculateVectorAverage(ArrayList<ArrayList<Double>> vectors){
        
        ArrayList<Double> target = new ArrayList<>();

        int numberOfWordVectors = 0;
        
        for(ArrayList<Double> wV:vectors){
                        
            if(wV != null && target.size() < 1 && wV.size() > 0){
                
                target = wV;
                numberOfWordVectors++;
            }else if(wV != null && wV.size() > 0){
                
                for(int i = 0;i<wV.size();i++){
                    
                    Double d = target.get(i);
                    d+=wV.get(i);
                    
                    numberOfWordVectors++;
                }
            }
        }
        
        if(numberOfWordVectors == 0){
            
            return target;
        }
        
        for(Double d: target){
            
            d = d/numberOfWordVectors;
        }
        
        return target;
    }
    
    public Double getVectorMagnitude(ArrayList<Double> vector){
        
        Double sum = new Double(0);
        
        sum = vector.stream().map((d) -> d*d).reduce(sum, (accumulator, _item) -> accumulator + _item);
        
        return Math.sqrt(sum);
    }

    public int getNumericalRepresentationOfIntent(){
        
        return Arrays.asList(ALLOWED_INTENTS).indexOf(intent);
    }
    
    public HashMap<String,Integer> getNamedEntitiesDistribution(){
        
        HashMap distribution = new HashMap();
        
        for(AnnotatedWord word: annotatedSentence.words){
            
            String nerLabel = getNamedEntityRecognitionLabelOfAnnotatedWord(word);
            
            if(distribution.get(nerLabel) == null){
                
                distribution.put(nerLabel, 1);
            }else{
                
                distribution.put(nerLabel, (Integer)distribution.get(nerLabel)+1);
            }
        }      
        
        return distribution;
    } 
    
    public ArrayList<Double> getWordVectorOfDependencyTreeRoot(){
        
        for(AnnotatedWord word: annotatedSentence.words){
            
            if(((DependencyTreeAnnotation)word.getAnnotations().get(DependencyTreeAnnotation.class)).isRoot){
                
                return getWordVectorOfAnnotatedWord(word);
            }
        }
        
        return null;
    }
    
    public ArrayList<Double> getAverageWordVectorOfVerbs(){
        
        ArrayList<ArrayList<Double>> vectors = new ArrayList<>();
        
        for(AnnotatedWord w:annotatedSentence.words){
                 
            // IF VERB, ADD VECTOR
            if(Arrays.asList(VERB_POS_TAGS).contains(getPartOfSpeechTagOfAnnotatedWord(w))){
                
                LOGGER.debug("Found verb '{}' ({})",w.word,getPartOfSpeechTagOfAnnotatedWord(w));
                
                ArrayList<Double> wV = getWordVectorOfAnnotatedWord(w);
              
                if(wV != null && wV.size()>0){

                        vectors.add(wV);    
                }else{
                    
                    LOGGER.debug("Word vector of verb '{}' is empty!",w.word);
                }    
            }
                
        }
        
        return calculateVectorAverage(vectors);
    }
    
    public ArrayList<Double> getAverageWordVectorOfFirstNWords(int n){
        
        ArrayList<ArrayList<Double>> vectors = new ArrayList<>();
        
        for(int i=0;(i<n && i<annotatedSentence.words.size());i++){
            
            AnnotatedWord w = annotatedSentence.words.get(i);
            ArrayList<Double> wV = getWordVectorOfAnnotatedWord(w);
              
            if(wV != null && wV.size()>0){

                    vectors.add(wV);    
            }
        }
        
        return calculateVectorAverage(vectors);
    }
}
