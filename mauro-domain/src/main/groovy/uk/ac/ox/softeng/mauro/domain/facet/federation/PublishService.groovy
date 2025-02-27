package uk.ac.ox.softeng.mauro.domain.facet.federation


import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Inject
import org.apache.commons.lang3.StringUtils

@CompileStatic
@AutoClone
class PublishService {
    final EmbeddedServer embeddedServer
    final MauroPluginService mauroPluginService

    @Inject
    PublishService(EmbeddedServer embeddedServer, MauroPluginService mauroPluginService) {
        this.embeddedServer = embeddedServer
        this.mauroPluginService = mauroPluginService
    }

    List<PublishedModel> getPublishedModels(List<Model> models) {
        String uriString = "${embeddedServer.getScheme()}://${embeddedServer.getHost()}:${embeddedServer.getPort()}"
        models.collect {Model model -> getPublishedModel(model, uriString)}
    }


    PublishedModel getPublishedModel(Model model, String uriString) {
        PublishedModel publishedModel = new PublishedModel(model.id.toString(),
                                                           model.label, model.modelVersion, model.modelVersionTag, model.description, model.modelType, model.lastUpdated,
                                                           model.dateCreated, model.author, [])
        String modelUrlPath = deduceModelUrlPath(model.domainType)
        publishedModel.links = mauroPluginService.mauroPlugins.findAll {
            ModelExporterPlugin.isInstance(it)
                && ((ModelExporterPlugin) it).getHandlesModelType() == model.class
        }.sort().collect {mauroPlugin ->
            new MauroLink("${uriString}/${modelUrlPath}/export/${mauroPlugin.namespace}/${mauroPlugin.name}/${mauroPlugin.version}",
                          ((ModelExporterPlugin) mauroPlugin).getContentType())
        }
        publishedModel
    }

    private static String deduceModelUrlPath(String domainType) {
        if (domainType == Terminology.class.simpleName) {
            return 'terminologies'
        } else return StringUtils.uncapitalize(domainType) + 's'
    }
}
