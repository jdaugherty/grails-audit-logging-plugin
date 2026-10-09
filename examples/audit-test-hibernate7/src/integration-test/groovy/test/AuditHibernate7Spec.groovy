package test

import grails.plugins.orm.auditable.AuditLogContext
import grails.testing.mixin.integration.Integration
import org.springframework.transaction.TransactionStatus
import spock.lang.Specification

@Integration
class AuditHibernate7Spec extends Specification {

    void setup() {
        Author.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                Author.where {}.deleteAll()
            }
            AuditTrail.where {}.deleteAll()
        }
    }

    void "insert, update and delete are audited when the transaction commits"() {
        when:
        Long id = Author.withTransaction {
            new Author(name: 'Aaron', age: 37).save(flush: true, failOnError: true).id
        }
        Author.withTransaction {
            Author.get(id).age = 38
        }
        Author.withTransaction {
            Author.get(id).delete(flush: true)
        }

        then:
        auditRows() == [
            ['DELETE', 'age', '38', null],
            ['DELETE', 'name', 'Aaron', null],
            ['INSERT', 'age', null, '37'],
            ['INSERT', 'name', null, 'Aaron'],
            ['UPDATE', 'age', '37', '38'],
        ]
    }

    void "a rolled back transaction is not audited and does not stop auditing in the same session"() {
        when:
        Author.withNewSession {
            Author.withTransaction { TransactionStatus transactionStatus ->
                new Author(name: 'Rolled back', age: 1).save(flush: true, failOnError: true)
                transactionStatus.setRollbackOnly()
            }
            // The session reuses its Hibernate transaction
            Author.withTransaction {
                new Author(name: 'Committed', age: 2).save(flush: true, failOnError: true)
            }
        }

        then:
        auditRows() == [
            ['INSERT', 'age', null, '2'],
            ['INSERT', 'name', null, 'Committed'],
        ]
    }

    private static List<List<String>> auditRows() {
        AuditTrail.withNewTransaction {
            AuditTrail.list()
                .collect { [it.eventName, it.propertyName, it.oldValue, it.newValue] }
                .sort { it[0] + it[1] }
        }
    }
}
