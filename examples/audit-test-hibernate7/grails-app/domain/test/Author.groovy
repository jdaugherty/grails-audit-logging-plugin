package test

import grails.plugins.orm.auditable.Auditable

class Author implements Auditable {
    String name
    Long age

    static constraints = {
        name nullable: false
        age nullable: false
    }
}
