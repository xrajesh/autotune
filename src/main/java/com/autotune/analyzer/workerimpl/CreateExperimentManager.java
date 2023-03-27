/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.workerimpl;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.data.ExperimentInterface;
import com.autotune.analyzer.data.ExperimentInterfaceImpl;
import com.autotune.analyzer.deployment.AutotuneDeploymentInfo;
import com.autotune.analyzer.deployment.KruizeDeployment;
import com.autotune.common.experiments.DatasourceInfo;
import com.autotune.common.experiments.DeploymentPolicy;
import com.autotune.common.experiments.DeploymentSettings;
import com.autotune.common.experiments.DeploymentTracking;
import com.autotune.common.experiments.ExperimentSettings;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.ResourceDetails;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.experiments.TrialInfo;
import com.autotune.common.experiments.TrialSettings;
import com.autotune.common.k8sObjects.ContainerObject;
import com.autotune.common.k8sObjects.DeploymentObject;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.k8sObjects.SloInfo;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;
import com.autotune.experimentManager.data.result.TrialMetaData;
import com.autotune.utils.HttpUtils;
import com.autotune.utils.ServerContext;
import com.autotune.utils.TrialHelpers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static com.autotune.analyzer.Experimentator.startExperiment;
import static com.autotune.analyzer.deployment.KruizeDeployment.addLayerInfo;
import static com.autotune.analyzer.deployment.KruizeDeployment.matchPodsToAutotuneObject;

/**
 * Analyser worker which gets initiated via blocking queue either from rest API or Autotune CRD.
 * Move status from queue to in progress.
 * Start HPO loop if
 * TargetCluster : Local
 * mode : Experiment or Monitoring
 */

public class CreateExperimentManager implements AutotuneWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperimentManager.class);

    @Override
    public void execute(KruizeObject kruizeObject, Object o, AutotuneExecutor autotuneExecutor, ServletContext context) {
        System.out.println("CreateExperiement exec");
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        experimentInterface.addExperimentToDB(kruizeObject);
        //experimentInterface.updateExperimentStatus(kruizeExperiment, AnalyzerConstants.ExpStatus.IN_PROGRESS);

        if (kruizeObject.getExperimentUseCaseType().isLocalExperiment() || kruizeObject.getExperimentUseCaseType().isLocalMonitoring()) {
            System.out.println("CrEx "+ kruizeObject.getExperimentName());
            //matchPodsToAutotuneObject(kruizeObject);
            for (String kruizeConfig : KruizeDeployment.autotuneConfigMap.keySet()) {
                 System.out.println(kruizeConfig);
                addLayerInfo(KruizeDeployment.autotuneConfigMap.get(kruizeConfig), kruizeObject);
            }
            System.out.println("local local"+kruizeObject.toString());
            if (kruizeObject.getExperimentUseCaseType().isLocalExperiment()) {
                
                if (!KruizeDeployment.deploymentMap.isEmpty() &&
                        KruizeDeployment.deploymentMap.get(kruizeObject.getExperimentName()) != null) {
                    Map<String, ApplicationDeployment> depMap = KruizeDeployment.deploymentMap.get(kruizeObject.getExperimentName());
                    for (String deploymentName : depMap.keySet()) {
                        System.out.println("starting Experiment"+ kruizeObject.getExperimentName());
                        startExperiment(kruizeObject, depMap.get(deploymentName));
                    }
                    LOGGER.info("Added Kruize object " + kruizeObject.getExperimentName());
                } else {
                    LOGGER.error("Kruize object " + kruizeObject.getExperimentName() + " not added as no related deployments found!");
                }
            }else if (kruizeObject.getExperimentUseCaseType().isLocalMonitoring()){
                System.out.println("Local monitoring");
                System.out.println( KruizeDeployment.deploymentMap.isEmpty());
                System.out.println(kruizeObject.getExperimentName());
                
                        System.out.println("starting Experiment"+ kruizeObject.getExperimentName());
                        startRecommendation(kruizeObject);

                    //LOGGER.info("Added Kruize object " + kruizeObject.getExperimentName());
                
            }
        }
    }

    public static void startRecommendation(KruizeObject ko){
        System.out.println("in metrics");
        SloInfo sloInfo = ko.getSloInfo();
        System.out.println("in metrics"+ sloInfo.toString());
        StringBuilder trialResultUrl = new StringBuilder("http://localhost:8080/listExperiments")
        .append("?")
        .append("experiment_name")
        .append("=")
        .append(ko.getExperimentName());

TrialInfo trialInfo = new TrialInfo(ko.getExperimentId(),
        0,
        trialResultUrl.toString());
        DeploymentTracking deploymentTracking = new DeploymentTracking();
        DeploymentPolicy deploymentPolicy = new DeploymentPolicy("ACM");
        DeploymentSettings deploymentSettings = new DeploymentSettings(deploymentPolicy,
                deploymentTracking);
           TrialSettings ts = new     TrialSettings("1",null,null,null,null);
        ExperimentSettings experimentSettings = new ExperimentSettings(ts,
        deploymentSettings, false, true, false);

DatasourceInfo datasourceInfo = null;
try {
    datasourceInfo = new DatasourceInfo(AutotuneDeploymentInfo.getMonitoringAgent(),
            //new URL(AutotuneDeploymentInfo.getMonitoringAgentEndpoint()));
            new URL("http://localhost:10901"));
} catch (MalformedURLException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
}
HashMap<String, DatasourceInfo> datasourceInfoHashMap = new HashMap<>();
datasourceInfoHashMap.put(AutotuneDeploymentInfo.getMonitoringAgent(), datasourceInfo);  //Change key value as per YAML input
  //experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations()
         HashMap<String, HashMap<String, Metric>> containersMetricsHashMap = new HashMap<String, HashMap<String, Metric>>();
         for (DeploymentObject depobj : ko.getDeployments().values()){
          for (ContainerObject conobj : depobj.getContainers().values()){
            HashMap<String, Metric> metricsHashMap = new HashMap<String, Metric>();
            for (Metric metric : sloInfo.getFunctionVariables()) {
                metricsHashMap.put(metric.getName(), metric);
                 System.out.println(metric.getName());
             }
            containersMetricsHashMap.put(conobj.getContainer_name(), metricsHashMap);
          }
         }
         TrialIterationMetaData timd = new TrialIterationMetaData();
         timd.setIterationNumber(0);
         timd.setStatus(com.autotune.experimentManager.utils.EMUtil.EMExpStatus.IN_PROGRESS);
         LinkedHashMap<Integer, TrialIterationMetaData> it = new LinkedHashMap<Integer, TrialIterationMetaData>();
         it.put(0, timd);
         TrialMetaData trialMetaData = new TrialMetaData();
                 trialMetaData.setCreationDate(new Timestamp(System.currentTimeMillis()));
                 trialMetaData.setIterations(it);
         TrialDetails td = new  TrialDetails("0", null);
         td.setTrialMetaData(trialMetaData);
         ResourceDetails rd = new ResourceDetails(ko.getNamespace(), ko.getDeployment_name());
         HashMap<String, TrialDetails>  trailDetails  = new HashMap<String, TrialDetails> ();
         trailDetails.put(ko.getExperimentId(), td);
         ExperimentTrial experimentTrial = new ExperimentTrial(ko.getExperimentName(),
        ko.getMode(),
        rd,
        ko.getExperimentId(),
        null,
        containersMetricsHashMap,
         trialInfo,
       datasourceInfoHashMap,
       experimentSettings,
        trailDetails );
        experimentTrial.setTrialResultURL(ServerContext.UPDATE_RESULTS_END_POINT);
        SendTrialToEM(experimentTrial);
 
     /*   System.out.println("in metrics"+ sloInfo.toString());
        for (Metric metric : sloInfo.getFunctionVariables()) {
            System.out.println(metric.toString());
        }*/
    }

    public static void SendTrialToEM(ExperimentTrial experimentTrial) {
try {
int trialNumber = experimentTrial.getTrialInfo().getTrialNum();
// Prepare to send the trial config to EM
String experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);
System.out.println(experimentTrialJSON);
/* STEP 4: Send trial to EM */

URL createExperimentTrialURL = new URL("http://localhost:8080/"+"createExperimentTrial");
String runId = HttpUtils.postRequest(createExperimentTrialURL, experimentTrialJSON.toString());

System.out.println("run id "+ runId);
} catch (Exception e) {
e.printStackTrace();
}
}
}
