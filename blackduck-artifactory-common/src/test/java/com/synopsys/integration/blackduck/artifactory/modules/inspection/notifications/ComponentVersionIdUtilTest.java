package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme.ComponentVersionIdUtil;
import com.synopsys.integration.exception.IntegrationException;

class ComponentVersionIdUtilTest {

    @Test
    void extractComponentVersionId() throws IntegrationException {
        String expectedComponentVersionId = "e7142eee-d1a2-4b8e-ba87-01f84ac82b1f";
        String componentVersionId = ComponentVersionIdUtil.extractComponentVersionId("https://blackduck.synopsys.com/api/components/08f3bea3-fbfb-4f01-97dd-3f49419f3ea9/versions/" + expectedComponentVersionId);
        assertEquals(expectedComponentVersionId, componentVersionId);
    }

    @Test
    void extractComponentVersionIdThrows() {
        assertThrows(IntegrationException.class, () -> ComponentVersionIdUtil.extractComponentVersionId("https://blackduck.synopsys.com/api/components/08f3bea3-fbfb-4f01-97dd-3f49419f3ea9/"));
    }
}
