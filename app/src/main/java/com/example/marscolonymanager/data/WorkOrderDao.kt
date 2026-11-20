package com.example.marscolonymanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface WorkOrderDao {
    @Query("SELECT * FROM work_orders ORDER BY id DESC")
    fun observeOrders(): Flow<List<WorkOrderEntity>>

    @Transaction
    @Query("SELECT * FROM work_orders WHERE id = :id")
    fun observeOrder(id: Long): Flow<WorkOrderWithHistory?>

    @Query("SELECT * FROM work_orders WHERE id = :id")
    suspend fun findOrder(id: Long): WorkOrderEntity?

    @Insert
    suspend fun insertOrder(order: WorkOrderEntity): Long

    @Insert
    suspend fun insertEvent(event: WorkOrderEventEntity)

    @Update
    suspend fun updateOrder(order: WorkOrderEntity)

    @Query("DELETE FROM work_orders WHERE id = :id")
    suspend fun deleteOrder(id: Long): Int
}