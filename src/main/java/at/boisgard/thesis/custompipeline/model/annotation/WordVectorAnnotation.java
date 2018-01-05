/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline.model.annotation;

import java.util.ArrayList;
import lombok.Data;

/**
 *
 * @author BUERO
 */
public @Data class WordVectorAnnotation extends BaseAnnotation{
    
    public final ArrayList<Double> vector;
}
