package com.example.caisse

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.caisse.data.Produit
import com.example.caisse.data.ShopInfos
import com.example.caisse.util.PdfReportGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PdfEtatStockTest {

    private val cat = UUID.randomUUID()

    private fun produit(nom: String, stock: Int) =
        Produit(nom = nom, prix = 1000.0, categoryId = cat, stock = stock)

    @Test
    fun generateEtatStock_produitUnPdfValide() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val infos = ShopInfos(
            1, "Boutique Test", "Adresse", "0102030405", "test@test.fr",
            siret = "123", logo = null,
            passwordHash = "h", passwordSalt = "s", devise = "FCFA"
        )
        // 60 produits pour forcer la pagination sur plusieurs pages
        val produits = (1..60).map { produit("Produit $it", stock = it % 15) }

        val file = PdfReportGenerator.generateEtatStock(context.cacheDir, infos, produits)

        assertTrue("le fichier doit exister", file.exists())
        assertTrue("le fichier ne doit pas être vide", file.length() > 500)
        val headerBytes = ByteArray(4)
        file.inputStream().use { it.read(headerBytes) }
        assertTrue("doit être un PDF (en-tête %PDF)", String(headerBytes, Charsets.US_ASCII) == "%PDF")
        // Copie inspectable via adb run-as (le cacheDir est nettoyé par le test)
        file.copyTo(java.io.File(context.filesDir, "etat_stock_test.pdf"), overwrite = true)
        file.delete()
    }

    @Test
    fun generateEtatStock_listeVide_neCrashePas() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = PdfReportGenerator.generateEtatStock(context.cacheDir, null, emptyList())
        assertTrue(file.exists() && file.length() > 0)
        file.delete()
    }
}
