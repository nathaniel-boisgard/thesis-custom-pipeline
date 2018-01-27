/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.model;

import at.boisgard.thesis.custompipeline.model.annotation.BaseAnnotation;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author BUERO
 */
@AllArgsConstructor
public @Data class AnnotatedWord {
        
    public final String word;
    public int start;
    public int end;
    public HashMap<Class<?>,BaseAnnotation> annotations;
}
