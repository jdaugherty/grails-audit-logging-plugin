package test

import grails.plugins.orm.auditable.ReflectionUtils
import spock.lang.Specification
import spock.util.EmbeddedSpecRunner

/**
 * Not a Grails unit test itself, so the extension leaves it alone and it can observe the state a unit test leaves behind.
 */
class AuditLoggingUnitTestExtensionSpec extends Specification {

    void "a unit test's application is only used while its spec runs"() {
        when:
        def results = new EmbeddedSpecRunner().runWithImports('''
            import grails.testing.gorm.DataTest
            import test.Author
            import test.Book
            import test.Publisher

            class UsesAuditableDomain extends Specification implements DataTest {
                Class[] getDomainClassesToMock() { [Author, Book, Publisher] }

                def "reads audit configuration"() {
                    expect:
                    new Author(name: 'Aaron', age: 50).auditablePropertyNames.contains('name')
                }
            }
        ''')

        then:
        results.testsSucceededCount == 1
        results.failureCount == 0
        ReflectionUtils.application == null
        ReflectionUtils.applicationSupplier == null
    }

    void "reading audit configuration outside a unit test still fails clearly"() {
        when:
        ReflectionUtils.applicationConfig

        then:
        IllegalStateException e = thrown()
        e.message.contains('initialization must complete')
    }
}
