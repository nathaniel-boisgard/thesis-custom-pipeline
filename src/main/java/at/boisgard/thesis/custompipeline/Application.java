/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline;

import at.boisgard.thesis.custompipeline.classification.DataGenerator;
import at.boisgard.thesis.custompipeline.pipeline.service.CoreNLPService;
import at.boisgard.thesis.custompipeline.pipeline.service.Word2VecService;
import at.boisgard.thesis.custompipeline.pipeline.service.BagOfWordsVectorService;
import at.boisgard.thesis.custompipeline.provider.UtteranceProvider;
import at.boisgard.thesis.custompipeline.weka.ModelGenerator;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 *
 * @author BUERO
 */
@ComponentScan
@SpringBootApplication
public class Application {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    
    @Autowired
    public CoreNLPService coreNLPService;
    
    @Autowired
    public Word2VecService word2VecService;
    
    @Autowired
    public BagOfWordsVectorService bowVectorSercvice;
    
    @Autowired
    public UtteranceProvider utteranceProvider;
    
    @Autowired
    public DataGenerator dataGenerator;
    
    @Value("${only.model:0}")
    public int onlyModel;
        
    @PostConstruct
    public void runPipeline(){
        
        if(onlyModel==0){
            dataGenerator.generate();
        }            
        
        ModelGenerator modelGenerator = new ModelGenerator(dataGenerator.trainingOutputFileName, dataGenerator.testOutputFileName);
        modelGenerator.createAndEvaluateModels();
    }
    
   
    
    public static void main(String[] args){
        
        SpringApplication.run(Application.class, args).close();
    }
    
}
