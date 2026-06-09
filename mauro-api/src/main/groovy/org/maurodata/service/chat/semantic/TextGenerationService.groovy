package org.maurodata.service.chat.semantic

import groovy.transform.CompileStatic

@CompileStatic
interface TextGenerationService {
    String generate(String model, String prompt)
}
