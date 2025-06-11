package org.maurodata.api.terminology

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import org.maurodata.api.model.ModelApi
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface CodeSetApi extends ModelApi<CodeSet> {

    @Get(value = Paths.CODE_SET_ID)
    CodeSet show(UUID id)

    @Post(value = Paths.FOLDER_LIST_CODE_SET)
    CodeSet create(UUID folderId, @Body @NonNull CodeSet codeSet)

    @Put(value = Paths.CODE_SET_ID)
    CodeSet update(UUID id, @Body @NonNull CodeSet codeSet)

    @Put(value = Paths.CODE_SET_TERM_ID)
    CodeSet addTerm(@NonNull UUID id,
                    @NonNull UUID termId)

    @Delete(value = Paths.CODE_SET_ID)
    HttpResponse delete(UUID id, @Body @Nullable CodeSet codeSet, @Nullable Boolean permanent)

    @Delete(value = Paths.CODE_SET_TERM_ID)
    CodeSet removeTermFromCodeSet(@NonNull UUID id,
                                  @NonNull UUID termId)

    @Get(value = Paths.FOLDER_LIST_CODE_SET)
    ListResponse<CodeSet> list(UUID folderId)

    @Get(value = Paths.CODE_SET_LIST)
    ListResponse<CodeSet> listAll()

    @Get(value = Paths.CODE_SET_TERM_LIST)
    ListResponse<Term> listAllTermsInCodeSet(@NonNull UUID id)

    @Put(value = Paths.CODE_SET_FINALISE)
    CodeSet finalise(UUID id, @Body FinaliseData finaliseData)

    @Get(Paths.CODE_SET_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)

    @Put(value = Paths.CODE_SET_NEW_BRANCH_MODEL_VERSION)
    CodeSet createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Get(Paths.CODE_SET_DOI)
    Map doi(UUID id)
}
