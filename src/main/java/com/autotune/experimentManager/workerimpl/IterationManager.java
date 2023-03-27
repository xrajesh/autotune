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
package com.autotune.experimentManager.workerimpl;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.experimentManager.data.result.*;
import com.autotune.experimentManager.handler.eminterface.EMHandlerFactory;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * This is worker to execute experiments  in several steps sequentially.
 */
public class IterationManager implements AutotuneWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(IterationManager.class);

    public IterationManager() {
    }

    @Override
    public void execute(KruizeObject kruizeObject, Object o, AutotuneExecutor autotuneExecutor, ServletContext context) {
       System.out.println("IterationManager exec");
        ExperimentTrial experimentTrial = (ExperimentTrial) o;
        if (experimentTrial.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
            LOGGER.debug("Experiment name {} started processing", experimentTrial.getExperimentName());
            initWorkflow(experimentTrial);
            autotuneExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            AutotuneWorker theWorker = new CallableFactory().create(autotuneExecutor.getWorker());
                            theWorker.execute(null, experimentTrial, autotuneExecutor, context);
                        }
                    }
            );
        } else {
            System.out.println("submitting..");
            findAndSubmitTask(experimentTrial, autotuneExecutor, context);
        }
    }

    /**
     * This function helps to execute workflow steps automatically based on
     * Experiment,Trial,Iteration status
     *
     * @param experimentTrial
     * @param autotuneExecutor
     * @param context
     */
    private void findAndSubmitTask(ExperimentTrial experimentTrial, AutotuneExecutor autotuneExecutor, ServletContext context) {
        AtomicBoolean taskSubmitted = new AtomicBoolean(false);
        if (experimentTrial.getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS)) { 
            System.out.println("in progress");
            experimentTrial.getTrialDetails().forEach((trialNum, trialDetails) -> {
                System.out.println("in progress2");
                if (taskSubmitted.get()) return;
                System.out.println("not returned in progress2");
                if (proceed(trialDetails)) {
                    //EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial, trialDetails);
                   System.out.println(trialDetails.getTrialMetaData());
                    trialDetails.getTrialMetaData().getIterations().forEach((iteration, iterationTrialMetaDetails) -> {
                        System.out.println("in progress 3");
                        if (taskSubmitted.get()) return;
                        if (proceed(iterationTrialMetaDetails)) {
                            iterationTrialMetaDetails.getWorkFlow().forEach((stepName, stepMetadata) -> {
                                if (taskSubmitted.get()) return;
                                if (stepMetadata.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                                    System.out.println("in progress que");
                                    String stepClassName = experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getIterationWorkflowMap().get(stepName);
                                    EMHandlerInterface theWorker = null;
                                    theWorker = new EMHandlerFactory().create(stepClassName);
                                    if (null != theWorker) {
                                        EMStatusUpdateHandler.updateTrialIterationDataStatus(experimentTrial, trialDetails, iterationTrialMetaDetails);
                                        System.out.println("in progress exec");
                                        theWorker.execute(experimentTrial,
                                                trialDetails,
                                                iterationTrialMetaDetails,
                                                stepMetadata,
                                                autotuneExecutor,
                                                context);
                                    } else {
                                        LOGGER.error("Class : {} implementation not found ", stepClassName);
                                        stepMetadata.setStatus(EMUtil.EMExpStatus.FAILED);
                                    }
                                    System.out.println("in progress set true");
                                    taskSubmitted.set(true);
                                    return;
                                }
                            });
                        }
                    });
                    // Call Trial Workflow
                    trialDetails.getTrialMetaData().getTrialWorkflow().forEach((stepName, stepsMetaData) -> {
                        System.out.println("in progress trail");
                        if (taskSubmitted.get()) return;
                        if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                            String stepClassName = experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getTrialWorkflowMap().get(stepName);
                            EMHandlerInterface theWorker = null;
                            theWorker = new EMHandlerFactory().create(stepClassName);
                            if (null != theWorker) {
                                System.out.println("in progress executed"+ stepClassName);
                                theWorker.execute(experimentTrial,
                                        trialDetails,
                                        null,
                                        stepsMetaData,
                                        autotuneExecutor,
                                        context);
                            } else {
                                LOGGER.error("Class : {} implementation not found ", stepClassName);
                                stepsMetaData.setStatus(EMUtil.EMExpStatus.FAILED);
                            }
                            taskSubmitted.set(true);
                            return;
                        }
                    });
                }
            });
        }
    }

    /*
        initiate Metadata to track Experiment progress and workflow progress.
     */
    private void initWorkflow(ExperimentTrial experimentTrial) {
        try {
            //Update experiment level metadata
            ExperimentMetaData experimentMetaData = experimentTrial.getExperimentMetaData();
            AutoTuneWorkFlow autoTuneWorkFlow = experimentMetaData.getAutoTuneWorkFlow();
            //System.out.println("x workflow " + autoTuneWorkFlow.toString());
            if (null == autoTuneWorkFlow) {
                autoTuneWorkFlow = new AutoTuneWorkFlow(experimentTrial.getExperimentSettings().isDo_experiment(),
                        experimentTrial.getExperimentSettings().isDo_monitoring(),
                        experimentTrial.getExperimentSettings().isWait_for_load(),
                        experimentTrial.getTrialResultURL());     //Check Workflow is Experiment or Monitoring Workflow
                experimentMetaData.setAutoTuneWorkFlow(autoTuneWorkFlow);
                experimentTrial.setExperimentMetaData(experimentMetaData);
            }
            System.out.println("x workflow 2" + autoTuneWorkFlow.toString());
            //update Trial level metadata
             experimentTrial.getTrialDetails().forEach((trailNum, trialDetail) -> {
                System.out.println("updating trail level");
                TrialMetaData trialMetaData = trialDetail.getTrialMetaData();
                if (trialMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                    System.out.println("updating trail level1" + experimentTrial.getExperimentSettings().getTrialSettings());
                    int iterationCount = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations());
                    LinkedHashMap<Integer, TrialIterationMetaData> trialIterationMap = new LinkedHashMap<>();
                    LinkedHashMap<Integer, LinkedHashMap<String, StepsMetaData>> iterationWorkflow = new LinkedHashMap<>();
                    System.out.println("updating trail level1.1");
                    IntStream.rangeClosed(1, iterationCount).forEach((iteration) -> {
                        TrialIterationMetaData trialIterationMetaData = new TrialIterationMetaData();
                        LinkedHashMap<String, StepsMetaData> stepsMetaDataLinkedHashMap = new LinkedHashMap<>();
                        experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getIterationWorkflowMap().forEach(
                                (workerName, workerNameClass) -> {
                                    StepsMetaData stepsMetaData = new StepsMetaData();
                                    stepsMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                                    stepsMetaData.setStepName(workerName);
                                    stepsMetaDataLinkedHashMap.put(
                                            workerName, stepsMetaData
                                    );
                                }
                        );
                        System.out.println("updating trail level2");
                        trialIterationMetaData.setWorkFlow(stepsMetaDataLinkedHashMap);
                        trialIterationMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                        trialIterationMetaData.setIterationNumber(iteration);
                        trialIterationMap.put(iteration, trialIterationMetaData);
                    });
                    trialMetaData.setIterations(trialIterationMap);
                    LinkedHashMap<String, StepsMetaData> trialWorkflowSteps = new LinkedHashMap<>();
                    experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getTrialWorkflowMap().forEach(
                            (workerName, workerNameClass) -> {
                                StepsMetaData stepsMetaData = new StepsMetaData();
                                stepsMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                                stepsMetaData.setStepName(workerName);
                                trialWorkflowSteps.put(
                                        workerName, stepsMetaData
                                );
                            }
                    );
                    System.out.println("updating trail level3");
                    trialMetaData.setTrialWorkflow(trialWorkflowSteps);
                    trialMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                    trialDetail.setTrialMetaData(trialMetaData);
                }
            }); 
            experimentTrial.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            LOGGER.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Name:{}-Status:{}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~", experimentTrial.getExperimentName(), EMUtil.EMExpStatus.IN_PROGRESS);
            if (null == experimentTrial.getExperimentMetaData().getBeginTimestamp())
                experimentTrial.getExperimentMetaData().setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            experimentTrial.setStatus(EMUtil.EMExpStatus.FAILED);
            experimentTrial.getExperimentMetaData().setEndTimestamp(new Timestamp(System.currentTimeMillis()));
        }
    }

    public boolean proceed(TrialDetails trialDetails) {
        System.out.println("in process funvtion" +trialDetails.getTrialMetaData().getStatus() );
        return trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.QUEUED) ||
                trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS);
    }

    public boolean proceed(TrialIterationMetaData trialIterationMetaData) {
        return trialIterationMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED) ||
                trialIterationMetaData.getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS);
    }

}
