/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *
 * @author BUERO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class Utterance {
    
    public AnnotatedSentence annotatedSentence;
    
    @JsonProperty("text")
    public String rawText;
    
    @JsonProperty
    public String intent;
    
}
