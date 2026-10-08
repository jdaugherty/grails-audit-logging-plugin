package test

import grails.plugins.orm.auditable.ReflectionUtils
import grails.testing.gorm.DataTest
import spock.lang.Specification

class AuditableUnitTestSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [Author, Book, Publisher]
    }

    @Override
    Closure doWithConfig() {
        { config ->
            config.grails.plugin.auditLog.excluded = ['age', 'version', 'lastUpdated', 'lastUpdatedBy']
        }
    }

    void "an auditable domain reads audit configuration from the spec's application"() {
        when:
        Collection<String> names = new Author(name: 'Aaron', age: 50).auditablePropertyNames

        then:
        names.contains('name')
        !names.contains('age')
        ReflectionUtils.application.is(grailsApplication)
    }
}
