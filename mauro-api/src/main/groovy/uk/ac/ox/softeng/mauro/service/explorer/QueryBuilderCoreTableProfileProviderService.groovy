package uk.ac.ox.softeng.mauro.service.explorer

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.profile.JsonBasedProfile

@Singleton
@CompileStatic
class QueryBuilderCoreTableProfileProviderService extends JsonBasedProfile {

    final static String NAMESPACE = 'uk.ac.ox.softeng.maurodatamapper.plugins.explorer.querybuilder'

    QueryBuilderCoreTableProfileProviderService(ObjectMapper objectMapper) {
        super(objectMapper)
    }

    @Override
    String getJsonFileName() {
        'queryBuilderCoreTableProfile.json'
    }

    @Override
    String getMetadataNamespace() {
        NAMESPACE
    }

    @Override
    String getNamespace() {
        NAMESPACE
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getDisplayName() {
        'Mauro Data Explorer - Query Builder Core Table'
    }
}
