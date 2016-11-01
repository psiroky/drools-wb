/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.workbench.services.verifier.api.client.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import org.drools.workbench.services.verifier.api.client.resources.css.AnalysisCssResources;

/**
 * General Decision Table resources.
 */
public interface AnalyzerResources
        extends
        ClientBundle {

    AnalyzerResources INSTANCE = GWT.create( AnalyzerResources.class );

    @Source("css/Analysis.css")
    AnalysisCssResources analysisCss();

};