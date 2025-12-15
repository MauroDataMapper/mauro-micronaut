package org.maurodata.domain.facet.federation


import org.maurodata.domain.model.version.ModelVersion

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated

import java.time.Instant

@CompileStatic
@AutoClone
@Introspected
class PublishedModel implements Comparable<PublishedModel> {

    String modelId
    @JsonProperty("label")
    String modelLabel
    @JsonProperty("version")
    ModelVersion modelVersion
    @Nullable
    String modelVersionTag
    @Nullable
    String description
    String modelType
    @DateUpdated
    @JsonAlias(['last_updated'])
    Instant lastUpdated
    @DateCreated
    @JsonAlias(['date_created'])
    Instant dateCreated

    @Nullable
    Instant datePublished

    @Nullable
    String author
    List<MauroLink> links = []

    PublishedModel() {
    }

    PublishedModel(String modelId, String modelLabel, ModelVersion modelVersion, String modelVersionTag, String description, String modelType, Instant lastUpdated,
                   Instant dateCreated, Instant dateFinalised,String author, List<MauroLink> links) {
        this.modelId = modelId
        this.modelLabel = modelLabel
        this.modelVersion = modelVersion
        this.modelVersionTag = modelVersionTag
        this.description = description
        this.modelType = modelType
        this.lastUpdated = lastUpdated
        this.dateCreated = dateCreated
        this.datePublished = dateFinalised
        this.author = author
        this.links = links
    }


    String getTitle() {
        "${modelLabel} ${modelVersion}"
    }


    String getDescription() {
        description == title ? null : description
    }


    void setDescription(String description) {
        if (description && description != title) this.description = description
    }

    @Override
    int compareTo(PublishedModel that) {
        int res = this.modelLabel <=> that.modelLabel
        if (res == 0) res = this.modelVersion <=> that.modelVersion
        res
    }


    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof PublishedModel)) return false

        PublishedModel that = (PublishedModel) o

        if (author != that.author) return false
        if (dateCreated != that.dateCreated) return false
        if (datePublished != that.datePublished) return false
        if (description != that.description) return false
        if (lastUpdated != that.lastUpdated) return false
        if (links != that.links) return false
        if (modelId != that.modelId) return false
        if (modelLabel != that.modelLabel) return false
        if (modelType != that.modelType) return false
        if (modelVersion != that.modelVersion) return false
        if (modelVersionTag != that.modelVersionTag) return false
        return true
    }

    int hashCode() {
        int result
        result = modelId.hashCode()
        result = 31 * result + modelLabel.hashCode()
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + (modelVersionTag != null ? modelVersionTag.hashCode() : 0)
        result = 31 * result + (description != null ? description.hashCode() : 0)
        result = 31 * result + modelType.hashCode()
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0)
        result = 31 * result + dateCreated.hashCode()
        result = 31 * result + (datePublished != null ? datePublished.hashCode() : 0)
        result = 31 * result + (author != null ? author.hashCode() : 0)
        return result
    }

    @Override
    String toString() {
        return "PublishedModel{" +
               "modelId='" + modelId + '\'' +
               ", modelLabel='" + modelLabel + '\'' +
               ", modelVersion=" + modelVersion +
               ", modelVersionTag='" + (modelVersionTag ? modelVersionTag: "") + '\'' +
               ", description='" + (description ? description : "") + '\'' +
               ", modelType='" + modelType + '\'' +
               ", lastUpdated=" + lastUpdated +
               ", dateCreated=" + dateCreated +
               ", datePublished=" + (datePublished ? datePublished : null) +
               ", author='" + (author ? author : "")  + '\'' +
               ", links=" + links +
               '}'
    }
}

