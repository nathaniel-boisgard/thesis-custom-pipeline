/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.weka;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import org.slf4j.LoggerFactory;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Instances;

/**
 *
 * @author BUERO
 */
public class ModelGenerator {
    
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ModelGenerator.class);
    
    public String trainingFileName;
    public String testFileName;
    public Instances trainingInstances;
    public Instances testInstances;
    
    public SMO model;
    
    public Evaluation evaluation;
    public String evaluationFileName;
    
    PrintWriter pW;
    
    public ModelGenerator(String trainingFileName, String testFileName, String evaluationFileName){
        
        this.trainingFileName = trainingFileName;
        this.testFileName = testFileName;
        
        this.evaluationFileName = evaluationFileName;
        
        LOGGER.info("Training and evaluating models on {} and {}",trainingFileName,testFileName);
        
        try {
            BufferedReader bR = new BufferedReader(new FileReader(trainingFileName));
            trainingInstances = new Instances(bR);
            bR.close();
            
            trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);
            
            BufferedReader testBR = new BufferedReader(new FileReader(testFileName));
            testInstances = new Instances(testBR);
            testBR.close();
            
            testInstances.setClassIndex(testInstances.numAttributes()-1);
            
            LOGGER.info("Loaded training and test instances for model creation and evaluation");
            
            PrintWriter pW = new PrintWriter(evaluationFileName);
        } catch (IOException ex) {
            LOGGER.error("Could not load ARFF file", ex);
        }
    }
    
    public void createAndEvaluateModels(){
        
        try {
            
            // NEED TO MANUALLY INIT ALGORITHM AND KERNELS
            PolyKernel polyKernel = new PolyKernel(trainingInstances, 250007,1.0,Boolean.FALSE);
            RBFKernel rbfKernel = new RBFKernel(trainingInstances, 250007, 0.01);
            
            model = new SMO();
            model.setBatchSize("100");
            model.setChecksTurnedOff(true);
            model.setEpsilon(1.0E-12);
            
            double[] cValues = {0.01,0.1,1,10,100};
            
            for(double d:cValues){
            
                model.setC(d);
                // POLY AND THEN RBF
                
                
                LOGGER.info("Building model with C={} and Kernel=PolyKernel",d);
                model.setKernel(polyKernel);
                model.buildClassifier(trainingInstances);
                
                evaluateModel();
                
                LOGGER.info("Building model with C={} and Kernel=RBFKernel",d);
                model.setKernel(rbfKernel);
                model.buildClassifier(trainingInstances);
                
                evaluateModel();
            }
             
        } catch (Exception e) {
            LOGGER.error("Error!",e);
        }
        
        closeWriter();
            
    }
  
    public void evaluateModel() throws Exception{
        
        evaluation = new Evaluation(testInstances);
        evaluation.evaluateModel(model, testInstances); 

        LOGGER.info(evaluation.toSummaryString("\nResults\n======\n", false));
        LOGGER.info(evaluation.toMatrixString("\nConfusion Matrix\n======\n"));
        
        saveEvaluationToFile();
    }
    
    public void saveEvaluationToFile() throws Exception{
         
        pW.println(String.format("\nMODEL\n======\nKernel: %s, C: %.3f",model.getKernel().getClass().getName(),model.getC()));
        pW.println(evaluation.toSummaryString("\nResults\n======\n", false));
        pW.println(evaluation.toMatrixString("\nConfusion Matrix\n======\n"));
    }
    
    public void closeWriter(){
        
        pW.close();
    }
}
