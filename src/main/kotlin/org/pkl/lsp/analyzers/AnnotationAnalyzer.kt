/*
 * Copyright © 2024-2025 Apple Inc. and the Pkl project authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pkl.lsp.analyzers

import org.pkl.lsp.ErrorMessages
import org.pkl.lsp.Project
import org.pkl.lsp.ast.*
import org.pkl.lsp.type.computeThisType

class AnnotationAnalyzer(project: Project) : Analyzer(project) {
  override fun doAnalyze(node: PklNode, diagnosticsHolder: DiagnosticsHolder): Boolean {
    if (node !is PklAnnotation) return true
    val type = node.type ?: return true
    val context = node.containingFile.pklProject
    if (type !is PklDeclaredType) {
      diagnosticsHolder.addError(type, ErrorMessages.create("annotationHasNoName"))
      return true
    }

    val resolvedType = type.name.resolve(context)
    // unresolved type name is handled by TypeNameAnalyzer
    if (resolvedType == null) {
      return false
    }
    if (resolvedType !is PklClass) {
      diagnosticsHolder.addError(type, ErrorMessages.create("notAnnotation"))
      return false
    }
    if (resolvedType.isAbstract) {
      diagnosticsHolder.addError(type, ErrorMessages.create("typeIsAbstract"))
      return false
    }
    val base = project.pklBaseModule
    if (
      !resolvedType
        .computeThisType(base, mapOf(), context)
        .isSubtypeOf(base.annotationType, base, context)
    ) {
      diagnosticsHolder.addError(type, ErrorMessages.create("notAnnotation"))
    }

    return false
  }
}
