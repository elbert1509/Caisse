package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.AppTable
import com.example.caisse.data.Produit
import com.example.caisse.data.TableItem
import java.util.UUID

@Dao
interface TableDao {
    // ---------- TABLES ----------
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertTable(appTable: AppTable)

    @Update
    suspend fun updateTable(appTable: AppTable)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTable(appTable: AppTable)

    @Query("SELECT * FROM app_table WHERE active = 1")
    suspend fun getActiveTables(): List<AppTable>

    @Query("SELECT * FROM app_table")
    suspend fun getAllTablesOnce(): List<AppTable>

    @Query("SELECT * FROM app_table WHERE id = :id")
    suspend fun getTableById(id: UUID): AppTable?

    @Query("SELECT * FROM app_table WHERE name = :name")
    suspend fun getTableByName(name: String): AppTable?


    // ---------- TABLE ITEMS ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTableItem(tableItem: TableItem)

    @Update
    suspend fun updateProductInTable(tableItem: TableItem)

    @Insert
    suspend fun addProductToTable(tableItem: TableItem)  // garde si utilisé

    @Query("DELETE FROM table_item WHERE tableId = :tableId AND productId = :productId")
    suspend fun deleteProductFromTable(tableId: UUID, productId: UUID)

    @Query("SELECT * FROM table_item WHERE tableId = :tableId")
    suspend fun getTableItems(tableId: UUID): List<TableItem>

    @Query("SELECT * FROM table_item")
    suspend fun getAllTableItemsOnce(): List<TableItem>
    @Query("DELETE FROM table_item WHERE tableId = :tableId")
    suspend fun clearTableItems(tableId: UUID)

}