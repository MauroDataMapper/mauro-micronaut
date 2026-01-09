package org.maurodata.util

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import jakarta.inject.Singleton
import org.maurodata.ErrorHandler

@Singleton
@CompileStatic
class PathStringUtils {

    static final String VERTICAL_BAR_ESCAPE = "\\|"
    static final String COLON = ":"
    static final String BRANCH_DELIMITER = '$'
    static final String DISCARD_AFTER_VERSION = ~/.*\$/
    static final String DISCARD_BEFORE_VERSION = ~/\$(.+)/
    static final String REMOVE_VERSION_DELIM = '\\$.*'
    /**
     *
     * @param path eg  "fo:soluta eum architecto|dm:modi unde est$matrix|dc:est quasi vel|de:new data element label"
     *        pathPrefix eg 'de',
     *
     *        path: "fo:soluta eum architecto|dm:modi unde est$1.0.0|dc:est quasi vel|dc:est sed hic",
     *        pathPrefix: dc
     *        returns last : "est sed hic"  (ie child)
     *
     *  split each subpath '|'
     *  find subpath matching pathPrefix
     * @return the subpath eg  "new data element label"
     */
    static String getItemSubPath(String pathPrefix, String fullPath) {
        String[] pathSubPaths = splitBy(fullPath, VERTICAL_BAR_ESCAPE).reverse()
        String subPathAndPrefix = pathSubPaths.find {it.startsWith("$pathPrefix:")}
        if (!subPathAndPrefix) ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Path starting with $pathPrefix not found")
        String subPath = subPathAndPrefix - "$pathPrefix:"
        // Discard version after subpath if any
        String subPathOnly = subPath.find(DISCARD_AFTER_VERSION) ?: subPath
        //find up to and including $ eg branch
        subPathOnly - BRANCH_DELIMITER
    }

    static String lastSubPath(String subPath) {
        splitBy(subPath, VERTICAL_BAR_ESCAPE).last()
    }

    static String[] splitBy(String path, String separator) {
        path.split(separator)
    }

    static String getVersionFromPath(String fullPath) {
        String[] parts = fullPath.split(VERTICAL_BAR_ESCAPE)
        String version
        parts.each {
            version = it.find(DISCARD_BEFORE_VERSION) {match, captured -> captured} ?: version
        }
        version
    }

}
