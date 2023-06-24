package com.example.bookappkotlin.models

class ModelComment {
//    variable  should be with same spellings and types as added in firebase
    var id =""
    var bookId =""
    var timestamp =""
    var comment =""
    var uid =""
//empty constructor , required by firebase
    constructor()

    // param contrustor
    constructor(id: String, bookId: String, timestamp: String, comment: String, uid: String) {
        this.id = id
        this.bookId = bookId
        this.timestamp = timestamp
        this.comment = comment
        this.uid = uid
    }
}