/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import at.boisgard.thesis.custompipeline.model.Utterance;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author BUERO
 */
@Component
public class UtteranceProvider {
    
    public String trainingFilePath;
    public String testFilePath;
    
    public ObjectMapper oM = new ObjectMapper();
    
    public UtteranceProvider(
            @Value("${utterance.training.file.path:'FAIL'}") String trainingFilePath,
            @Value("${utterance.training.file.path:'FAIL'}") String testFilePath){
        
        this.trainingFilePath = trainingFilePath;
        this.testFilePath = testFilePath;
    }
    
    public ArrayList<Utterance> getTrainingUtterances() throws IOException{
        
        return getUtterances(trainingFilePath);
    }
    
    public ArrayList<Utterance> getTestUtterances() throws IOException{
        
        return getUtterances(testFilePath);
    }
    
    public ArrayList<Utterance> getLimitedSetOfTrainingUtterances(double ratio) throws IOException{
        
        ArrayList<Utterance> allData = getTrainingUtterances();
        
        ArrayList<Utterance> limitedData = new ArrayList<>();
        
        for(Utterance u:allData){
            
            if(Math.random() <= ratio){
                limitedData.add(u);
            }
        }
        
        return limitedData;        
    }
    
    public ArrayList<Utterance> getUtterances(String filePath) throws IOException{
        
        ArrayList<Utterance> data = oM.readValue(
                new InputStreamReader(
                    new FileInputStream(filePath),
                    StandardCharsets.UTF_8
                ), 
                oM.getTypeFactory().constructCollectionType(ArrayList.class,Utterance.class)
        );
        
        return data;
    }
}
