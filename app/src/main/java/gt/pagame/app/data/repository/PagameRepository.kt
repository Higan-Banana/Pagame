package gt.pagame.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import com.google.firebase.Timestamp
import kotlin.random.Random
import com.google.firebase.firestore.ListenerRegistration
import gt.pagame.app.data.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
fun userMessage(error: Throwable): String = when (error) {
    is PagameValidationException -> error.message ?: "Revisa los datos de la operación."
    is FirebaseFirestoreException -> when (error.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "No se pudo autorizar el cambio. Revisa los permisos o las reglas Spark publicadas en Firebase."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "No hay conexión con Firebase. Intenta nuevamente."
        FirebaseFirestoreException.Code.ABORTED -> "Los datos cambiaron al mismo tiempo. Intenta nuevamente."
        else -> "No se pudo guardar el cambio. Intenta nuevamente."
    }
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado."
        "ERROR_INVALID_EMAIL" -> "Ingresa un correo válido."
        "ERROR_WEAK_PASSWORD" -> "Usa una contraseña de al menos 6 caracteres."
        "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera un momento."
        "ERROR_REQUIRES_RECENT_LOGIN" -> "Cierra sesión y vuelve a ingresar para eliminar tu cuenta."
        "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada."
        else -> "No se pudo iniciar sesión. Revisa el correo y la contraseña."
    }
    else -> "No se pudo completar la operación. Revisa tu conexión."
}
class PagameRepository private constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _registering = MutableStateFlow(false)
    val registering = _registering.asStateFlow()
    private val _sessionUid = MutableStateFlow<String?>(auth.currentUser?.uid)
    val sessionUid = _sessionUid.asStateFlow()
    private val _currentUser = MutableStateFlow(UserProfile())
    val currentUser = _currentUser.asStateFlow()
    private val _people = MutableStateFlow<Map<String, FriendItem>>(emptyMap())
    val people = _people.asStateFlow()
    private val _events = MutableStateFlow<List<EventItem>>(emptyList())
    val events = _events.asStateFlow()
    private val _onlineEvents = MutableStateFlow<List<OnlineEvent>>(emptyList())
    val onlineEvents = _onlineEvents.asStateFlow()
    private val _friends = MutableStateFlow<List<FriendItem>>(emptyList())
    val friends = _friends.asStateFlow()
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests = _friendRequests.asStateFlow()
    private val _currency = MutableStateFlow(Currency.GTQ)
    val currency = _currency.asStateFlow()
    private val _theme = MutableStateFlow(AppThemeMode.SYSTEM)
    val theme = _theme.asStateFlow()
    private val _profileChecked = MutableStateFlow(false)
    val profileChecked = _profileChecked.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val deletedProfiles = mutableSetOf<String>()
    private var generation = 0L
    private var friendIds = emptyList<String>()
    private val listeners = mutableListOf<ListenerRegistration>()
    private val profileListeners = mutableMapOf<String, ListenerRegistration>()
    init { auth.addAuthStateListener { bind(it.currentUser?.uid) } }
    private fun bind(uid: String?) {
        val binding = ++generation
        listeners.forEach { it.remove() }; listeners.clear()
        profileListeners.values.forEach { it.remove() }; profileListeners.clear()
        deletedProfiles.clear()
        _people.value = emptyMap(); _friends.value = emptyList(); friendIds = emptyList()
        _friendRequests.value = emptyList()
        _onlineEvents.value = emptyList(); _events.value = emptyList()
        _currentUser.value = UserProfile(); _profileChecked.value = false
        _error.value = null; _loading.value = uid != null; _sessionUid.value = uid
        _currency.value = Currency.GTQ; _theme.value = AppThemeMode.SYSTEM
        if (uid == null) return
        listeners += db.collection("profiles").document(uid).addSnapshotListener { snapshot, error ->
            if (_sessionUid.value != uid || binding != generation) return@addSnapshotListener
            if (error != null) { _error.value = userMessage(error); return@addSnapshotListener }
            _profileChecked.value = true
            if (snapshot?.exists() == true) {
                val friend = person(snapshot.data.orEmpty(), uid)
                _currentUser.value = UserProfile(uid, friend.name, auth.currentUser?.email.orEmpty(),
                    friend.initial, friend.codeOrStatus, auth.currentUser?.isEmailVerified == true, friend.colorIndex)
                _people.update { it + (uid to friend) }; rebuild()
            } else { _currentUser.value = UserProfile(); _loading.value = false }
        }
        listeners += db.collection("users").document(uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (_sessionUid.value != uid || binding != generation) return@addSnapshotListener
            if (error != null) { _error.value = userMessage(error); return@addSnapshotListener }
            if (snapshot?.metadata?.hasPendingWrites() == true) return@addSnapshotListener
            val data = snapshot?.data.orEmpty()
            friendIds = strings(data["friendIds"])
            _currency.value = Currency.entries.firstOrNull { it.name == data["currency"] } ?: Currency.GTQ
            _theme.value = AppThemeMode.entries.firstOrNull { it.name == data["theme"] } ?: AppThemeMode.SYSTEM
            watchPeople(uid); rebuild()
        }
        listeners += requestsFor(uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (_sessionUid.value != uid || binding != generation) return@addSnapshotListener
            if (error != null) { _error.value = userMessage(error); return@addSnapshotListener }
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                acceptRequests(uid, snapshot.documents)
            }
        }
        listeners += db.collection("events").whereArrayContains("participantIds", uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (_sessionUid.value != uid || binding != generation) return@addSnapshotListener
            _loading.value = false
            if (error != null) { _error.value = userMessage(error); return@addSnapshotListener }
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                acceptEvents(uid, snapshot.documents)
            }
        }
    }
    fun retry() = bind(auth.currentUser?.uid)

    private fun acceptEvents(uid: String, documents: List<DocumentSnapshot>) {
        val current = _onlineEvents.value.associateBy { it.id }
        _onlineEvents.value = documents.mapNotNull { document ->
            if (document.get("createdAt") == null) return@mapNotNull current[document.id]
            runCatching { event(document.id, document.data.orEmpty()) }
                .onFailure { _error.value = userMessage(it) }.getOrNull()?.let { incoming ->
                    current[incoming.id]?.takeIf { it.revision > incoming.revision } ?: incoming
                }
        }.sortedWith(compareByDescending<OnlineEvent> { it.createdAt }.thenByDescending { it.id })
        watchPeople(uid)
        rebuild()
    }

    private fun requestsFor(uid: String) = db.collection("friendRequests").whereArrayContains("members", uid)
    private fun requestRef(sender: String, recipient: String) =
        db.collection("friendRequests").document("$sender~$recipient")

    private fun acceptRequests(uid: String, documents: List<DocumentSnapshot>) {
        _friendRequests.value = documents.mapNotNull { doc ->
            val sender = doc.getString("senderId") ?: return@mapNotNull null
            val recipient = doc.getString("recipientId") ?: return@mapNotNull null
            val status = doc.getString("status") ?: return@mapNotNull null
            FriendRequest(doc.id, sender, recipient, status, doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L)
        }.sortedByDescending { it.createdAt }
        watchPeople(uid)
        rebuild()
    }

    // Refresh actual screen state without clearing lists or recreating the session.
    suspend fun refresh() = withTimeout(15_000L) {
        val uid = requireUser()
        val binding = generation
        coroutineScope {
            val requestsRequest = async { requestsFor(uid).get(Source.SERVER).awaitResult() }
            val meRequest = async { profile(uid).get(Source.SERVER).awaitResult() }
            val settingsRequest = async { settings(uid).get(Source.SERVER).awaitResult() }
            val eventsRequest = async {
                db.collection("events").whereArrayContains("participantIds", uid).get(Source.SERVER).awaitResult()
            }
            val me = meRequest.await()
            val preferences = settingsRequest.await()
            val events = eventsRequest.await()
            val requests = requestsRequest.await()
            if (binding != generation || _sessionUid.value != uid) return@coroutineScope
            if (me.exists()) {
                val friend = person(me.data.orEmpty(), uid)
                _currentUser.value = UserProfile(uid, friend.name, auth.currentUser?.email.orEmpty(),
                    friend.initial, friend.codeOrStatus, auth.currentUser?.isEmailVerified == true, friend.colorIndex)
                _people.update { it + (uid to friend) }
            }
            friendIds = strings(preferences.get("friendIds"))
            _currency.value = Currency.entries.firstOrNull { it.name == preferences.getString("currency") } ?: Currency.GTQ
            _theme.value = AppThemeMode.entries.firstOrNull { it.name == preferences.getString("theme") } ?: AppThemeMode.SYSTEM
            acceptRequests(uid, requests.documents)
            acceptEvents(uid, events.documents)
            val wanted = (friendIds + _friendRequests.value.map { it.otherId(uid) } + _onlineEvents.value.flatMap { it.participantIds + it.expenses.map { expense -> expense.createdBy } }).toSet() - uid
            wanted.chunked(10).forEach { ids ->
                val people = ids.map { id -> async { id to profile(id).get(Source.SERVER).awaitResult() } }.awaitAll()
                if (binding != generation || _sessionUid.value != uid) return@coroutineScope
                people.forEach { (id, doc) ->
                    if (doc.getBoolean("deleted") == true) deletedProfiles.add(id) else deletedProfiles.remove(id)
                    _people.update { if (doc.exists()) it + (id to person(doc.data.orEmpty(), id)) else it - id }
                }
            }
            rebuild()
        }
    }

    private fun watchPeople(uid: String) {
        val binding = generation
        val wanted = (friendIds + _friendRequests.value.map { it.otherId(uid) } + _onlineEvents.value.flatMap { it.participantIds + it.expenses.map { expense -> expense.createdBy } }).toSet() - uid
        (profileListeners.keys - wanted).forEach { id ->
            profileListeners.remove(id)?.remove(); _people.update { it - id }
        }
        (wanted - profileListeners.keys).forEach { id ->
            profileListeners[id] = db.collection("profiles").document(id).addSnapshotListener { snap, error ->
                if (_sessionUid.value != uid || binding != generation) return@addSnapshotListener
                if (error != null) { _error.value = userMessage(error); return@addSnapshotListener }
                if (snap?.getBoolean("deleted") == true) deletedProfiles.add(id) else deletedProfiles.remove(id)
                _people.update { if (snap?.exists() == true) it + (id to person(snap.data.orEmpty(), id)) else it - id }
                rebuild()
            }
        }
    }
    private fun rebuild() {
        val uid = _sessionUid.value.orEmpty()
        val sharedEventCounts = mutableMapOf<String, Int>()
        _onlineEvents.value.forEach { event ->
            event.participantIds.forEach { id -> sharedEventCounts[id] = (sharedEventCounts[id] ?: 0) + 1 }
        }
        _friends.value = friendIds.filter { it !in deletedProfiles }.mapNotNull { _people.value[it] }.map { friend ->
            friend.copy(eventsTogetherCount = sharedEventCounts[friend.id] ?: 0)
        }.sortedBy { it.name }
        _events.value = _onlineEvents.value.map { event ->
            val balance = event.balances()[uid] ?: 0L
            EventItem(event.id, event.title, dateLabel(event.createdAt), event.participantIds.size,
                event.totalMinor / 100.0,
                when { balance > 0 -> BalanceStatus.YOU_ARE_OWED; balance < 0 -> BalanceStatus.YOU_OWE
                    else -> BalanceStatus.ALL_SETTLED },
                kotlin.math.abs(balance) / 100.0,
                event.participantIds.map { _people.value[it]?.initial ?: "?" },
                currency = event.currency, participantIds = event.participantIds,
                createdByName = displayName(event.ownerId))
        }
    }
    fun displayName(uid: String): String {
        if (uid == _sessionUid.value) return "Tú"
        val friend = _people.value[uid] ?: return "Usuario"
        return if (friend.name == "Tú" || _people.value.values.count { it.name == friend.name } > 1) "${friend.name} (${friend.codeOrStatus})" else friend.name
    }
    suspend fun login(email: String, password: String) { auth.signInWithEmailAndPassword(email.trim(), password).awaitResult() }
    suspend fun register(name: String, email: String, password: String) {
        applicationScope.async {
            _registering.value = true
            try {
                val uid = auth.createUserWithEmailAndPassword(email.trim(), password).awaitResult().user!!.uid
                initProfile(name)
                // Keep the registration screen until the profile listener is ready for navigation.
                withTimeout(30_000L) { currentUser.first { it.id == uid } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (auth.currentUser != null) _error.value = userMessage(e)
                throw e
            } finally { _registering.value = false }
        }.await()
    }
    private fun requireUser(): String = auth.currentUser?.uid ?: throw PagameValidationException("Inicia sesión para continuar.")
    private fun profile(uid: String) = db.collection("profiles").document(uid)
    private fun settings(uid: String) = db.collection("users").document(uid)
    private fun ensure(ok: Boolean, message: String) { if (!ok) throw PagameValidationException(message) }

    suspend fun initProfile(name: String) {
        val uid = requireUser()
        val cleanName = name.trim()
        ensure(cleanName.isNotBlank() && cleanName.length <= 60, "Ingresa tu nombre (máximo 60 caracteres).")
        repeat(100) {
            val code = "PG-${Random.nextInt(1000, 10000)}"
            val codeRef = db.collection("friendCodes").document(code)
            val created = db.runTransaction { tx ->
                val existing = tx.get(profile(uid))
                if (existing.exists()) {
                    ensure(existing.getBoolean("deleted") != true, "Esta cuenta está en proceso de eliminación.")
                    true
                } else if (tx.get(codeRef).exists()) false
                else {
                    tx.set(profile(uid), mapOf("uid" to uid, "name" to cleanName, "friendCode" to code,
                        "colorIndex" to ((uid.hashCode().ushr(1)) % 8), "createdAt" to FieldValue.serverTimestamp(), "deleted" to false))
                    tx.set(settings(uid), mapOf("currency" to "GTQ", "theme" to "SYSTEM", "friendIds" to emptyList<String>()))
                    tx.set(codeRef, mapOf("uid" to uid))
                    true
                }
            }.awaitResult()
            if (created) return
        }
        throw PagameValidationException("No se pudo asignar un código disponible. Intenta nuevamente.")
    }
    suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email.trim()).awaitResult() }
    fun signOut() { auth.signOut() }
    suspend fun deleteAccount() {
        val user = auth.currentUser ?: return
        val token = user.getIdToken(false).awaitResult()
        val authTime = (token.claims["auth_time"] as? Number)?.toLong() ?: 0
        ensure(System.currentTimeMillis() / 1000 - authTime < 300,
            "Por seguridad, cierra sesión y vuelve a ingresar antes de eliminar tu cuenta.")
        val events = db.collection("events").whereArrayContains("participantIds", user.uid).limit(1).get(Source.SERVER).awaitResult()
        ensure(events.isEmpty, "Primero debes salir de tus eventos o eliminarlos si eres organizador.")
        // A minimal tombstone preserves identity if another device was inviting this UID concurrently.
        db.runTransaction { tx ->
            val current = tx.get(profile(user.uid))
            val privateData = tx.get(settings(user.uid))
            val code = current.getString("friendCode")
            val codeRef = code?.let { db.collection("friendCodes").document(it) }
            val codeData = codeRef?.let { tx.get(it) }
            if (current.exists()) tx.update(profile(user.uid), mapOf("name" to "Usuario eliminado", "colorIndex" to 0, "deleted" to true))
            if (privateData.exists()) tx.delete(settings(user.uid))
            if (codeRef != null && codeData?.getString("uid") == user.uid) tx.delete(codeRef)
            null
        }.awaitResult()
        user.delete().awaitResult()
        auth.signOut()
    }
    suspend fun updateName(name: String) {
        val uid = requireUser()
        val cleanName = name.trim()
        ensure(cleanName.isNotBlank() && cleanName.length <= 60, "Ingresa un nombre de hasta 60 caracteres.")
        db.runTransaction { tx ->
            val current = tx.get(profile(uid))
            ensure(current.exists() && current.getBoolean("deleted") != true, "Tu perfil no está disponible.")
            tx.update(profile(uid), "name", cleanName)
            null
        }.awaitResult()
    }

    suspend fun updateProfile(values: Map<String, Any>) {
        val uid = requireUser()
        val batch = db.batch()
        values["colorIndex"]?.let { batch.update(profile(uid), "colorIndex", it) }
        val preferences = values.filterKeys { it == "currency" || it == "theme" }
        if (preferences.isNotEmpty()) batch.update(settings(uid), preferences)
        batch.commit().awaitResult()
    }
    suspend fun lookupFriend(code: String): FriendItem? {
        val uid = requireUser()
        if (!FriendCode.isComplete(code)) return null
        val entry = db.collection("friendCodes").document(code).get(Source.SERVER).awaitResult()
        val friendId = entry.getString("uid") ?: return null
        if (friendId == uid) return null
        val found = profile(friendId).get(Source.SERVER).awaitResult()
        return if (found.exists() && found.getBoolean("deleted") != true) person(found.data.orEmpty(), friendId) else null
    }
    // No friendship is created before the recipient explicitly accepts.
    suspend fun addFriend(friendId: String): String {
        val uid = requireUser()
        ensure(friendId != uid, "No puedes agregarte a ti mismo.")
        return db.runTransaction { tx ->
            val friend = tx.get(profile(friendId))
            val me = tx.get(settings(uid))
            val sent = tx.get(requestRef(uid, friendId))
            val received = tx.get(requestRef(friendId, uid))
            ensure(friend.exists() && friend.getBoolean("deleted") != true, "Este usuario ya no está disponible.")
            val existing = strings(me.get("friendIds"))
            when {
                sent.getString("status") == "accepted" || received.getString("status") == "accepted" -> "Ya son amigos."
                received.getString("status") == "pending" -> "Esta persona ya te envió una solicitud. Acéptala en Solicitudes."
                sent.getString("status") == "pending" -> "La solicitud ya está enviada. Espera su aceptación."
                else -> {
                    ensure(friendId in existing || existing.size < 300, "Has alcanzado el límite de amigos.")
                    tx.set(requestRef(uid, friendId), mapOf(
                        "senderId" to uid, "recipientId" to friendId, "members" to listOf(uid, friendId),
                        "status" to "pending", "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()))
                    "Solicitud enviada. Aparecerá en tus amigos cuando la acepte."
                }
            }
        }.awaitResult()
    }

    suspend fun acceptFriendRequest(requestId: String) {
        val uid = requireUser()
        val ref = db.collection("friendRequests").document(requestId)
        db.runTransaction { tx ->
            val request = tx.get(ref)
            ensure(request.exists() && request.getString("recipientId") == uid,
                "Esta solicitud ya no está disponible.")
            val sender = request.getString("senderId") ?: throw PagameValidationException("Solicitud no válida.")
            val senderProfile = tx.get(profile(sender))
            val me = tx.get(settings(uid))
            ensure(senderProfile.exists() && senderProfile.getBoolean("deleted") != true,
                "Esta cuenta ya no está disponible. Puedes rechazar la solicitud.")
            if (request.getString("status") != "accepted") {
                ensure(request.getString("status") == "pending", "Esta solicitud ya no está pendiente.")
                val existing = strings(me.get("friendIds"))
                ensure(sender in existing || existing.size < 300, "Has alcanzado el límite de amigos.")
                // arrayUnion preserves concurrent additions without reading the other user's private settings.
                tx.update(settings(uid), "friendIds", FieldValue.arrayUnion(sender))
                tx.update(settings(sender), "friendIds", FieldValue.arrayUnion(uid))
                tx.update(ref, mapOf("status" to "accepted", "updatedAt" to FieldValue.serverTimestamp()))
            }
            null
        }.awaitResult()
    }

    suspend fun dismissFriendRequest(requestId: String) {
        val uid = requireUser()
        val ref = db.collection("friendRequests").document(requestId)
        db.runTransaction { tx ->
            val request = tx.get(ref)
            if (request.exists()) {
                ensure(uid in strings(request.get("members")), "Esta solicitud no te pertenece.")
                ensure(request.getString("status") == "pending", "Esta solicitud ya fue aceptada.")
                tx.delete(ref)
            }
            null
        }.awaitResult()
    }
    suspend fun removeFriend(friendId: String) {
        val uid = requireUser()
        ensure(friendId != uid, "No puedes eliminarte a ti mismo.")
        db.runTransaction { tx ->
            val me = tx.get(settings(uid))
            val otherProfile = tx.get(profile(friendId))
            val sent = tx.get(requestRef(uid, friendId))
            val received = tx.get(requestRef(friendId, uid))
            if (friendId in strings(me.get("friendIds"))) {
                tx.update(settings(uid), "friendIds", FieldValue.arrayRemove(friendId))
                if (otherProfile.exists() && otherProfile.getBoolean("deleted") != true) {
                    tx.update(settings(friendId), "friendIds", FieldValue.arrayRemove(uid))
                }
                if (sent.exists()) tx.delete(sent.reference)
                if (received.exists()) tx.delete(received.reference)
            }
            null
        }.awaitResult()
    }
    suspend fun createEvent(id: String, title: String, participantIds: List<String>) {
        val uid = requireUser()
        val ref = db.collection("events").document(id)
        db.runTransaction { tx ->
            val old = tx.get(ref)
            if (old.exists()) {
                ensure(old.getString("ownerId") == uid, "Este identificador ya está utilizado.")
            } else {
                val me = tx.get(settings(uid))
                val people = SparkEvents.ids(participantIds + uid)
                ensure(people.all { it == uid || it in strings(me.get("friendIds")) }, "Agrega primero a estas personas a tus amigos.")
                people.forEach { personId ->
                    val person = tx.get(profile(personId))
                    ensure(person.exists() && person.getBoolean("deleted") != true, "Uno de los participantes ya no está disponible.")
                }
                val data = SparkEvents.create(uid, title, people, me.getString("currency") ?: "GTQ", System.currentTimeMillis())
                writeEvent(tx, id, data, creating = true)
            }
            null
        }.awaitResult()
    }
    suspend fun command(action: String, values: Map<String, Any> = emptyMap()): Map<String, Any?> {
        val uid = requireUser()
        val id = values["eventId"] as? String ?: throw PagameValidationException("Falta el evento.")
        val ref = db.collection("events").document(id)
        val binding = generation
        val committed = db.runTransaction { tx ->
            val snap = tx.get(ref)
            ensure(snap.exists(), "Este evento ya no está disponible.")
            val original = normalizeEvent(snap.data.orEmpty())
            if (action == "updateParticipants") {
                val me = tx.get(settings(uid))
                val before = strings(original["participantIds"])
                val after = SparkEvents.ids(values["participantIds"])
                ensure(after.all { it in before || it in strings(me.get("friendIds")) }, "Agrega primero a estas personas a tus amigos.")
                (after - before.toSet()).forEach { personId ->
                    val person = tx.get(profile(personId))
                    ensure(person.exists() && person.getBoolean("deleted") != true, "Uno de los participantes ya no está disponible.")
                }
            }
            val next = SparkEvents.mutate(original, uid, action, values, System.currentTimeMillis())
            if (next == null) {
                tx.delete(ref)
                repeat(10) { tx.delete(ref.collection("ledger").document(it.toString())) }
            } else if (next != original) writeEvent(tx, id, next)
            next
        }.awaitResult()
        // Only publish after the server accepted the transaction; never show a failed payment as saved.
        if (binding == generation && _sessionUid.value == uid) {
            if (committed == null) _onlineEvents.update { list -> list.filterNot { it.id == id } }
            else {
                val incoming = event(id, committed)
                _onlineEvents.update { list ->
                    val current = list.firstOrNull { it.id == id }
                    if (current != null && current.revision > incoming.revision) list
                    else (list.filterNot { it.id == id } + incoming)
                        .sortedWith(compareByDescending<OnlineEvent> { it.createdAt }.thenByDescending { it.id })
                }
            }
            watchPeople(uid)
            rebuild()
        }
        return emptyMap()
    }
    private fun writeEvent(tx: Transaction, id: String, data: Map<String, Any?>, creating: Boolean = false) {
        val ref = db.collection("events").document(id)
        val stored = data.toMutableMap()
        stored["createdAt"] = if (creating) FieldValue.serverTimestamp() else Timestamp(java.util.Date(number(data["createdAt"])))
        stored["updatedAt"] = FieldValue.serverTimestamp()
        if (creating) tx.set(ref, stored) else tx.update(ref, stored.filterKeys { it != "createdAt" })
        SparkEvents.ledgers(data).forEachIndexed { index, ledger -> tx.set(ref.collection("ledger").document(index.toString()), ledger) }
    }
    private fun normalizeEvent(data: Map<String, Any?>) = data + mapOf(
        "createdAt" to number(data["createdAt"]), "updatedAt" to number(data["updatedAt"]))
    private fun person(data: Map<String, Any?>, id: String) = FriendItem(
        id, data["name"] as? String ?: "", initials(data["name"] as? String ?: ""),
        codeOrStatus = data["friendCode"] as? String ?: "", colorIndex = (data["colorIndex"] as? Number)?.toInt() ?: 0
    )
    private fun event(id: String, data: Map<String, Any?>): OnlineEvent = SparkEvents.decode(id, normalizeEvent(data))
    companion object {
        val instance by lazy { PagameRepository() }
        private fun number(value: Any?) = when (value) { is Timestamp -> value.toDate().time; is Number -> value.toLong(); else -> 0L }
        private fun strings(value: Any?) = (value as? List<*>)?.filterIsInstance<String>().orEmpty()
        fun dateLabel(time: Long): String = java.text.SimpleDateFormat("dd/MM/yyyy • HH:mm", java.util.Locale.getDefault()).format(java.util.Date(time))
    }
}
