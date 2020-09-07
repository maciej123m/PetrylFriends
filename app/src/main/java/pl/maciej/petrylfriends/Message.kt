package pl.maciej.petrylfriends

class Message(val author : String, val message : String, val tokenID : String ,val time: Long) {
    constructor() : this("","","",0)
}