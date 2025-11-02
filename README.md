# 💼 Caisse — Application Android de gestion de ventes

**Caisse** est une application Android complète de **gestion de point de vente (POS)** développée en **Kotlin** avec **Jetpack Compose** et **Room**.  
Elle permet d’enregistrer des ventes, gérer le stock, produire des rapports journaliers/hebdomadaires/mensuels, et exporter des rapports PDF détaillés.

---

## 🚀 Fonctionnalités principales

### 🛒 Vente & Facturation
- Création et validation de ventes depuis un panier.
- Gestion des **tables** pour les ventes sur place (mode restaurant).
- Génération automatique de **factures (Invoice)** à la validation.
- Impression ou partage des rapports au format **PDF**.

### 📦 Gestion de stock
- Mise à jour automatique du stock à chaque vente.
- Affichage du **stock restant** dans les rapports de produits.
- Alerte visuelle pour les produits en faible quantité.

### 📊 Tableaux de bord & Rapports
- Écran **Dashboard** avec trois **KPI** (chiffres d’affaires du jour, de la semaine, du mois).
- Écran **Rapports** avec 3 onglets :
  - **Journalier** : ventes et produits du jour.
  - **Hebdomadaire** : résumé des 7 derniers jours.
  - **Mensuel** : synthèse du mois en cours.
- Export des rapports PDF directement depuis le bas de l’écran (“Bottom_rapport”).

### 🧾 Génération de PDF
- Exporte automatiquement le rapport sélectionné (journalier, hebdo, mensuel).
- Format A4 clair et lisible avec :
  - Nom du produit,
  - Quantité vendue,
  - Stock restant,
  - Chiffre d’affaires total.
- Partage natif Android (WhatsApp, Gmail, Drive, etc.).

---

## 🏗️ Architecture technique

- **Langage** : Kotlin  
- **UI** : Jetpack Compose  
- **Base de données** : Room (SQLite)  
- **Injection de dépendances** : ViewModel + Repository pattern  
- **Export PDF** : `PdfDocument` + `FileProvider`  
- **Navigation** : `NavHost` / `NavController`

---

## 📂 Structure principale

