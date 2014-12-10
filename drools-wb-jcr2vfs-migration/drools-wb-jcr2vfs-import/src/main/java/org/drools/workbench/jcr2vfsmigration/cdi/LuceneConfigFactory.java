package org.drools.workbench.jcr2vfsmigration.cdi;

import org.uberfire.ext.metadata.backend.lucene.LuceneConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

@ApplicationScoped
public class LuceneConfigFactory {

    @Produces
    @Named("luceneConfig")
    public LuceneConfig getLuceneConfig() {
        return new LuceneConfig(null, null, null, null);
    }
}
