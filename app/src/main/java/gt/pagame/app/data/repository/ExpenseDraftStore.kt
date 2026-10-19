package gt.pagame.app.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ExpenseDraft(
    val id: String, val title: String, val amount: String,
    val payerId: String, val includedIds: List<String>, val expiresAt: Long
)

/** Private to this device; drafts are never published as expenses or shared with other users. */
class ExpenseDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("expense_drafts", Context.MODE_PRIVATE)
    private fun key(uid: String, eventId: String) = "$uid~$eventId"

    init {
        val now = System.currentTimeMillis()
        val expired = preferences.all.filterValues {
            runCatching { JSONObject(it as String).getLong("expiresAt") <= now }.getOrDefault(true)
        }.keys
        if (expired.isNotEmpty()) preferences.edit().also { editor -> expired.forEach { editor.remove(it) } }.apply()
    }

    fun read(uid: String, eventId: String): ExpenseDraft? {
        val raw = preferences.getString(key(uid, eventId), null) ?: return null
        val draft = runCatching {
            val json = JSONObject(raw)
            require(json.optString("uid", uid) == uid && json.optString("eventId", eventId) == eventId)
            val ids = json.getJSONArray("includedIds")
            ExpenseDraft(json.getString("id"), json.getString("title"), json.getString("amount"),
                json.getString("payerId"), List(ids.length()) { ids.getString(it) },
                json.getLong("expiresAt"))
        }.getOrNull()
        if (draft == null || draft.expiresAt <= System.currentTimeMillis()) {
            clear(uid, eventId)
            return null
        }
        return draft
    }

    fun save(uid: String, eventId: String, draft: ExpenseDraft) {
        if (uid.isBlank()) return
        val json = JSONObject().put("id", draft.id).put("title", draft.title).put("amount", draft.amount)
            .put("uid", uid).put("eventId", eventId).put("payerId", draft.payerId)
            .put("includedIds", JSONArray(draft.includedIds)).put("expiresAt", draft.expiresAt)
        preferences.edit().putString(key(uid, eventId), json.toString()).apply()
    }

    fun clear(uid: String, eventId: String) { preferences.edit().remove(key(uid, eventId)).apply() }

    companion object { const val LIFETIME_MS = 30 * 60 * 1000L }
}
