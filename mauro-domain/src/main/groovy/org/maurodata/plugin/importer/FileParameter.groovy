/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.maurodata.plugin.importer

import groovy.transform.CompileStatic
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * @since 06/03/2018
 */
@CompileStatic
class FileParameter implements Closeable {

    byte[] fileContents
    InputStream inputStream
    Path filePath
    Boolean temporaryFile = false
    Long fileSize
    String fileName
    String fileType
    FileParameter() {

    }

    FileParameter(String fileName, String fileType, byte[] fileContents) {
        this.fileName = fileName
        this.fileType = fileType
        this.fileContents = fileContents
    }

    FileParameter(String fileName, String fileType, InputStream inputStream) {
        this.fileName = fileName
        this.fileType = fileType
        this.inputStream = inputStream
    }

    FileParameter(String fileName, String fileType, Path filePath, Boolean temporaryFile = false, Long fileSize = null) {
        this.fileName = fileName
        this.fileType = fileType
        this.filePath = filePath
        this.temporaryFile = temporaryFile
        this.fileSize = fileSize
    }

    byte[] getFileContents() {
        if (fileContents == null && inputStream != null) {
            fileContents = inputStream.bytes
            inputStream = null
        } else if (fileContents == null && filePath != null) {
            fileContents = Files.readAllBytes(filePath)
        }
        fileContents
    }

    void setFileContents(byte[] fileContents) {
        this.fileContents = fileContents
        this.inputStream = null
    }

    InputStream getInputStream() {
        if (inputStream != null) {
            return inputStream
        }
        if (fileContents != null) {
            return new ByteArrayInputStream(fileContents)
        }
        if (filePath != null) {
            return new BufferedInputStream(Files.newInputStream(filePath), 8192)
        }
        InputStream.nullInputStream()
    }

    File getFile() {
        filePath?.toFile()
    }

    @Override
    void close() {
        if (temporaryFile && filePath != null) {
            Files.deleteIfExists(filePath)
            filePath = null
        }
    }

}
