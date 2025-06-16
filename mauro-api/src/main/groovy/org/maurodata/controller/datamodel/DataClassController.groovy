package org.maurodata.controller.datamodel


import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.datamodel.DataClassApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.datamodel.DataClassContentRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.service.datamodel.DataClassService
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataClassController extends AdministeredItemController<DataClass, DataModel> implements DataClassApi {

    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    DataModelCacheableRepository dataModelRepository
    DataModelContentRepository dataModelContentRepository
    DataClassContentRepository dataClassContentRepository
    DataClassService dataClassService

    @Inject
    DataClassController(AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository, DataModelCacheableRepository dataModelRepository,
                        DataModelContentRepository dataModelContentRepository,
                        DataClassContentRepository dataClassContentRepository,
                        DataClassService dataClassService) {
        super(DataClass, dataClassRepository, dataModelRepository, dataClassContentRepository)
        this.dataClassService = dataClassService
        this.dataModelRepository = dataModelRepository
        this.dataClassRepository = dataClassRepository
        this.dataModelContentRepository = dataModelContentRepository
        this.dataClassContentRepository = dataClassContentRepository
    }

    @Audit
    @Get(Paths.DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, @Body @NonNull DataClass dataClass) {
        super.create(dataModelId, dataClass)
    }

    @Audit
    @Put(Paths.DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Audit(
        parentDomainType = DataModel,
        parentIdParamName = 'dataModelId',
        deletedObjectDomainType = DataClass
    )
    
    @Transactional
    @Delete(Paths.DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass) {
        DataClass dataClassToDelete = dataClassRepository.readById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToDelete, "DataClass $id not found")

        HttpResponse deletedResponse = super.delete(id, dataClass)
        deletedResponse
    }

    @Audit
    @Get(Paths.DATA_CLASS_SEARCH)
    ListResponse<DataClass> list(UUID dataModelId, @Nullable PaginationParams params = new PaginationParams()) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        List<DataClass> classes = dataClassRepository.readAllByDataModelAndParentDataClassIsNull(dataModel)
        classes.each {
            updateDerivedProperties(it)
        }
        ListResponse<DataClass>.from(classes,params)
    }

    @Audit
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID parentDataClassId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, UUID parentDataClassId, @Body @NonNull DataClass dataClass) {

        cleanBody(dataClass)
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.EDITOR, dataModel)
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        accessControlService.checkRole(Role.EDITOR, parentDataClass)
        dataClass.parentDataClass = parentDataClass
        createEntity(dataModel, dataClass)
        return dataClass
    }

    @Audit
    @Put(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'parentDataClassId',
        deletedObjectDomainType = DataClass
    )
    @Delete(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass) {
        super.delete(id, dataClass)
    }

    @Audit
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    ListResponse<DataClass> list(UUID dataModelId, UUID parentDataClassId) {
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        accessControlService.checkRole(Role.READER, parentDataClass)
        ListResponse.from(dataClassRepository.readAllByParentDataClass_Id(parentDataClassId))

    }

    @Audit
    @Put(Paths.DATA_CLASS_EXTENDS)
    DataClass createExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId) {
        DataClass sourceDataClass = dataClassRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, sourceDataClass)
        DataClass targetDataClass = dataClassRepository.readById(otherClassId)
        dataClassRepository.createExtensionRelationship(sourceDataClass, targetDataClass)
        dataClassRepository.findById(id)
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'id',
        deletedObjectDomainType = DataClass,
        description = 'Delete DataClass extends relationship'
    )
    @Delete(Paths.DATA_CLASS_EXTENDS)
    DataClass deleteExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId) {
        DataClass sourceDataClass = dataClassRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, sourceDataClass)
        DataClass targetDataClass = dataClassRepository.readById(otherClassId)
        dataClassRepository.deleteExtensionRelationship(sourceDataClass, targetDataClass)
        dataClassRepository.findById(id)
    }


    @Audit
    @Post(Paths.DATA_CLASS_COPY)
    @Transactional
    DataClass copyDataClass(UUID dataModelId, UUID otherModelId, UUID dataClassId) {
        DataModel dataModel = dataModelContentRepository.findWithContentById(dataModelId)
        accessControlService.checkRole(Role.EDITOR, dataModel)
        DataClass dataClass = dataClassContentRepository.readWithContentById(dataClassId)
        accessControlService.canDoRole(Role.EDITOR, dataClass)
        DataModel otherModel = dataModelRepository.readById(otherModelId)
        accessControlService.canDoRole(Role.READER, otherModel)
        //verify
        if (dataClass.dataModel.id != otherModel.id) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Cannot find dataClass $dataClassId for dataModel $otherModelId")
        }
        DataClass copied = dataClass.clone()
        copied.parentDataClass = null //do not keep existing source structure if source is child
        DataClass savedCopy = createEntity(dataModel, copied)
        savedCopy = dataClassService.copyReferenceTypes( savedCopy, dataModel)
        savedCopy.dataClasses = dataClassService.copyChildren(savedCopy, savedCopy.dataClasses, dataModel)
        savedCopy.dataElements = dataClassService.copyDataElementsAndDataTypes(savedCopy.dataElements, dataModel)
        savedCopy
    }

    @Get(Paths.DATA_CLASS_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }
}
