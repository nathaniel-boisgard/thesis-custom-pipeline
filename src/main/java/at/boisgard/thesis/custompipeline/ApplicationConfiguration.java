/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.boisgard.thesis.custompipeline;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 *
 * @author BUERO
 */
@Configuration
@PropertySource("classpath:services.properties")
public class ApplicationConfiguration {
    
    public static final int APPLICATION_TRAINING_ENV = 1;
    public static final int APPLICATION_TEST_ENV = 2;
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(){
        
        return new PropertySourcesPlaceholderConfigurer();
    }
    
}
