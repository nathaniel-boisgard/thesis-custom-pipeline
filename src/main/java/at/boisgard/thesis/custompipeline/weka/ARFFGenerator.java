/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.weka;

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
    
    public ARFFGenerator(String relation){
        
        this.relation = relation;
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
