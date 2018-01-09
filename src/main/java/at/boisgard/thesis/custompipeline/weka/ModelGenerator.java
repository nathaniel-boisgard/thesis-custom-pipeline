/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.weka;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.slf4j.LoggerFactory;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Instances;
import weka.core.Utils;

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
    
    public ModelGenerator(String trainingFileName, String testFileName){
        
        this.trainingFileName = trainingFileName;
        this.testFileName = testFileName;
        
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
        } catch (IOException ex) {
            LOGGER.error("Could not load ARFF file", ex);
        }
    }
    
    public void createAndEvaluateModels(){
            
        String[] modelDerivations = {
            "-C 0.01 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\"",
            "-C 0.1 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\"",
            "-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\"",
            "-C 10.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\"",
            "-C 100.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\"",
            "-C 0.01 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.RBFKernel -G 0.01 -C 250007\"",
            "-C 0.1 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.RBFKernel -G 0.01 -C 250007\"",
            "-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.RBFKernel -G 0.01 -C 250007\"",
            "-C 10.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.RBFKernel -G 0.01 -C 250007\"",
            "-C 100.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.RBFKernel -G 0.01 -C 250007\""
        };
        
        for(String derivation: modelDerivations){
            
            try {
                
                LOGGER.info("Attemting to build SVM Model derivation '{}'",derivation);
                
                createSVMModel(derivation);
                evaluateModel();
                
                LOGGER.info(evaluation.toSummaryString("\nResults\n======\n", false));
                
            } catch (Exception e) {
                
                LOGGER.error("Could not train and evaluate model!",e);
            }
        }
    }
    
    public void createSVMModel(String optionString) throws Exception{
        
        model = new SMO();
        String[] options = Utils.splitOptions(optionString);

        model.setOptions(options);
        model.buildClassifier(trainingInstances);   
    }
    
    public void evaluateModel() throws Exception{
        
        evaluation = new Evaluation(testInstances);
        evaluation.evaluateModel(model, testInstances);
    }
    
}
