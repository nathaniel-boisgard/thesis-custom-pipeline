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
    
    /**
     * Provides access to a custom word2vec webservice running at [host]:[port]
     * For details on the service see: 
     * https://github.com/nathaniel-boisgard/thesis-word2vec-wrapper
     * 
     * @param host
     * @param port 
     */
    public Word2VecService(
        @Value("${word2vec.host:'localhost'}") String host,
        @Value("${@word2vec.port:5000}") int port 
    ){
        
        LOGGER.info("Initiating Word2Vec service at {}:{}",host,port);
        
        this.host = host;
        this.port = port;        
    }
    
    /**
     * To properly connect with Sanford CoreNLP merge the annotations
     * 
     * @param document
     * @return
     * @throws URISyntaxException
     * @throws IOException 
     */
    public HashMap<CoreLabel,ArrayList<Double>> annotate(Annotation document) throws URISyntaxException, IOException{
        
        HashMap<CoreLabel,ArrayList<Double>> results = new HashMap<>();       
        
        for(CoreLabel token: document.get(CoreAnnotations.TokensAnnotation.class)){
            
            String word = token.get(CoreAnnotations.TextAnnotation.class);
            
            ArrayList<Double> wordVector = getWordVector(word);
            
            results.put(token, wordVector);
        }
        
        return results;
    }
    
    /**
     * Returns an ArrayList of Double values representing the 
     * word2vector vector of a given word
     * 
     * @param word
     * @return
     * @throws URISyntaxException
     * @throws IOException 
     */
    public ArrayList<Double> getWordVector(String word) throws URISyntaxException, IOException{
                
        HttpGet getRequest = new HttpGet(createRequestURI(word));
        
        HttpResponse httpResponse = null;
        int maxTries = 10;
        
        while(httpResponse == null && maxTries-- > 0){
            
            try {
                
                httpResponse = httpClient.execute(getRequest);
                
            } catch (Exception e) {
                LOGGER.error("Could not retrieve answer from Word2Vec remote service ({} tries left)",maxTries,e);
            }
        }
                
        String response = parseHTTPResponse(httpResponse);
        
        ArrayList<Double> responseVector = convertStringResponseToVector(response);
        
        return responseVector;
    }
    
    /**
     * Convert the String representation returned from the word2vec webservice
     * to a proper vector format
     * 
     * @param stringResponse
     * @return 
     */
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
    
    /**
     * Build a proper URI to request the webservice
     * 
     * @param word
     * @return
     * @throws URISyntaxException 
     */
    public URI createRequestURI(String word) throws URISyntaxException{
        
        URI uri = new URIBuilder().setScheme("http")
                .setHost(host)
                .setPort(port)
                .setPath("/")
                .setParameter("word", word)
                .build();
        
        LOGGER.debug("Created URI : {}",uri);
        
        return uri;
    }
    
    /**
     * Convert the response from the webservice to a usable String
     * 
     * @param httpResponse
     * @return
     * @throws IOException 
     */
    public String parseHTTPResponse(HttpResponse httpResponse) throws IOException{
        
        HttpEntity entity = httpResponse.getEntity();
        
        if(entity != null){
            
            String stringResponse = EntityUtils.toString(entity);
            
            return stringResponse;
            
        }else{
            
            throw new IOException("Got empty response from Word2Vec service!");
        }
    }
    
    public int getWord2VecVectorSize(){
        
        try {
            return getWordVector("test").size();
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Could not get W2V vector size", ex);
        }
        
        return 0;
    }
}
