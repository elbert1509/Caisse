# Déploiement Caisse sur un nouveau PadPOS (mode Kiosk)

Procédure complète pour configurer un nouveau pad : installation, passage en **mode kiosk Device Owner**, et **allègement du système** pour la fluidité.

- **App** : `com.example.caisse`
- **Admin component** : `com.example.caisse/.kiosk.KioskAdminReceiver`
- **Android cible** : 8.1+ (testé sur MediaTek)

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

### Règles de sécurité

**Firestore** (Console → Firestore → Règles) :
```
match /config/{doc}  { allow read: if true; allow write: if false; }
match /devices/{doc} { allow read: if true; allow write: if false; }
```

**Storage** (Console → Storage → Règles) :
```
match /app-release.apk { allow read: if true; allow write: if false; }
```

---

## 6. Checklist de validation d'un nouveau pad

- [ ] App installée et signée (keystore de prod)
- [ ] Device Owner posé (`Success: Device owner set`)
- [ ] App verrouillée au lancement (kiosk, pas de barre de navigation)
- [ ] PIN admin OK → sortie maintenance OK
- [ ] WiFi / Bluetooth accessibles en maintenance
- [ ] Impression Bluetooth fonctionnelle
- [ ] Reboot → retour direct dans l'app
- [ ] Document `devices/{ANDROID_ID}` créé dans Firestore
- [ ] Test « Vérifier les mises à jour »
- [ ] Apps inutiles désactivées + animations réduites

---

## Désinstallation / Réinitialisation du kiosk

```bash
# Retirer le Device Owner
adb shell dpm remove-active-admin com.example.caisse/.kiosk.KioskAdminReceiver
# Réactiver les apps désactivées si besoin (pm enable <paquet>)
# Remettre les animations
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
```
