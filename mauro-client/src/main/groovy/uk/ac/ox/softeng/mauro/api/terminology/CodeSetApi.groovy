package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface CodeSetApi extends ModelApi<CodeSet> {

    @Get(value = Paths.CODE_SET_BY_ID)
    CodeSet show(UUID id)

    @Post(value = Paths.CODE_SETS_BY_FOLDER_ID)
    CodeSet create(UUID folderId, @Body @NonNull CodeSet codeSet)

    @Put(value = Paths.CODE_SET_BY_ID)
    CodeSet update(UUID id, @Body @NonNull CodeSet codeSet)

    @Put(value = Paths.TERM_TO_CODE_SET)
    CodeSet addTerm(@NonNull UUID id,
                    @NonNull UUID termId)

    @Delete(value = Paths.CODE_SET_BY_ID)
    HttpStatus delete(UUID id, @Body @Nullable CodeSet codeSet)

    @Delete(value = Paths.TERM_TO_CODE_SET)
    CodeSet removeTermFromCodeSet(@NonNull UUID id,
                                  @NonNull UUID termId)

    @Get(value = Paths.CODE_SETS_BY_FOLDER_ID)
    ListResponse<CodeSet> list(UUID folderId)

    @Get(value = Paths.CODE_SETS)
    ListResponse<CodeSet> listAll()

    @Get(value = Paths.TERMS_IN_CODE_SET)
    ListResponse<Term> listAllTermsInCodeSet(@NonNull UUID id)

    @Put(value = Paths.FINALISE_CODE_SETS)
    CodeSet finalise(UUID id, @Body FinaliseData finaliseData)

    @Get('/codeSets/{id}/diff/{otherId}')
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)

    @Put(value = Paths.CODE_SET_NEW_BRANCH_MODEL_VERSION)
    CodeSet createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)
}
