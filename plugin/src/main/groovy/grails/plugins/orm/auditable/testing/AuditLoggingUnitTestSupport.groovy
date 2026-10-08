/* Copyright 2026 the original author or authors.
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
package grails.plugins.orm.auditable.testing

import grails.core.GrailsApplication
import grails.plugins.orm.auditable.AuditLoggingConfigUtils
import grails.plugins.orm.auditable.ReflectionUtils
import groovy.transform.CompileStatic
import org.grails.testing.GrailsUnitTest
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo

import java.util.function.Supplier

/**
 * Points audit configuration at a unit test's application for the lifetime of its spec.
 *
 * Only loaded by {@link AuditLoggingUnitTestExtension} once it has found Grails testing support on the classpath.
 */
@CompileStatic
class AuditLoggingUnitTestSupport {

    private AuditLoggingUnitTestSupport() {
        // static only
    }

    static void register(SpecInfo spec) {
        spec.addSetupSpecInterceptor(new UseSpecApplication())
        spec.addCleanupSpecInterceptor(new ClearSpecApplication())
    }

    /**
     * Forget any application, supplier and cached audit configuration, so nothing leaks from one spec into the next
     * one that runs in the same JVM.
     */
    static void reset() {
        ReflectionUtils.applicationSupplier = null
        ReflectionUtils.application = null
        AuditLoggingConfigUtils.resetSecondaryConfigs()
        AuditLoggingConfigUtils.resetAuditConfig()
    }

    private static class UseSpecApplication implements IMethodInterceptor {
        @Override
        void intercept(IMethodInvocation invocation) throws Throwable {
            GrailsUnitTest test = (GrailsUnitTest) invocation.sharedInstance
            reset()
            // resolved on the first read of audit configuration, so the spec's application is not created early
            ReflectionUtils.applicationSupplier = { -> test.grailsApplication } as Supplier<GrailsApplication>
            invocation.proceed()
        }
    }

    private static class ClearSpecApplication implements IMethodInterceptor {
        @Override
        void intercept(IMethodInvocation invocation) throws Throwable {
            try {
                invocation.proceed()
            }
            finally {
                reset()
            }
        }
    }
}
