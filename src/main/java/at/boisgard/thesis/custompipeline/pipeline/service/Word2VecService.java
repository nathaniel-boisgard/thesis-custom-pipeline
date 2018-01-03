/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Word2VecService {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(Word2VecService.class);
    
    public Word2VecService(
        @Value("${word2vec.host:'localhost'}") String host,
        @Value("${@word2vec.port:5000}") int port 
    ){
        
        LOGGER.info("Initiating Word2Vec service at {}:{}",host,port);
        
    }
}
