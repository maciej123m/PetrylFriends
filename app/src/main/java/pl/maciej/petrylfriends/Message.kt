package pl.maciej.petrylfriends

import com.google.firebase.database.Exclude

class Message(val author : String, val message : String, val tokenID : String ,val time: Long) {
    @set:Exclude
    @get:Exclude
    var key : String? = null
    constructor() : this("","","",0)
}