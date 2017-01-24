package com.blackducksoftware.integration.hub.artifactory.inspect

import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.io.BdioWriter
import com.blackducksoftware.bdio.io.LinkedDataContext
import com.blackducksoftware.bdio.model.BillOfMaterials
import com.blackducksoftware.bdio.model.CreationInfo
import com.blackducksoftware.bdio.model.Project

@Component
class BdioFileWriter {
    BdioWriter createBdioWriter(final OutputStream outputStream, final String projectName, final String projectVersion, final URI projectUri) {
        def linkedDataContext = new LinkedDataContext()
        def bdioWriter = new BdioWriter(linkedDataContext, outputStream)

        def bom = new BillOfMaterials()
        bom.setId("uuid:${UUID.randomUUID()}")
        bom.setName("${projectName} Black Duck I/O Export")
        bom.setSpecVersion(linkedDataContext.getSpecVersion())
        bom.setCreationInfo(CreationInfo.currentTool())
        bdioWriter.write(bom)

        def project = new Project()
        project.setId(projectUri.toString())
        project.setName(projectName)
        project.setVersion(projectVersion)
        bdioWriter.write(project)

        bdioWriter
    }
}