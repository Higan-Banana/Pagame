package gt.pagame.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dateOrSubtitle: String,
    val categoryIcon: String = "🍕",
    val isToday: Boolean = false
)
