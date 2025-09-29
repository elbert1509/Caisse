package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.caisse.data.Invoice
import com.example.caisse.data.InvoiceItem
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
}