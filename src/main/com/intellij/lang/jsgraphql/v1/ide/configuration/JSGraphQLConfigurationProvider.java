/*
 *  Copyright (c) 2015-present, Jim Kynde Meyer
 *  All rights reserved.
 *
 *  This source code is licensed under the MIT license found in the
 *  LICENSE file in the root directory of this source tree.
 */
package com.intellij.lang.jsgraphql.v1.ide.configuration;

import com.google.common.collect.Lists;
import com.intellij.lang.jsgraphql.endpoint.psi.*;
import com.intellij.lang.jsgraphql.ide.project.graphqlconfig.GraphQLConfigManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class JSGraphQLConfigurationProvider {

    private final Project myProject;
    private final GraphQLConfigManager graphQLConfigManager;

    public JSGraphQLConfigurationProvider(@NotNull Project myProject) {
        this.myProject = myProject;
        graphQLConfigManager = GraphQLConfigManager.getService(myProject);
    }

    public static JSGraphQLConfigurationProvider getService(@NotNull Project project) {
        return ServiceManager.getService(project, JSGraphQLConfigurationProvider.class);
    }

    public VirtualFile getEndpointEntryFile(PsiFile psiFile) {
        return getEndpointEntryFile(psiFile.getVirtualFile());
    }

    public VirtualFile getEndpointEntryFile(VirtualFile virtualFile) {

        final Ref<VirtualFile> configBaseDir = Ref.create();
        final JSGraphQLSchemaEndpointConfiguration configuration = graphQLConfigManager.getEndpointLanguageConfiguration(virtualFile, configBaseDir);
        if (configuration != null) {
            final String entryRelativeFileName = configuration.entry;
            if (StringUtil.isNotEmpty(entryRelativeFileName) && configBaseDir.get() != null) {
                final VirtualFile entryFile = configBaseDir.get().findFileByRelativePath(entryRelativeFileName);
                if (entryFile != null) {
                    return entryFile;
                }
            }
        }
        return null;
    }

    public List<JSGraphQLSchemaEndpointAnnotation> getEndpointAnnotations(PsiFile psiFile) {
        final List<JSGraphQLSchemaEndpointAnnotation> annotations = Lists.newArrayList();

        final JSGraphQLSchemaEndpointConfiguration endpointLanguageConfiguration = graphQLConfigManager.getEndpointLanguageConfiguration(psiFile.getVirtualFile(), null);
        if (endpointLanguageConfiguration != null && endpointLanguageConfiguration.annotations != null) {
            annotations.addAll(endpointLanguageConfiguration.annotations);
        }

        // also include in-language annotations (could eventually remove the need for the config file annotations)
        final Collection<JSGraphQLEndpointTypeResult<JSGraphQLEndpointAnnotationDefinition>> languageAnnotations = JSGraphQLEndpointPsiUtil.getKnownDefinitionNames(
                psiFile,
                JSGraphQLEndpointAnnotationDefinition.class,
                false
        );
        for (JSGraphQLEndpointTypeResult<JSGraphQLEndpointAnnotationDefinition> languageAnnotation : languageAnnotations) {
            final JSGraphQLSchemaEndpointAnnotation annotationConfig = new JSGraphQLSchemaEndpointAnnotation();
            annotationConfig.name = languageAnnotation.name;
            final JSGraphQLEndpointArgumentsDefinition argumentsDefinition = languageAnnotation.element.getArgumentsDefinition();
            if (argumentsDefinition != null && argumentsDefinition.getInputValueDefinitions() != null) {
                for (JSGraphQLEndpointInputValueDefinition argument : argumentsDefinition.getInputValueDefinitions().getInputValueDefinitionList()) {
                    final JSGraphQLSchemaEndpointAnnotationArgument argumentConfig = new JSGraphQLSchemaEndpointAnnotationArgument();
                    argumentConfig.name = argument.getInputValueDefinitionIdentifier().getText();
                    if (argument.getCompositeType() != null) {
                        argumentConfig.type = argument.getCompositeType().getText();
                    }
                    annotationConfig.arguments.add(argumentConfig);
                }
            }
            annotations.add(annotationConfig);
        }

        return annotations;
    }

}
