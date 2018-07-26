/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.spring.context.properties;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.spring.context.annotation.NacosProperties;
import com.alibaba.nacos.spring.factory.NacosServiceFactory;
import com.alibaba.nacos.spring.util.NacosBeanUtils;
import com.alibaba.nacos.spring.util.NacosUtils;
import com.alibaba.spring.util.PropertyValuesUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.validation.DataBinder;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import static com.alibaba.nacos.spring.util.NacosUtils.resolveProperties;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * {@link NacosConfigurationProperties} Binding {@link BeanPostProcessor}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see NacosConfigurationProperties
 * @see BeanPostProcessor
 * @since 0.1.0
 */
public class NacosConfigPropertiesBindingPostProcessor implements BeanPostProcessor, ApplicationContextAware,
        EnvironmentAware {

    /**
     * The name of {@link NacosConfigPropertiesBindingPostProcessor} Bean
     */
    public static final String BEAN_NAME = "nacosConfigPropertiesBindingPostProcessor";

    private Properties globalNacosPropertiesBean;

    private NacosServiceFactory nacosServiceFactoryBean;

    private Environment environment;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        NacosConfigurationProperties nacosConfigurationProperties = findAnnotation(bean.getClass(), NacosConfigurationProperties.class);

        if (nacosConfigurationProperties != null) {
            bind(bean, beanName, nacosConfigurationProperties);
        }

        return bean;
    }

    private void bind(Object bean, String beanName, NacosConfigurationProperties nacosConfigurationProperties) {

        ConfigService configService = resolveConfigService(nacosConfigurationProperties);

        String dataId = nacosConfigurationProperties.dataId();

        String groupId = nacosConfigurationProperties.groupId();

        String content = NacosUtils.getContent(configService, dataId, groupId);

        DataBinder dataBinder = new DataBinder(bean);

        Properties properties = new Properties();

        try {
            properties.load(new StringReader(content));
            dataBinder.bind(new MutablePropertyValues(properties));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private ConfigService resolveConfigService(NacosConfigurationProperties nacosConfigurationProperties)
            throws BeansException {

        NacosProperties nacosProperties = nacosConfigurationProperties.properties();

        Properties properties = resolveProperties(nacosProperties, environment, globalNacosPropertiesBean);

        ConfigService configService = null;

        try {
            configService = nacosServiceFactoryBean.createConfigService(properties);
        } catch (NacosException e) {
            throw new BeanCreationException(e.getErrMsg(), e);
        }

        return configService;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        globalNacosPropertiesBean = NacosBeanUtils.getGlobalPropertiesBean(applicationContext);
        nacosServiceFactoryBean = NacosBeanUtils.getNacosServiceFactory(applicationContext);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
