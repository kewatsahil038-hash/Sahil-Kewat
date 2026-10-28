package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.ActionStatus
import com.example.model.ActionType
import com.example.model.JarvisCommandResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class CloudCommand(
    val id: String = "",
    val userId: String = "",
    val rawCommand: String = "",
    val actionType: String = "",
    val target: String = "",
    val details: String = "",
    val status: String = "",
    val spokenResponseHindi: String = "",
    val spokenResponseEnglish: String = "",
    val aiModel: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class JarvisCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Observes command history for currently authenticated user.
     */
    fun observeCommandHistory(): Flow<List<CloudCommand>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return db.collection("users")
            .document(uid)
            .collection("commands")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        CloudCommand(
                            id = doc.id,
                            userId = doc.getString("userId").orEmpty(),
                            rawCommand = doc.getString("rawCommand").orEmpty(),
                            actionType = doc.getString("actionType").orEmpty(),
                            target = doc.getString("target").orEmpty(),
                            details = doc.getString("details").orEmpty(),
                            status = doc.getString("status").orEmpty(),
                            spokenResponseHindi = doc.getString("spokenResponseHindi").orEmpty(),
                            spokenResponseEnglish = doc.getString("spokenResponseEnglish").orEmpty(),
                            aiModel = doc.getString("aiModel").orEmpty(),
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing command doc", e)
                        null
                    }
                }
            }
            .catch { e ->
                Log.e(TAG, "Error observing command history", e)
                emit(emptyList())
            }
    }

    /**
     * Saves a command result to user's Cloud Firestore history.
     */
    suspend fun saveCommand(result: JarvisCommandResult, aiModel: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "rawCommand" to result.rawCommand,
                "actionType" to result.actionType.name,
                "target" to result.target,
                "details" to result.details,
                "status" to result.status.name,
                "spokenResponseHindi" to result.spokenResponseHindi,
                "spokenResponseEnglish" to result.spokenResponseEnglish,
                "aiModel" to aiModel,
                "timestamp" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users")
                .document(uid)
                .collection("commands")
                .add(payload)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save command to Cloud Firestore", e)
            false
        }
    }

    /**
     * Updates an existing command status (e.g. from PENDING_CONFIRMATION to EXECUTED).
     */
    suspend fun updateCommandStatus(commandId: String, status: ActionStatus): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("users")
                .document(uid)
                .collection("commands")
                .document(commandId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update command status", e)
            false
        }
    }

    companion object {
        private const val TAG = "JarvisCloudRepo"
    }
}
