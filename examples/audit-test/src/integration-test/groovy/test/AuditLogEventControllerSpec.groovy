package test

import grails.plugins.orm.auditable.AuditLogContext
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Value
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Integration
class AuditLogEventControllerSpec extends Specification {

    @Value('${local.server.port}')
    Integer port

    void setup() {
        Author.withNewTransaction {
            AuditLogContext.withoutAuditLog {
                Book.where {}.deleteAll()
                Author.where {}.deleteAll()
            }
        }
        AuditTrail.withNewTransaction {
            AuditTrail.where {}.deleteAll()
        }
    }

    void "the plugin's controller lists the configured audit domain class"() {
        given:
        Author.withNewTransaction {
            new Author(name: 'Aaron', age: 37, famous: true).save(flush: true, failOnError: true)
        }

        when:
        HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:${port}/auditLogEvent/list")).build(),
            HttpResponse.BodyHandlers.ofString()
        )

        then:
        response.statusCode() == 200
        response.body().contains('AuditLogEvent List')
        response.body().contains('test.Author')
    }
}
