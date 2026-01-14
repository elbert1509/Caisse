package com.example.piece.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.piece.data.Invoice
import com.example.piece.data.InvoiceItem
import java.util.UUID

@Dao
interface InvoiceDao {
    @Insert
    suspend fun addInvoice(invoice: Invoice)

    @Insert
    suspend fun addInvoiceItem(invoiceItem: InvoiceItem)

    @Query("SELECT * FROM invoice ORDER BY date DESC")
    suspend fun getAllInvoices(): List<Invoice>

    @Query("SELECT * FROM invoice_item WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: UUID): List<InvoiceItem>

    @Query("SELECT * FROM invoice WHERE id = :id")
    suspend fun getInvoiceById(id: UUID): Invoice?

    @Query("SELECT * FROM invoice")
    suspend fun getAllInvoicesOnce(): List<Invoice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoice_item WHERE id = :id")
    suspend fun getInvoiceItemById(id: UUID): InvoiceItem?

    @Query("SELECT * FROM invoice_item")
    suspend fun getAllInvoiceItemsOnce(): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateInvoiceItem(invoiceItem: InvoiceItem)
}