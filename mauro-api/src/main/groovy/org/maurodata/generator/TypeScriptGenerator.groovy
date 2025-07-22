package org.maurodata.generator

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.web.router.Router
import jakarta.inject.Inject
import org.maurodata.ApiClient
import org.reflections.Reflections

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

class TypeScriptGenerator extends ApiClient {

    @Inject
    Router router


    @Override
    void run() {

        // Initialize Reflections with the package
        Reflections reflections = new Reflections("org.maurodata")
        Set<Class> controllerClasses = reflections.getTypesAnnotatedWith(Controller).
            findAll{!it.name.endsWith("Intercepted")} as Set<Class>
        Set<Class> domainTypes = [] as Set<Class>
        controllerClasses.each {controllerClass ->
            controllerClass.declaredMethods.findAll {method ->
                method.declaredAnnotations.any {annotation ->
                    [Post, Put, Delete].contains(annotation.annotationType())
                } &&
                !method.name.startsWith('$')

            }.each {method ->
                domainTypes.add(method.returnType)
                method.parameterTypes.each { parameterType ->
                    domainTypes.add(parameterType)
                }
            }
        }

        Set<Class> extendsTypes = [] as Set<Class>
        domainTypes.each {domainType ->
            while(domainType.packageName.startsWith("org.maurodata")) {
                extendsTypes.add(domainType)
                domainType = domainType.superclass
            }
        }
        Set<String> domainClassNames = extendsTypes.collect {
            it.simpleName
        }
        extendsTypes.findAll { domainType ->
            domainType.packageName.startsWith("org.maurodata.domain")
        }.each {domainType ->
            File file = new File("/Users/james/git/mauro/mauro-micronaut/mauro-client/build/typescript/${domainType.simpleName}.d.ts")
            StringBuffer sb = new StringBuffer("")
            String extendsString = ''
            if(domainType.superclass.packageName.startsWith("org.maurodata")) {
                extendsString = " extends ${domainType.superclass.simpleName}"
            }
            sb.append("export interface ${domainType.simpleName}${extendsString} {\n")
            domainType.declaredFields.each {field ->
                if(!Modifier.isStatic(field.modifiers)) {
                    sb.append("    ${field.name}: ${mapField(field, domainClassNames)};\n")
                }
            }
            sb.append("}\n")
            file.write(sb.toString())
        }
    }

    final static Map<String, String> typeMap = [
    'String': 'string',
    'Date': 'date',
    'UUID': 'uuid',
    'Boolean': 'boolean',
    'boolean': 'boolean',
    'Integer': 'bigint',
    'Instant': 'date']

    static String mapField(Field javaField, Set<String> allClasses) {
        if(javaField.type == List || javaField.type == Set) {
            return mapFieldType(((Class) ((ParameterizedType) javaField.getGenericType()).actualTypeArguments[0]), allClasses) + "[]"
        }
        else {
            return mapFieldType(javaField.type, allClasses)
        }
    }

    static String mapFieldType(Class javaClass, Set<String> allClasses) {
        String jsType = typeMap[javaClass.simpleName]
        if(!jsType) {
            if(allClasses.contains(javaClass.simpleName)) {
                jsType = javaClass.simpleName
            } else {
                System.err.println("Unknown type: ${javaClass}")
                jsType = 'string'
            }
        }
        return jsType

    }
}
