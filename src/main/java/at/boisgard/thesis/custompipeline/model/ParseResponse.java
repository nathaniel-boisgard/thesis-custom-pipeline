/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import lombok.Data;

/**
 *
 * @author BUERO
 */
public @Data class ParseResponse {
    
    @JsonProperty
    public final String query;
    
    @JsonProperty
    public String intent;
    
    @JsonProperty
    public ArrayList<ParseResponseNamedEntity> entities;
}


