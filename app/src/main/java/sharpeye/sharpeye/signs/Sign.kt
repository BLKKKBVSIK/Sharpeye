package sharpeye.sharpeye.signs

class Sign(_name : String, _id : Int, _kind: SignKind, _speed : Int, _additionalInfo: String) {

    var name : String = ""
    var id : Int = 0
    var kind : SignKind
    var speed : Int
    var additionalInfos : String

    init {
        this.name = _name
        this.id = _id
        this.kind = _kind
        this.speed = _speed
        this.additionalInfos = _additionalInfo
    }


}