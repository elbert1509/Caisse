package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.AppTable
import com.example.caisse.data.Produit
import com.example.caisse.data.TableItem
import java.util.UUID

@Dao
interface TableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTable(appTable: AppTable)

    @Update
    suspend fun updateTable(appTable: AppTable)

    @Query("SELECT * FROM app_table WHERE id = :id")
    suspend fun getTableById(id: UUID): AppTable?

    @Query("SELECT * FROM app_table")
    suspend fun getAllTablesOnce(): List<AppTable>

    @Query("SELECT * FROM app_table WHERE active = 1")
    suspend fun getActiveTables(): List<AppTable>

    @Insert
    suspend fun addProductToTable(tableItem: TableItem)

    @Update
    suspend fun updateProductInTable(tableItem: TableItem)

    @Query("DELETE FROM table_item WHERE tableId = :tableId AND productId = :productId")
    suspend fun deleteProductFromTable(tableId: UUID, productId: UUID)

    @Query("SELECT * FROM table_item WHERE tableId = :tableId")
    suspend fun getTableItems(tableId: UUID): List<TableItem>

    @Query("SELECT * FROM table_item WHERE id = :id")
    suspend fun getTableItemById(id: UUID): TableItem?

    @Query("SELECT * FROM table_item")
    suspend fun getAllTableItemsOnce(): List<TableItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTableItem(tableItem: TableItem)


}