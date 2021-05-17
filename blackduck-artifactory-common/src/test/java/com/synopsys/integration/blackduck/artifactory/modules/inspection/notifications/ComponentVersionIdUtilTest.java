package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme.ComponentVersionIdUtil;
import com.synopsys.integration.exception.IntegrationException;

class ComponentVersionIdUtilTest {

    private final static String DUMMY_ID = "08f3bea3-fbfb-4f01-97dd-3f49419f3ea9";

    @Test
    void extractComponentVersionId() throws IntegrationException {
        String expectedComponentVersionId = "e7142eee-d1a2-4b8e-ba87-01f84ac82b1f";
        String componentVersionId = ComponentVersionIdUtil.extractComponentVersionId(String.format("https://blackduck.synopsys.com/api/components/%s/versions/%s", DUMMY_ID, expectedComponentVersionId));
        assertEquals(expectedComponentVersionId, componentVersionId);
    }

    @Test
    void extractBOMComponentVersionId() throws IntegrationException {
        String expectedComponentVersionId = "e7142eee-d1a2-4b8e-ba87-01f84ac82b1f";
        String componentVersionId = ComponentVersionIdUtil.extractComponentVersionId(String.format(
            "https://blackduck.synopsys.com/api/projects/%s/versions/%s/components/%s/versions/%s",
            DUMMY_ID,
            DUMMY_ID,
            DUMMY_ID,
            expectedComponentVersionId
        ));
        assertEquals(expectedComponentVersionId, componentVersionId);
    }
}
