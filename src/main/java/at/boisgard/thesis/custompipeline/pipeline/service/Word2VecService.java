/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.pipeline.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Word2VecService {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(Word2VecService.class);
    
    public final String host;
    public final int port;
    
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    
    public Word2VecService(
        @Value("${word2vec.host:'localhost'}") String host,
        @Value("${@word2vec.port:5000}") int port 
    ){
        
        LOGGER.info("Initiating Word2Vec service at {}:{}",host,port);
        
        this.host = host;
        this.port = port;        
    }
    
    public HashMap<CoreLabel,ArrayList<Double>> annotate(Annotation document) throws URISyntaxException, IOException{
        
        HashMap<CoreLabel,ArrayList<Double>> results = new HashMap<>();       
        
        for(CoreLabel token: document.get(CoreAnnotations.TokensAnnotation.class)){
            
            String word = token.get(CoreAnnotations.TextAnnotation.class);
            
            ArrayList<Double> wordVector = getWordVector(word);
            
            results.put(token, wordVector);
        }
        
        return results;
    }
    
    public ArrayList<Double> getWordVector(String word) throws URISyntaxException, IOException{
                
        HttpGet getRequest = new HttpGet(createRequestURI(word));
        
        String response = parseHTTPResponse(httpClient.execute(getRequest));
        
        ArrayList<Double> responseVector = convertStringResponseToVector(response);
        
        return responseVector;
    }
    
    public ArrayList<Double> convertStringResponseToVector(String stringResponse){
        
        stringResponse = stringResponse.replaceAll("(\\[\\s*)","").replaceAll("(\\s*\\])", "").replaceAll("(\\s{2,})", " ");
            
        ArrayList<Double> result = new ArrayList<>();
        
        if(stringResponse.equals("")){
            
            return result;
        }
        
        String[] stringResponseParts = stringResponse.split(" ");
        
        for(String s: stringResponseParts){
            
            result.add(Double.parseDouble(s));
        }
        
        return result;
    }
    
    public URI createRequestURI(String word) throws URISyntaxException{
        
        URI uri = new URIBuilder().setScheme("http")
                .setHost(host)
                .setPort(port)
                .setPath("/")
                .setParameter("word", word)
                .build();
        
        LOGGER.info("Created URI : {}",uri);
        
        return uri;
    }
    
    public String parseHTTPResponse(HttpResponse httpResponse) throws IOException{
        
        HttpEntity entity = httpResponse.getEntity();
        
        if(entity != null){
            
            String stringResponse = EntityUtils.toString(entity);
            
            return stringResponse;
            
        }else{
            
            throw new IOException("Got empty response from Word2Vec service!");
        }
    }
}
