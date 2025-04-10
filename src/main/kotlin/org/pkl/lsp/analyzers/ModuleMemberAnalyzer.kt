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
import org.pkl.lsp.ast.PklMethod
import org.pkl.lsp.ast.PklModule
import org.pkl.lsp.ast.PklNode
import org.pkl.lsp.ast.PklProperty

class ModuleMemberAnalyzer(project: Project) : Analyzer(project) {

  override fun doAnalyze(node: PklNode, diagnosticsHolder: DiagnosticsHolder): Boolean {
    val context = node.containingFile.pklProject
    when (node) {
      is PklProperty -> {
        val isAmends = node.enclosingModule?.isAmend == true
        val supermodule = node.enclosingModule?.supermodule(context)?.cache(context)

        if (isAmends && !node.isLocal && supermodule != null) {
          val superProperty = supermodule.properties[node.name]
          if (superProperty == null) {
            diagnosticsHolder.addWarning(
              node.identifier ?: node,
              ErrorMessages.create("unresolvedProperty", node.name),
            )
          }
        }
      }
      is PklMethod -> {
        val isAmends = node.enclosingModule?.isAmend ?: false

        if (isAmends && !node.isLocal) {
          diagnosticsHolder.addError(
            node.methodHeader.identifier ?: node,
            ErrorMessages.create("missingModifierLocal"),
          )
        }
      }
    }

    return node is PklModule
  }
}
