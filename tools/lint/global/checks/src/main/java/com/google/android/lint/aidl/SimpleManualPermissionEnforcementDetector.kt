/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.lint.aidl

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UMethod

/**
 * Looks for methods implementing generated AIDL interface stubs
 * that can have simple permission checks migrated to
 * @EnforcePermission annotations
 */
@Suppress("UnstableApiUsage")
class SimpleManualPermissionEnforcementDetector : AidlImplementationDetector() {
    override fun visitAidlMethod(
            context: JavaContext,
            node: UMethod,
            interfaceName: String,
            body: UBlockExpression
    ) {
        val enforcePermissionFix = EnforcePermissionFix.fromBlockExpression(context, body) ?: return
        val lintFix = enforcePermissionFix.toLintFix(context, node)
        val message =
                "$interfaceName permission check ${
                    if (enforcePermissionFix.errorLevel) "should" else "can"
                } be converted to @EnforcePermission annotation"

        val incident = Incident(
                ISSUE_SIMPLE_MANUAL_PERMISSION_ENFORCEMENT,
                enforcePermissionFix.manualCheckLocations.last(),
                message,
                lintFix
        )

        // TODO(b/265014041): turn on errors once all code that would cause one is fixed
        // if (enforcePermissionFix.errorLevel) {
        //     incident.overrideSeverity(Severity.ERROR)
        // }

        context.report(incident)
    }

    companion object {

        private val EXPLANATION = """
            Whenever possible, method implementations of AIDL interfaces should use the @EnforcePermission
            annotation to declare the permissions to be enforced.  The verification code is then
            generated by the AIDL compiler, which also takes care of annotating the generated java
            code.

            This reduces the risk of bugs around these permission checks (that often become vulnerabilities).
            It also enables easier auditing and review.

            Please migrate to an @EnforcePermission annotation. (See: go/aidl-enforce-howto)
        """.trimIndent()

        @JvmField
        val ISSUE_SIMPLE_MANUAL_PERMISSION_ENFORCEMENT = Issue.create(
                id = "SimpleManualPermissionEnforcement",
                briefDescription = "Manual permission check can be @EnforcePermission annotation",
                explanation = EXPLANATION,
                category = Category.SECURITY,
                priority = 5,
                severity = Severity.WARNING,
                implementation = Implementation(
                        SimpleManualPermissionEnforcementDetector::class.java,
                        Scope.JAVA_FILE_SCOPE
                ),
        )
    }
}