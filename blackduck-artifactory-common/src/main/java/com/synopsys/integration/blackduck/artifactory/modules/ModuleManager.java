/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.modules;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.BooleanUtils;
import org.artifactory.exception.CancelException;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.LogUtil;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

/**
 * This class is the public API for the blackDuckPlugin groovy script.
 * Changing this interface should be avoided if possible and any changes
 * made here must be reflected in the blackDuckPlugin.groovy file
 * in hub-artifactory
 */
public class ModuleManager {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ModuleRegistry moduleRegistry;
    private final FeatureAnalyticsCollector featureAnalyticsCollector;
    private final ScanModule scanModule;
    private final InspectionModule inspectionModule;
    private final PolicyModule policyModule;
    private final AnalyticsModule analyticsModule;
    private final ScanModuleConfig scanModuleConfig;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final PolicyModuleConfig policyModuleConfig;
    private final AnalyticsModuleConfig analyticsModuleConfig;

    public ModuleManager(final ModuleRegistry moduleRegistry, final FeatureAnalyticsCollector featureAnalyticsCollector, final ScanModule scanModule, final InspectionModule inspectionModule, final PolicyModule policyModule,
        final AnalyticsModule analyticsModule, final ScanModuleConfig scanModuleConfig, final InspectionModuleConfig inspectionModuleConfig, final PolicyModuleConfig policyModuleConfig, final AnalyticsModuleConfig analyticsModuleConfig) {
        this.moduleRegistry = moduleRegistry;
        this.featureAnalyticsCollector = featureAnalyticsCollector;
        this.scanModule = scanModule;
        this.inspectionModule = inspectionModule;
        this.policyModule = policyModule;
        this.analyticsModule = analyticsModule;
        this.scanModuleConfig = scanModuleConfig;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.policyModuleConfig = policyModuleConfig;
        this.analyticsModuleConfig = analyticsModuleConfig;
    }

    public static ModuleManager createFromModules(final ModuleRegistry moduleRegistry, final FeatureAnalyticsCollector featureAnalyticsCollector, final ScanModule scanModule, final InspectionModule inspectionModule,
        final PolicyModule policyModule,
        final AnalyticsModule analyticsModule) {
        final ScanModuleConfig scanModuleConfig = scanModule.getModuleConfig();
        final InspectionModuleConfig inspectionModuleConfig = inspectionModule.getModuleConfig();
        final PolicyModuleConfig policyModuleConfig = policyModule.getModuleConfig();
        final AnalyticsModuleConfig analyticsModuleConfig = analyticsModule.getModuleConfig();

        return new ModuleManager(moduleRegistry, featureAnalyticsCollector, scanModule, inspectionModule, policyModule, analyticsModule, scanModuleConfig, inspectionModuleConfig, policyModuleConfig, analyticsModuleConfig);
    }

    public void setModuleState(final TriggerType triggerType, final Map<String, List<String>> params) {
        LogUtil.start(logger, "setModuleState", triggerType);

        for (final Map.Entry<String, List<String>> entry : params.entrySet()) {
            if (entry.getValue().size() > 0) {
                final String moduleStateRaw = entry.getValue().get(0);
                final boolean moduleState = BooleanUtils.toBoolean(moduleStateRaw);
                final String moduleName = entry.getKey();
                final List<ModuleConfig> moduleConfigs = moduleRegistry.getModuleConfigsByName(moduleName);

                if (moduleConfigs.isEmpty()) {
                    logger.warn(String.format("No registered modules with the name '%s' found. Hit the blackDuckTestConfig endpoint to see why.", moduleName));
                } else {
                    moduleConfigs.forEach(moduleConfig -> {
                        logger.warn(String.format("Setting %s's enabled state to %b", moduleConfig.getModuleName(), moduleState));
                        moduleConfig.setEnabled(moduleState);
                    });
                }
            }
        }

        LogUtil.finish(logger, "setModuleState", triggerType);
    }

    public void triggerScan(final TriggerType triggerType) {
        runMethod(scanModuleConfig, triggerType, scanModule::triggerScan);
    }

    public void addPolicyStatus(final TriggerType triggerType) {
        runMethod(scanModuleConfig, triggerType, scanModule::addPolicyStatus);
    }

    public void deleteScanProperties(final TriggerType triggerType, final Map<String, List<String>> params) {
        runMethod(scanModuleConfig, triggerType, () -> scanModule.deleteScanProperties(params));
    }

    public void deleteScanPropertiesFromFailures(final TriggerType triggerType, final Map<String, List<String>> params) {
        runMethod(scanModuleConfig, triggerType, () -> scanModule.deleteScanPropertiesFromFailures(params));
    }

    public void deleteScanPropertiesFromOutOfDate(final TriggerType triggerType, final Map<String, List<String>> params) {
        runMethod(scanModuleConfig, triggerType, () -> scanModule.deleteScanPropertiesFromOutOfDate(params));
    }

    public void updateDeprecatedScanProperties(final TriggerType triggerType) {
        runMethod(scanModuleConfig, triggerType, scanModule::updateDeprecatedProperties);
    }

    public String getBlackDuckScanCron() {
        return scanModuleConfig.getScanCron();
    }

    public String getBlackDuckAddPolicyStatusCron() {
        return scanModuleConfig.getAddPolicyStatusCron();
    }

    public void updateDeprecatedInspectionProperties(final TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::updateDeprecatedProperties);
    }

    public void handleAfterCreateEvent(final ItemInfo itemInfo, final TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, itemInfo, inspectionModule::handleAfterCreateEvent);
    }

    public void handleAfterCopyEvent(final RepoPath targetRepoPath, final TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, targetRepoPath, inspectionModule::handleAfterCopyEvent);
    }

    public void identifyArtifacts(final TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::identifyArtifacts);
    }

    public void populateMetadata(final TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::populateMetadata);
    }

    public void updateMetadata(final TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::updateMetadata);
    }

    public void deleteInspectionProperties(final TriggerType triggerType, final Map<String, List<String>> params) {
        runMethod(inspectionModuleConfig, triggerType, () -> inspectionModule.deleteInspectionProperties(params));
    }

    public void reinspectFromFailures(final TriggerType triggerType, final Map<String, List<String>> params) {
        runMethod(inspectionModuleConfig, triggerType, () -> inspectionModule.reinspectFromFailures(params));
    }

    public String getBlackDuckIdentifyArtifactsCron() {
        return inspectionModuleConfig.getIdentifyArtifactsCron();
    }

    public String getBlackDuckPopulateMetadataCron() {
        return inspectionModuleConfig.getPopulateMetadataCron();
    }

    public String getBlackDuckUpdateMetadataCron() {
        return inspectionModuleConfig.getUpdateMetadataCron();
    }

    public void handleBeforeDownloadEvent(final TriggerType triggerType, final RepoPath repoPath) throws CancelException {
        runMethod(policyModuleConfig, triggerType, repoPath, policyModule::handleBeforeDownloadEvent);
    }

    public String submitAnalytics(final TriggerType triggerType) {
        final Boolean success = runMethod(analyticsModuleConfig, triggerType, analyticsModule::submitAnalytics);
        return success == null ? "AnalyticsModule disabled" : success.toString();
    }

    /**
     * Below are utility methods to help reuse the code for logging and analytics
     */

    private <T> void runMethod(final ModuleConfig moduleConfig, final TriggerType triggerType, final T consumable, final Consumer<T> consumer) {
        if (startMethodRun(moduleConfig, triggerType)) {
            consumer.accept(consumable);
            finishMethodRun(moduleConfig, triggerType, triggerType);
        }
    }

    private <T, U> void runMethod(final ModuleConfig moduleConfig, final TriggerType triggerType, final T consumable1, final U consumable2, final BiConsumer<T, U> consumer) {
        if (startMethodRun(moduleConfig, triggerType)) {
            consumer.accept(consumable1, consumable2);
            finishMethodRun(moduleConfig, triggerType, triggerType);
        }
    }

    private void runMethod(final ModuleConfig moduleConfig, final TriggerType triggerType, final Runnable runnable) {
        if (startMethodRun(moduleConfig, triggerType)) {
            runnable.run();
            finishMethodRun(moduleConfig, triggerType, triggerType);
        }
    }

    private <T> T runMethod(final ModuleConfig moduleConfig, final TriggerType triggerType, final Supplier<T> supplier) {
        T result = null;

        if (startMethodRun(moduleConfig, triggerType)) {
            result = supplier.get();
            finishMethodRun(moduleConfig, triggerType, result);
        }

        return result;
    }

    private boolean startMethodRun(final ModuleConfig moduleConfig, final TriggerType triggerType) {
        final String methodName = getMethodName();
        if (moduleConfig.isEnabled()) {
            LogUtil.start(logger, methodName, triggerType);
        } else if (triggerType.equals(TriggerType.REST_REQUEST)) {
            logger.warn(String.format("The %s is disabled! Re-enable it and hit the endpoint again", moduleConfig.getModuleName()));
        } else {
            logger.info(String.format("The %s is disabled. Cannot execute %s", moduleConfig.getModuleName(), methodName));
        }

        return moduleConfig.isEnabled();
    }

    private void finishMethodRun(final ModuleConfig moduleConfig, final TriggerType triggerType, final Object result) {
        final String methodName = getMethodName();
        featureAnalyticsCollector.logFeatureHit(moduleConfig.getModuleName(), methodName, result);
        LogUtil.finish(logger, methodName, triggerType);
    }

    private String getMethodName() {
        final int methodDepth = 4;
        final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        final String result = "UnknownMethod";

        if (stackTraceElements.length - methodDepth > 0) {
            return stackTraceElements[methodDepth].getMethodName();
        }

        return result;
    }
}
