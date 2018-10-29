package com.synopsys.integration.blackduck.artifactory.legacy.metadata;

import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.CompositeComponentManager;

public class VersionBomComponentLinkTest {
    @Test
    public void testCreatingVersionBomComponentLink() {
        final CompositeComponentManager compositeComponentManager = new CompositeComponentManager(null, null);
        final String projectVersionLink = "https://int-hub04.dc1.lan/api/projects/19569890-08e9-4a4f-af7e-b28709a05f90/versions/525fd05c-ecc3-40fc-9368-fa11ac6f7ef3";
        final String componentVersionLink = "https://int-hub04.dc1.lan/api/components/dc3dee66-4939-4dea-b22f-ead288b4f117/versions/f9e2e6ff-7340-4fb3-a29f-a6fa98a10bfe";

        final UriSingleResponse<ProjectVersionView> projectVersionViewUriResponse = new UriSingleResponse<ProjectVersionView>(projectVersionLink, ProjectVersionView.class);
        final UriSingleResponse<ComponentVersionView> componentVersionViewUriResponse = new UriSingleResponse<ComponentVersionView>(componentVersionLink, ComponentVersionView.class);

        final String expectedVersionBomComponentLink = projectVersionLink + "/components/dc3dee66-4939-4dea-b22f-ead288b4f117/versions/f9e2e6ff-7340-4fb3-a29f-a6fa98a10bfe";

        Assert.assertEquals(expectedVersionBomComponentLink, compositeComponentManager.getVersionBomComponentUriResponse(projectVersionViewUriResponse, componentVersionViewUriResponse).uri);
    }

}
