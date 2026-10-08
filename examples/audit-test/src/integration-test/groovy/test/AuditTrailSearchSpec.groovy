/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package test

import grails.plugins.orm.auditable.AuditLogContext
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

@Integration
class AuditTrailSearchSpec extends Specification {

    void setup() {
        AuditTrail.withNewTransaction {
            AuditTrail.executeUpdate('delete from AuditTrail')
        }
        AuditLogContext.withoutAuditLog {
            Author.withNewTransaction {
                Review.where {}.deleteAll()
                Book.where {}.deleteAll()
                Author.where {}.deleteAll()
                Publisher.where {}.deleteAll()
            }
        }
        Author.withNewTransaction {
            new Author(name: 'Aaron', age: 37, famous: true).save(flush: true, failOnError: true)
            new Author(name: 'Elmar', age: 42, famous: false).save(flush: true, failOnError: true)
        }
    }

    void "a blank query returns every audit trail"() {
        when:
        Map<String, Number> counts = AuditTrail.withTransaction {
            [all: AuditTrail.count(), found: AuditTrail.search(query, null).count()]
        }

        then:
        counts.all > 0
        counts.found == counts.all

        where:
        query << [null, '', '   ']
    }

    void "each query term must match one of the text columns"() {
        when:
        Map<String, List<AuditTrail>> results = AuditTrail.withTransaction {
            [
                aaron    : AuditTrail.search('aaron', null).list(),
                aaronName: AuditTrail.search('name aaron', null).list(),
                noMatch  : AuditTrail.search('aaron nomatch', null).list()
            ]
        }

        then: "matching is case insensitive"
        results.aaron
        results.aaron.every { it.newValue == 'Aaron' }

        and: "terms are combined with and"
        results.aaronName*.propertyName == ['name']
        results.aaronName*.newValue == ['Aaron']

        and: "a term that matches nothing excludes everything"
        results.noMatch.empty
    }

    void "createdAfter only returns audit trails created after the date"() {
        given:
        Date tomorrow = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)

        when:
        Map<String, Number> counts = AuditTrail.withTransaction {
            [
                all           : AuditTrail.count(),
                sinceEpoch    : AuditTrail.search(null, new Date(0)).count(),
                afterTomorrow : AuditTrail.search(null, tomorrow).count(),
                aaronTomorrow : AuditTrail.search('aaron', tomorrow).count()
            ]
        }

        then:
        counts.all > 0
        counts.sinceEpoch == counts.all
        counts.afterTomorrow == 0
        counts.aaronTomorrow == 0
    }
}
