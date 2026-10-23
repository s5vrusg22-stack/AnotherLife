package com.anotherlife.app.engine

import androidx.room.withTransaction
import com.anotherlife.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Converts accepted social invitations into bounded AI work; never auto-generates on clock ticks. */
class SocialDialogueEngine(private val db: AnotherLifeDatabase, private val dialogue: NpcDialogueEngine) {
    private val dao = db.socialDialogueDao()
    private val gate = Mutex()

    suspend fun runNext(worldId: String): String? = gate.withLock {
        val job = db.withTransaction {
            val pending = dao.next(worldId) ?: return@withTransaction null
            if (dao.claim(worldId, pending.id) == 1) pending else null
        } ?: return@withLock null
        try {
            val result = dialogue.converse(worldId, job.senderId, job.recipientId, job.topic, turns = 4)
            dao.complete(worldId, job.id, result.conversationId)
            result.conversationId
        } catch (cancelled: CancellationException) {
            dao.fail(worldId, job.id, "cancelled")
            throw cancelled
        } catch (error: Exception) {
            dao.fail(worldId, job.id, error.message?.take(200) ?: "inference failed")
            throw error
        }
    }

    suspend fun skip(worldId: String, jobId: String) = dao.skip(worldId, jobId)
}
