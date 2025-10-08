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

/**
 * @since 06/03/2018
 */
class FileParameter {

    byte[] fileContents
    String fileName
    String fileType
    FileParameter() {

    }

    FileParameter(String fileName, String fileType, byte[] fileContents) {
        this.fileName = fileName
        this.fileType = fileType
        this.fileContents = Arrays.copyOf(fileContents, fileContents.size())
    }

    byte[] getFileContents() {
        Arrays.copyOf(fileContents, fileContents.size())
    }

    void setFileContents(byte[] fileContents) {
        this.fileContents = Arrays.copyOf(fileContents, fileContents.size())
    }

    //void setFileContents(ByteArrayInputStream fileContents) {
    //    setFileContents(fileContents.readAllBytes())
    //}

    InputStream getInputStream() {
        new ByteArrayInputStream(fileContents)
    }


}
