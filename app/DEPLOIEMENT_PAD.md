# Déploiement Caisse sur un nouveau PadPOS (mode Kiosk)

Procédure complète pour configurer un nouveau pad : installation, passage en **mode kiosk Device Owner**, et **allègement du système** pour la fluidité.

- **App** : `com.example.caisse`
- **Admin component** : `com.example.caisse/.kiosk.KioskAdminReceiver`
- **Android cible** : 7.0+ (terminaux Android 7 en 4G/3G, pads MediaTek 8.1). `minSdk = 23` dans `app/build.gradle.kts` (`java.time.*` est couvert par le desugaring). Le blocage réseau (VPN permanent, section 2 bis) nécessite **Android 7.0 (API 24)** minimum.

---

## 0. Prérequis

- PC avec **ADB** installé (`adb devices` doit lister le pad).
- Sur le pad : **Options développeur** activées + **Débogage USB** activé.
- ⚠️ **AUCUN compte Google** configuré sur le pad (Paramètres → Comptes). Sinon `set-device-owner` échoue avec *« not allowed... there are already some accounts »*. Si besoin, supprimer les comptes avant.
- L'APK doit être **signé avec le keystore de production** (toujours le même keystore pour pouvoir mettre à jour ensuite).

---

## 1. Installer l'application

```bash
adb install -r app-release.apk
```

Vérifier que le récepteur d'admin est bien présent :

```bash
adb shell dumpsys package com.example.caisse | grep KioskAdminReceiver
```

---

## 2. Passer en mode Kiosk (Device Owner)

```bash
adb shell dpm set-device-owner com.example.caisse/.kiosk.KioskAdminReceiver
```

Réponse attendue :
```
Success: Device owner set to package com.example.caisse
```

Vérifier :
```bash
adb shell dumpsys device_policy | grep -A2 "Device Owner"
```

Au lancement de l'app :
- L'écran se **verrouille** sur l'app (mode LOCKED, sans toast « can't be unpinned »).
- L'app devient le **launcher HOME** persistant → au reboot, retour direct dans l'app.

> Si le toast « This app can't be unpinned » apparaît, c'est que l'app est en mode PINNED (pas LOCKED). Vérifier que le Device Owner est bien posé (étape ci-dessus).

---

## 2 bis. Blocage réseau : seule la caisse consomme des données

### Activer / désactiver depuis Firestore

Le blocage se pilote à distance, comme `paused` pour les mises à jour : **il est désactivé tant que le champ n'est pas mis à `true`**. Le pad écoute Firestore en temps réel (`NetworkBlockController`) : le changement s'applique en quelques secondes, sans redémarrage ni nouvelle version.

| Champ (booléen) | Où | Effet |
|---|---|---|
| `vpnBlocking` | `devices/{ANDROID_ID}` (prioritaire) ou `config/app_android` (tous les pads) | `true` = seule la caisse a le réseau. `false` ou absent = pas de blocage. |
| `vpnLockdown` | idem | Mode lockdown du VPN. Absent = constante `KioskManager.VPN_LOCKDOWN` (`false`). |

Exemples :
- Tester sur un seul pad : `devices/{ANDROID_ID}` → `vpnBlocking = true`.
- Généraliser : `config/app_android` → `vpnBlocking = true`. Un pad peut s'en exclure avec `vpnBlocking = false` dans son `devices/{id}`.
- Couper d'urgence : repasser à `false`. La caisse étant exclue du VPN, elle relit toujours Firestore et lève le blocage.

L'état appliqué est affiché dans l'écran de maintenance (« Blocage réseau : actif / inactif »). Sans réseau au démarrage, le pad garde le dernier état posé (le système le mémorise).

> ⚠️ Même piège que `targetVersionCode` : nom de champ exact, sans espace, et de type **booléen** (pas la chaîne `"true"`).

### Politiques posées

Au lancement en Device Owner, `KioskManager.applyKioskPolicies()` pose toujours les verrous ci-dessous ; le VPN, lui, suit `vpnBlocking` :

| Politique | Effet |
|---|---|
| VPN permanent `BlockingVpnService` (`setAlwaysOnVpnPackage`, API 24+), **si `vpnBlocking = true`** | VPN local « trou noir » : tout le trafic des autres applis (Play Services, Play Store, synchros système…) est capté et jeté. La caisse est exclue du VPN et garde le réseau normal. Vaut pour la 4G/3G **et** le WiFi. |
| `DISALLOW_CONFIG_VPN` | Impossible de retirer ou changer le VPN dans les Réglages. |
| `DISALLOW_CONFIG_TETHERING` | Pas de partage de connexion. |
| `DISALLOW_CONFIG_MOBILE_NETWORKS` | Réglages réseau mobile (APN, données) verrouillés, **y compris en maintenance**. Configurer l'APN **avant** `set-device-owner`. |
| Play Store masqué (`setApplicationHidden`) | `com.android.vending` invisible et inactif. |
| Mises à jour système reportées (`createPostponeInstallPolicy`) | Pas d'OTA constructeur automatique (report de 30 jours renouvelé par le système). |

**Mode lockdown** : champ Firestore `vpnLockdown`, sinon constante `KioskManager.VPN_LOCKDOWN` (`false` par défaut).
- `false` : si le VPN s'arrête, les autres applis retrouvent le réseau jusqu'à son redémarrage automatique.
- `true` : le système bloque tout trafic hors VPN, même VPN arrêté. Sur Android 7, l'appli propriétaire du VPN permanent est normalement exemptée du blocage, **à vérifier sur terminal** avant de passer à `true` en production (voir tests ci-dessous). Grâce au champ `vpnLockdown`, ce test se fait sur un pad sans recompiler.

> ⚠️ Conséquences : WhatsApp, Gmail ou toute autre appli n'a plus de réseau. Le partage d'un rapport vers WhatsApp/e-mail ouvre l'appli mais l'envoi restera en attente. La synchro de l'heure par Internet (NTP, faite par le système) est aussi bloquée : garder **« Date/heure automatique » fournie par le réseau mobile** (NITZ) et vérifier l'heure du pad (horodatage NF525).

### Vérifier sur le terminal

```bash
# 1. Le VPN permanent est posé et actif
adb shell dumpsys connectivity | grep -i -E "VPN|tun0"
#    -> un réseau de type VPN (interface tun0) doit apparaître, avec "Caisse - blocage réseau"
adb shell settings get secure always_on_vpn_app        # -> com.example.caisse
adb shell settings get secure always_on_vpn_lockdown   # -> 0 (ou 1 si VPN_LOCKDOWN = true)
adb shell ip addr show tun0                             # -> 10.0.0.2/32
adb logcat -d -s BlockingVpnService KioskManager        # -> "VPN trou noir actif."

# 2. La caisse a toujours le réseau
#    Dans l'app : maintenance -> « Vérifier les mises à jour » doit répondre (Firestore joint),
#    ou importer un catalogue depuis « Données ». Côté système :
adb shell pm list packages -U com.example.caisse         # -> uid:10xxx
adb shell dumpsys netstats detail | grep -A4 "uid=10xxx"  # -> rxBytes/txBytes en hausse sur l'interface mobile (ccmni*/rmnet*)

# 3. Une autre appli n'a plus de réseau
#    Le shell adb (uid 2000) passe lui aussi par le VPN : un ping/DNS doit échouer.
adb shell ping -c 3 8.8.8.8                             # -> 100% packet loss
adb shell ping -c 3 google.com                          # -> unknown host
#    Navigateur (sortir en maintenance d'abord, sinon le LockTask l'empêche) :
adb shell am start -a android.intent.action.VIEW -d https://www.google.com
#    -> page en erreur (ERR_NAME_NOT_RESOLVED / pas de connexion)
```

> Tester d'abord avec `vpnLockdown = false`, puis refaire les 3 vérifications avec `vpnLockdown = true` dans `devices/{id}` et un redémarrage du pad : si la caisse perd le réseau en lockdown, remettre `false` (la caisse ne pouvant alors plus lire Firestore, retirer le VPN par ADB : voir « Désinstallation »).

---

## 3. Sortie admin / Maintenance

Depuis l'app :
1. **Appui long sur le titre « Tableau de bord »**.
2. Saisir le **code PIN admin** (défini dans `KioskManager.adminPin`).
3. Le kiosk se désactive → accès aux boutons **WiFi**, **Bluetooth**, **Vérifier les mises à jour**, et l'**ID du pad**.
4. **« Reprendre le kiosk »** pour re-verrouiller.

Déblocage de secours par ADB (en cas de souci) :
```bash
adb shell am force-stop com.example.caisse
# Et si nécessaire, retirer le device owner :
adb shell dpm remove-active-admin com.example.caisse/.kiosk.KioskAdminReceiver
```

---

## 4. Alléger le système (fluidité)

Désactiver les apps inutiles (réversible via `pm enable`). **Ne pas désactiver** : `com.google.android.gms` (Firebase), le clavier, les Réglages, le Bluetooth, l'imprimante, la téléphonie/SIM.

```bash
adb shell 'for p in \
  com.android.chrome com.android.calculator2 com.android.calendar \
  com.android.deskclock com.android.music com.android.musicfx \
  com.android.soundrecorder com.android.gallery3d com.android.egg \
  com.android.protips com.android.dreams.basic com.android.wallpaperpicker \
  com.android.wallpapercropper com.android.bookmarkprovider com.android.calllogbackup \
  com.google.android.apps.pdfviewer com.google.android.inputmethod.pinyin \
  com.svox.pico com.baidu.map.location \
  com.mediatek.camera com.mediatek.emcamera com.mediatek.callrecorder \
  com.mediatek.factorymode com.mediatek.filemanager com.mediatek.calendarimporter \
  com.mediatek.duraspeed com.mediatek.ygps com.mediatek.lbs.em2.ui \
  com.android.vending com.google.android.configupdater com.google.android.ims \
  com.mediatek.engineermode com.android.mms; do pm disable-user --user 0 $p; done'
```

Réduire les animations (interface plus réactive) :
```bash
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
```

Puis redémarrer :
```bash
adb reboot
```

### ⚠️ À NE JAMAIS désactiver
| Paquet | Raison |
|---|---|
| `com.google.android.gms` / `com.google.android.gsf` | Firebase (mises à jour) |
| `com.android.inputmethod.latin` | Clavier (saisie PIN / login) |
| `com.android.settings` | WiFi/Bluetooth en maintenance |
| `com.android.bluetooth` | Imprimante |
| `com.android.packageinstaller` | Installation des mises à jour |
| `com.android.managedprovisioning` | Device Owner / kiosk |
| `com.zj.printer.demo3`, `com.beanpod.*`, `cn.test.serial` | Services imprimante / matériel POS |
| Téléphonie : `com.android.phone`, `com.android.providers.telephony`, `com.android.carrierconfig`, `com.android.stk` | Data mobile / SIM |

Réactiver un paquet :
```bash
adb shell pm enable <paquet>
```

---

## 5. Mises à jour à distance (OTA silencieux)

L'app se met à jour seule via Firestore + Firebase Storage (privilège Device Owner, aucune interaction).

### Publier une nouvelle version
1. Incrémenter `versionCode` **et** `versionName` dans `app/build.gradle.kts`.
2. Générer l'APK signé (**même keystore**).
3. Uploader l'APK sur **Firebase Storage**.
4. Mettre à jour le document Firestore (voir ci-dessous).
5. Sur le pad : maintenance → **Vérifier les mises à jour**.

### Structure Firestore

**Global (tous les pads)** — `config/app_android` :
| Champ | Type | Exemple |
|---|---|---|
| `versionCode` | number | `4` |
| `versionName` | string | `"1.8"` |
| `apkUrl` | string | `gs://caisse-e2af3.firebasestorage.app/app-release.apk` |

**Ciblage d'un pad** — `devices/{ANDROID_ID}` (prioritaire sur le global) :
| Champ | Type | Effet |
|---|---|---|
| `targetVersionCode` | number | Version pour ce pad uniquement |
| `versionName` | string | ex. `"1.8"` |
| `apkUrl` | string | URL `gs://` de l'APK signé |
| `paused` | boolean | `true` = aucune mise à jour sur ce pad |

> L'`ANDROID_ID` du pad est affiché dans l'écran de maintenance (ou via `adb shell settings get secure android_id`).

> ⚠️ **Piège vécu** : pas d'**espace** ni de caractère parasite dans les noms de champs (`targetVersionCode`, pas `" targetVersionCode"`), sinon l'app lit `null` et n'installe rien.

> ⚠️ L'`apkUrl` doit être une **URI Firebase Storage `gs://`** (ou une URL HTTPS directe). **Pas** de lien Google Drive `/view` (renvoie du HTML, pas l'APK).

> La mise à jour ne se déclenche que si `targetVersionCode > versionCode installé` (pas de downgrade automatique). Même signature obligatoire.

> ⚠️ **Piège vécu** : `INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match`. Arrive si le pad a une build **debug** installée (signée avec le keystore debug auto-généré par Android Studio) et qu'on pousse une build **release** (keystore de prod) — Android refuse toute mise à jour dont le certificat diffère. Toujours vérifier que le pad tourne une build release signée avec le même keystore que le pipeline OTA avant de chercher un bug côté code. Le vrai résultat de l'installation (succès/échec/erreur précise) s'affiche maintenant dans l'écran de maintenance après « Vérifier les mises à jour » (avant, l'UI restait bloquée sur « Installation en cours… » même en cas d'échec).

### Règles de sécurité

**Firestore** (Console → Firestore → Règles) :
```
match /config/{doc}     { allow read: if true; allow write: if false; }
match /devices/{doc}    { allow read: if true; allow write: if false; }   // aussi vpnBlocking / vpnLockdown
match /catalogues/{doc} { allow read: if true; allow write: if false; }
```

**Storage** (Console → Storage → Règles) :
```
match /app-release.apk         { allow read: if true; allow write: if false; }
match /catalogues/{allPaths=**} { allow read: if true; allow write: if false; }
```

> Ces deux chemins doivent rester en lecture publique (`if true`) : l'app n'est jamais authentifiée via Firebase Auth sur l'écran de maintenance ou l'écran admin « Données » (le déverrouillage se fait par PIN local). Une règle `if request.auth != null` sur `app-release.apk` ou `catalogues/**` casse silencieusement l'OTA ou l'import de catalogue (pas d'erreur visible : le téléchargement échoue, ou l'image ne s'affiche juste pas).

---

## 6. Catalogues clients (import dynamique, sans recompilation)

Depuis le 2026-09-27, ajouter/modifier le catalogue (catégories, produits, vendeurs) d'un client ne nécessite **plus** de recompiler ni redéployer l'app — y compris sur des pads déjà en production.

- **Firestore** : un document par client dans `catalogues/{clientId}` (`label`, `categories[]`, `produits[]`, `vendeurs[]`). Consommé par `MenuViewModel.fetchCatalogue()` / `listCataloguesClients()`.
- **App** : écran admin « Données » (PIN requis, `menuViewModel.isAdminMode`) → section « Nouveau client (Firebase) » → liste déroulante (rafraîchissable) des clients Firestore → « Importer ce client ».
- **Outil d'upload** : `scripts/upload-catalogue/` (Node.js + `firebase-admin`, hors build Android). Nécessite une clé de compte de service Firebase (`scripts/upload-catalogue/service-account.json`, **jamais commitée**) et `npm install` une première fois.
  ```bash
  cd scripts/upload-catalogue
  node upload-catalogue.js catalogues/<client>.json
  ```
- **Images produit** : deux cas.
  - **Image déjà embarquée dans l'app** (ancien produit migré depuis un `SampleData*.kt`) : `"image": "android.resource://com.example.caisse/drawable/<nom>"` — fonctionne uniquement si le drawable existe déjà dans l'APK installé (nécessite recompilation pour en ajouter un nouveau).
  - **Image distante (recommandé pour tout nouveau produit)** : uploader le fichier dans Firebase Storage sous `catalogues/images/<fichier>` (dossier partagé entre tous les clients), puis dans le JSON juste `"image": "<fichier>"` — le script reconstruit automatiquement l'URL de téléchargement. Coil (chargement d'image dans l'app) lit cette URL `https://` à l'exécution : **aucune recompilation ni redéploiement requis**, y compris sur un pad déjà en prod.
- Les anciens boutons codés en dur (`SampleData*.kt` + boutons fixes dans `Donnee.kt`) ont été supprimés — tout nouveau catalogue passe désormais par ce mécanisme.

---

## 7. Checklist de validation d'un nouveau pad

- [ ] App installée et signée (keystore de prod)
- [ ] Device Owner posé (`Success: Device owner set`)
- [ ] App verrouillée au lancement (kiosk, pas de barre de navigation)
- [ ] PIN admin OK → sortie maintenance OK
- [ ] WiFi / Bluetooth accessibles en maintenance
- [ ] Impression Bluetooth fonctionnelle
- [ ] Reboot → retour direct dans l'app
- [ ] `vpnBlocking = true` posé dans Firestore, « Blocage réseau : actif » en maintenance
- [ ] VPN de blocage actif (`dumpsys connectivity`), caisse connectée, `adb shell ping 8.8.8.8` en échec
- [ ] Heure du pad correcte (date/heure automatique par le réseau mobile)
- [ ] Document `devices/{ANDROID_ID}` créé dans Firestore
- [ ] Test « Vérifier les mises à jour »
- [ ] Écran « Données » → « Nouveau client (Firebase) » liste bien les clients et importe correctement un catalogue
- [ ] Apps inutiles désactivées + animations réduites

---

## Désinstallation / Réinitialisation du kiosk

Le code `KioskManager.clearDeviceOwner()` lève proprement le VPN permanent, arrête le VPN, retire les restrictions réseau, ré-affiche le Play Store puis retire le Device Owner. Via ADB uniquement, le VPN permanent disparaît avec le Device Owner ; si besoin :

```bash
# Retirer le Device Owner
adb shell dpm remove-active-admin com.example.caisse/.kiosk.KioskAdminReceiver
# Si le VPN permanent est resté posé
adb shell settings put secure always_on_vpn_app ""
adb shell settings put secure always_on_vpn_lockdown 0
# Ré-afficher le Play Store
adb shell pm unhide com.android.vending   # peut exiger root selon le constructeur
# Réactiver les apps désactivées si besoin (pm enable <paquet>)
# Remettre les animations
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
```
