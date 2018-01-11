package com.ticesso.pdf.forms.service;

import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JAX-RS application.
 */
public class FormsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.<Class<?>>singleton(FormsRessource.class);
    }

}
