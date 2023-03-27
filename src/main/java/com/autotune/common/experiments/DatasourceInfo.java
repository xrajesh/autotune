/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.common.experiments;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.LoggerFactory;

/**
 * This Data object is used to store information about metric collectors like Prometheus, LogicMonitor, Dynatrace, Amazon Timestream etc
 * Example
 * "datasource_info": {
 * "name": "prometheus",
 * "url": "http://10.101.144.137:9090"
 * }
 */
public class DatasourceInfo {
    private final String provider; //monitoringAgent
    private final URL url; //monitoringAgentEndpoint


    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DatasourceInfo.class);
    
    public DatasourceInfo(String providerEnvVar, String urlEnvVar) {
        String provider = System.getenv(providerEnvVar);
        String urlString = System.getenv(urlEnvVar);
   
        if (provider == null || urlString == null) {
           throw new IllegalArgumentException("Both provider and url environment variables must be set.");
       }
   
       URL url = null;
           try {
               url = new URL(urlString);
           } catch (MalformedURLException e) {
               LOGGER.error( "Error creating URL: " + e.getMessage());
           }
   
           this.provider = provider;
           this.url = url;
       }

    public String getProvider() {
        return provider;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "DatasourceInfo{" +
                "provider='" + provider + '\'' +
                ", url=" + url +
                '}';
    }
}
