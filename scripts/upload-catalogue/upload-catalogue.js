/**
 * Upload un catalogue client vers Firestore (collection "catalogues"),
 * consomme par MenuViewModel.fetchCatalogue() / l'ecran Donnee.kt de l'app.
 *
 * Usage :
 *   npm install
 *   node upload-catalogue.js catalogues/test.json
 *
 * Pre-requis :
 *   - Un fichier de cle de compte de service Firebase, telecharge depuis
 *     Firebase Console -> Parametres du projet -> Comptes de service
 *     -> "Generer une nouvelle cle privee".
 *   - Chemin de ce fichier passe via la variable d'environnement
 *     GOOGLE_APPLICATION_CREDENTIALS, ou modifie SERVICE_ACCOUNT_PATH ci-dessous.
 *
 * Format du JSON d'entree (voir catalogues/test.json pour un exemple) :
 *   {
 *     "id": "test",              // -> id du document catalogues/{id}
 *     "label": "Test",
 *     "categories": [{ "key": "...", "name": "...", "description": "" }],
 *     "produits": [{
 *       "nom": "...", "prix": 1000, "categoryKey": "...", "stock": 20,
 *       "image": "aile.png",      // simple nom de fichier : URL reconstruite automatiquement
 *       // OU "image": "https://...png"  (URL deja complete, laissee telle quelle)
 *       "description": ""
 *     }],
 *     "vendeurs": [{ "nom": "...", "prenom": "..." }]
 *   }
 *
 * Les images sont supposees deja uploadees dans Firebase Storage sous
 * gs://<BUCKET>/catalogues/images/<nom-de-fichier> (dossier partage entre tous
 * les clients). Pour un client avec ses propres images, ajouter au JSON :
 *   "imageFolder": "catalogues/test"
 */

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const BUCKET = "caisse-e2af3.firebasestorage.app";
const SERVICE_ACCOUNT_PATH =
  process.env.GOOGLE_APPLICATION_CREDENTIALS ||
  path.join(__dirname, "service-account.json");

// Dossier Storage partage ou vivent toutes les images produit, quel que soit le client
// (gs://<BUCKET>/catalogues/images/<fichier>). Peut etre surcharge par catalogue via le
// champ JSON "imageFolder" (ex: "catalogues/test" si un client a ses propres images).
const DEFAULT_IMAGE_FOLDER = "catalogues/images";

function buildImageUrl(imageFolder, image) {
  if (!image) return null;
  // Deja une URI complete (http(s)://, ou android.resource:// pour un drawable
  // deja embarque dans l'app, ex. les anciens catalogues migres depuis SampleData*.kt).
  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(image)) return image;
  const storagePath = `${imageFolder}/${image}`;
  const encoded = encodeURIComponent(storagePath);
  return `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/${encoded}?alt=media`;
}

function loadCatalogue(jsonPath) {
  const raw = fs.readFileSync(jsonPath, "utf8");
  const data = JSON.parse(raw);

  if (!data.id) throw new Error("Le JSON doit avoir un champ 'id' (id du document catalogues/{id}).");
  if (!Array.isArray(data.categories)) throw new Error("'categories' doit etre un tableau.");
  if (!Array.isArray(data.produits)) throw new Error("'produits' doit etre un tableau.");

  const categoryKeys = new Set(data.categories.map((c) => c.key));
  for (const p of data.produits) {
    if (!categoryKeys.has(p.categoryKey)) {
      throw new Error(
        `Produit "${p.nom}" reference categoryKey="${p.categoryKey}" introuvable dans 'categories'.`
      );
    }
  }

  const imageFolder = data.imageFolder || DEFAULT_IMAGE_FOLDER;
  const produits = data.produits.map((p) => ({
    nom: p.nom,
    prix: Number(p.prix) || 0,
    categoryKey: p.categoryKey,
    stock: Number(p.stock) || 0,
    imageUrl: buildImageUrl(imageFolder, p.image),
    description: p.description || "",
  }));

  return {
    id: data.id,
    label: data.label || data.id,
    categories: data.categories.map((c) => ({
      key: c.key,
      name: c.name || c.key,
      description: c.description || "",
    })),
    produits,
    vendeurs: (data.vendeurs || []).map((v) => ({
      nom: v.nom || "",
      prenom: v.prenom || "",
    })),
  };
}

async function main() {
  const jsonArg = process.argv[2];
  if (!jsonArg) {
    console.error("Usage: node upload-catalogue.js <chemin-vers-catalogue.json>");
    process.exit(1);
  }

  if (!fs.existsSync(SERVICE_ACCOUNT_PATH)) {
    console.error(
      `Cle de compte de service introuvable : ${SERVICE_ACCOUNT_PATH}\n` +
        "Telecharge-la depuis Firebase Console -> Parametres du projet -> Comptes de service,\n" +
        "et place-la a cet emplacement (ou exporte GOOGLE_APPLICATION_CREDENTIALS)."
    );
    process.exit(1);
  }

  const jsonPath = path.resolve(jsonArg);
  const catalogue = loadCatalogue(jsonPath);

  admin.initializeApp({
    credential: admin.credential.cert(require(SERVICE_ACCOUNT_PATH)),
  });

  const db = admin.firestore();
  const { id, ...docData } = catalogue;

  await db.collection("catalogues").doc(id).set(docData);

  console.log(`Catalogue "${id}" (${docData.label}) uploade avec succes :`);
  console.log(`  - ${docData.categories.length} categories`);
  console.log(`  - ${docData.produits.length} produits`);
  console.log(`  - ${docData.vendeurs.length} vendeurs`);
}

main().catch((err) => {
  console.error("Echec de l'upload :", err.message);
  process.exit(1);
});
