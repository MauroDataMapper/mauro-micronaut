package uk.ac.ox.softeng.mauro.domain.model.version

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class CreateNewVersionData {

    Boolean copyPermissions = true
    /* TODO
    Boolean moveDataFlows = false
    Boolean copyDataFlows = false
    Boolean asynchronous = false
     */
    String label
    String branchName = 'main'

}
