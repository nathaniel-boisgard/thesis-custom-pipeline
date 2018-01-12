/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.pipeline;

import at.boisgard.thesis.custompipeline.model.AnnotatedSentence;
import at.boisgard.thesis.custompipeline.model.AnnotatedWord;
import at.boisgard.thesis.custompipeline.model.Utterance;
import at.boisgard.thesis.custompipeline.model.annotation.DependencyTreeAnnotation;
import at.boisgard.thesis.custompipeline.model.annotation.NamedEntityAnnotation;
import at.boisgard.thesis.custompipeline.model.annotation.PartOfSpeechAnnotation;
import at.boisgard.thesis.custompipeline.model.annotation.WordVectorAnnotation;
import at.boisgard.thesis.custompipeline.pipeline.service.CoreNLPService;
import at.boisgard.thesis.custompipeline.pipeline.service.Word2VecService;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 *
 * @author BUERO
 */
public class CustomPipeline {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(CustomPipeline.class);
    
    public CoreNLPService coreNLPService;
    public Word2VecService word2VecService;
    
    public CustomPipeline(CoreNLPService coreNLPService, Word2VecService word2VecService) throws URISyntaxException, IOException{
        
        this.coreNLPService = coreNLPService;
        this.word2VecService = word2VecService;
        
        LOGGER.info("Starting pipeline with {}, {}",coreNLPService,word2VecService);     
    }
    
    public void testPipeline(String input) throws URISyntaxException, IOException{
        
        Instant start = Instant.now();
        
        word2VecService.annotate(coreNLPService.annotate(input));
        
        Instant end = Instant.now();
        
        LOGGER.info("Parsed sentence {} in {}",input,Duration.between(start, end));
    }
    
    public ArrayList<Utterance> executePipelineOnUtteranceList(ArrayList<Utterance> utterances){
        
        utterances.forEach((u) -> {u = executePipelineOnUtterance(u);});
        
        return utterances;
    }
    
    public Utterance executePipelineOnUtterance(Utterance utterance){

        Instant start = Instant.now();
        
        // EXECUTE CORE NLP
        Annotation annotatedRawText = coreNLPService.annotate(utterance.getRawText());
        
        // CREATE LIST OF WORDS TOKENIZED BY CORE NLP AND ADD ANNOTATIONS
        ArrayList<AnnotatedWord> annotatedWords = new ArrayList<>();
        
        CoreMap coreNLPSentence = ((List<CoreMap>)annotatedRawText.get(CoreAnnotations.SentencesAnnotation.class)).get(0);
        
        // JUST GET ROOT OF DEPENDENCY TREE
        SemanticGraph dependencyTree = coreNLPSentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
        int beginningIndexOfDepTreeRoot = dependencyTree.getRoots().iterator().next().beginPosition();
           
        for(CoreLabel token: annotatedRawText.get(CoreAnnotations.TokensAnnotation.class)){
            
            String word = token.get(CoreAnnotations.TextAnnotation.class);

            AnnotatedWord annotatedWord = new AnnotatedWord(word,new HashMap<>());

            // ADD POS TAG            
            annotatedWord.annotations.put(PartOfSpeechAnnotation.class, new PartOfSpeechAnnotation(token.get(CoreAnnotations.PartOfSpeechAnnotation.class)));

            // ADD NER LABEL
            annotatedWord.annotations.put(NamedEntityAnnotation.class, new NamedEntityAnnotation(token.get(CoreAnnotations.NamedEntityTagAnnotation.class)));
            
            // ADD INFORMATION IF IT IS THE DEPENDENCY TREE ROOT
            annotatedWord.annotations.put(DependencyTreeAnnotation.class, new DependencyTreeAnnotation(token.beginPosition() == beginningIndexOfDepTreeRoot));
            
            // ADD WORD VECTOR
            try {            
                
                // FOR SOME MODELS, WE NEED TO CONVERT WORD TO LOWERCASE (GERMAN WIKI MODEL)
                annotatedWord.annotations.put(WordVectorAnnotation.class, new WordVectorAnnotation(word2VecService.getWordVector(word)));
            }catch (URISyntaxException | IOException ex) {
                LOGGER.error("Could not add word vector!", ex);
            }
            
            annotatedWords.add(annotatedWord);
        }
        
        // BUILD SENTENCE
        AnnotatedSentence annotatedSentence = new AnnotatedSentence(annotatedWords);
        
        // ADD TO UTTERANCE
        utterance.setAnnotatedSentence(annotatedSentence);
        
        Instant end = Instant.now();
        LOGGER.debug("Parsed sentence {} in {}",utterance.getRawText(),Duration.between(start, end));
        
        // RETURN ANNOTATED UTTERANCE
        return utterance;
    }
}
