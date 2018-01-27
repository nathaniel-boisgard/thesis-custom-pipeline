/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *
 * @author BUERO
 */
public @Data class ParseResponseNamedEntity {
    
    @JsonProperty
    public final int start;
    
    @JsonProperty
    public final int end;
    
    @JsonProperty
    public final String value;
    
    @JsonProperty
    public final String entity;
}
