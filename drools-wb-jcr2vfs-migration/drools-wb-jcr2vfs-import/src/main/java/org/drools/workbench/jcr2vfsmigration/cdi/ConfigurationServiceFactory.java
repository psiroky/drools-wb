package org.drools.workbench.jcr2vfsmigration.cdi;

import org.guvnor.structure.server.config.ConfigGroup;
import org.guvnor.structure.server.config.ConfigItem;
import org.guvnor.structure.server.config.ConfigType;
import org.guvnor.structure.server.config.ConfigurationFactory;
import org.guvnor.structure.server.config.ConfigurationService;
import org.guvnor.structure.server.config.SecureConfigItem;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.List;

@ApplicationScoped
public class ConfigurationServiceFactory {

    @Produces
    public ConfigurationService getConfigurationService() {
        return new ConfigurationService() {

            @Override
            public List<ConfigGroup> getConfiguration(ConfigType configType) {
                return null;
            }

            @Override
            public boolean addConfiguration(ConfigGroup configGroup) {
                return false;
            }

            @Override
            public boolean updateConfiguration(ConfigGroup configGroup) {
                return false;
            }

            @Override
            public boolean removeConfiguration(ConfigGroup configGroup) {
                return false;
            }
        };
    }

    @Produces
    public ConfigurationFactory getConfigurationFactory() {
        return new ConfigurationFactory() {
            @Override
            public ConfigGroup newConfigGroup(ConfigType configType, String s, String s1) {
                return null;
            }

            @Override
            public ConfigItem<String> newConfigItem(String s, String s1) {
                return null;
            }

            @Override
            public ConfigItem<Boolean> newConfigItem(String s, boolean b) {
                return null;
            }

            @Override
            public SecureConfigItem newSecuredConfigItem(String s, String s1) {
                return null;
            }

            @Override
            public ConfigItem<List> newConfigItem(String s, List list) {
                return null;
            }

            @Override
            public ConfigItem<Object> newConfigItem(String s, Object o) {
                return null;
            }
        };
    }
}
