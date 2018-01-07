/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.weka;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author BUERO
 */
public class ARFFGenerator {
    
    public String relation;
    public ArrayList<String> attributes;
    public HashMap<String,String> attributeTypes;
    public ArrayList<ArrayList<String>> data = new ArrayList<>(); 
    
    private PrintWriter streamingWriter;
    
    public ARFFGenerator(String relation){
        
        this.relation = relation;
    }
    
    public ARFFGenerator(String relation, ArrayList<String> attributes, HashMap<String,String> attributeTypes, String filePath){
        
        this.relation = relation;
        this.attributes = attributes;
        this.attributeTypes = attributeTypes;
        
        try(FileWriter fW = new FileWriter(filePath)){
            
            BufferedWriter bW = new BufferedWriter(fW);
            this.streamingWriter = new PrintWriter(bW);
        }catch(IOException e){
            
            System.err.println(e.toString());
        }
        
        this.streamingWriter.print(renderHeader());
    }
    
    public void closeStreamingWriter(){
        
        if(this.streamingWriter != null){
            
            this.streamingWriter.close();
        }
    }
    
    public String renderHeader(){
        
        StringBuilder sB = new StringBuilder();
        
        // HEADER
        sB.append(String.format("@RELATION\t%s\n", relation));
        sB.append("\n");
        
        for(String attributeName:attributes){
            
            sB.append(String.format("@ATTRIBUTE\t%s\t%s\n",attributeName,attributeTypes.get(attributeName)));
        }
        
        sB.append("\n").append("@DATA\n");

        return sB.toString();
    }
    
    public void streamInstanceToFile(ArrayList<String> instance){
        
        StringBuilder sB = new StringBuilder();
        
        for(String value: instance){
                
            sB.append(value);

            if(instance.indexOf(value)<(instance.size()-1)){
                sB.append(",");
            }
        }
            
        sB.append("\n");
        
        streamingWriter.print(sB.toString());
    }
    
    public String renderBody(){
        
        StringBuilder sB = new StringBuilder();
        
        for(ArrayList<String> instance: data){
            
            for(String value: instance){
                
                sB.append(value);
                
                if(instance.indexOf(value)<(instance.size()-1)){
                    sB.append(",");
                }
            }
            
            sB.append("\n");
        }
        
        return sB.toString();
    }
    
    public void saveFile(String filePath){
        
        try (PrintWriter pW = new PrintWriter(filePath)) {
            pW.print(renderHeader());
            pW.print(renderBody());
        }catch(IOException e){
            
        }
    }
}
