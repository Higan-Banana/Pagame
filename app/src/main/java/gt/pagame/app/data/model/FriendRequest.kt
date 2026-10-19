package gt.pagame.app.data.model

data class FriendRequest(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val status: String,
    val createdAt: Long
) {
    fun otherId(uid: String): String = if (senderId == uid) recipientId else senderId
}
