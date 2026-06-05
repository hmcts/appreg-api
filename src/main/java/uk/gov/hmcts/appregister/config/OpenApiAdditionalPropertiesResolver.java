package uk.gov.hmcts.appregister.config;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

class OpenApiAdditionalPropertiesResolver {
    private static final String PATH_RESOURCE_PATTERN = "classpath*:openapi/paths/**/*.yaml";
    private static final String GENERATED_MODEL_PACKAGE =
            "uk.gov.hmcts.appregister.generated.model";
    private static final String COMPONENT_SCHEMAS_PATH = "components/schemas/";
    private static final String OPENAPI_CLASSPATH_PREFIX = "classpath:openapi/";
    private static final String REF_KEY_SUFFIX = "schema.$ref";
    private static final String REQUEST_BODY_KEY = "requestBody";
    private static final String ADDITIONAL_PROPERTIES_KEY = "additionalProperties";
    private static final String ADDITIONAL_PROPERTIES_FALSE = Boolean.FALSE.toString();

    private final ResourcePatternResolver resourcePatternResolver;

    OpenApiAdditionalPropertiesResolver(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    Set<Class<?>> requestBodyTypesDisallowingAdditionalProperties() {
        Set<Class<?>> strictRequestBodyTypes = new LinkedHashSet<>();

        for (Resource pathResource : pathResources()) {
            Properties pathProperties = yamlProperties(pathResource);
            for (String ref : requestBodySchemaRefs(pathProperties)) {
                Optional<String> schemaLocation = schemaClasspathLocation(ref);
                if (schemaLocation.isPresent()
                        && schemaDisallowsAdditionalProperties(schemaLocation.get())) {
                    strictRequestBodyTypes.add(generatedModelClass(schemaLocation.get()));
                }
            }
        }

        return Collections.unmodifiableSet(strictRequestBodyTypes);
    }

    private Resource[] pathResources() {
        try {
            return resourcePatternResolver.getResources(PATH_RESOURCE_PATTERN);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to resolve OpenAPI path resources for additionalProperties", ex);
        }
    }

    private Set<String> requestBodySchemaRefs(Properties pathProperties) {
        Set<String> refs = new LinkedHashSet<>();

        for (var entry : pathProperties.entrySet()) {
            String key = entry.getKey().toString();
            if (key.contains(REQUEST_BODY_KEY) && key.endsWith(REF_KEY_SUFFIX)) {
                refs.add(entry.getValue().toString());
            }
        }

        return refs;
    }

    private Optional<String> schemaClasspathLocation(String ref) {
        String refPath = ref.split("#", 2)[0];
        int schemaPathIndex = refPath.indexOf(COMPONENT_SCHEMAS_PATH);
        if (schemaPathIndex < 0) {
            return Optional.empty();
        }

        return Optional.of(OPENAPI_CLASSPATH_PREFIX + refPath.substring(schemaPathIndex));
    }

    private boolean schemaDisallowsAdditionalProperties(String schemaLocation) {
        Properties schemaProperties =
                yamlProperties(resourcePatternResolver.getResource(schemaLocation));

        return Objects.equals(
                ADDITIONAL_PROPERTIES_FALSE,
                String.valueOf(schemaProperties.get(ADDITIONAL_PROPERTIES_KEY)));
    }

    private Class<?> generatedModelClass(String schemaLocation) {
        String className = GENERATED_MODEL_PACKAGE + "." + schemaModelName(schemaLocation);

        try {
            return ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Unable to resolve generated OpenAPI model class " + className, ex);
        }
    }

    private String schemaModelName(String schemaLocation) {
        String filename = schemaLocation.substring(schemaLocation.lastIndexOf('/') + 1);
        String baseName = StringUtils.stripFilenameExtension(filename);
        StringBuilder modelName = new StringBuilder();

        for (String segment : baseName.split("[-_]+")) {
            if (!segment.isBlank()) {
                modelName.append(capitalise(segment));
            }
        }

        return modelName.toString();
    }

    private String capitalise(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private Properties yamlProperties(Resource resource) {
        YamlPropertiesFactoryBean yamlPropertiesFactory = new YamlPropertiesFactoryBean();
        yamlPropertiesFactory.setResources(resource);

        Properties properties = yamlPropertiesFactory.getObject();
        if (properties == null) {
            return new Properties();
        }

        return properties;
    }
}
