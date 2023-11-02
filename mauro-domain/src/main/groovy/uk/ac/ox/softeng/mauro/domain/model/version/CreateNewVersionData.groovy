package uk.ac.ox.softeng.mauro.domain.model.version

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

/**
 * A DTO that contains information for a REST API new version/branch creation request.
 */
@CompileStatic
@Introspected
class CreateNewVersionData {
    /* TODO
    Boolean copyPermissions = true
    Boolean moveDataFlows = false
    Boolean copyDataFlows = false
    Boolean asynchronous = false
     */
    String label
    String branchName = 'main'

}
