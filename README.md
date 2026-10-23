# Caisse

Application Android de caisse (POS) pour restaurants et bars, déployée en **mode kiosque** sur des terminaux Android 7+ (4G/3G).

- **Package** : `com.example.caisse`
- **Stack** : Kotlin, Jetpack Compose (Material3), Room, Firebase (Auth, Firestore, Storage), WorkManager, Coil
- **SDK** : `minSdk 23`, `targetSdk 34`, `compileSdk 36`
- **Conformité** : NF525 (aucune suppression physique, ventes chaînées par hash SHA-256, journal technique JET, clôtures, export)

## Structure (`app/src/main/java/com/example/caisse/`)

| Dossier | Contenu |
|---|---|
| `composable/` | Écrans Compose (caisse, panier, tables, stock, historique, clôture, gestion…) |
| `data/`, `model/` | Room (entités, DAO, migrations), repository, `MenuViewModel`, workers (synchro, clôture auto) |
| `kiosk/` | Device Owner, LockTask, mises à jour silencieuses, VPN de blocage réseau |
| `dashboard/` | Tableau de bord et graphiques |
| `bluetooth/` | Imprimante ticket Bluetooth |
| `util/` | Hash fiscal, rapports PDF, export, partage |

## Mode kiosque et réseau

Une fois Device Owner (`adb shell dpm set-device-owner com.example.caisse/.kiosk.KioskAdminReceiver`), l'appli :

- verrouille l'écran sur la caisse (LockTask) et devient le launcher ;
- se met à jour seule via Firestore + Firebase Storage ;
- **est la seule à consommer des données** : un VPN local « trou noir » (`kiosk/BlockingVpnService`), imposé comme VPN permanent, jette le trafic de toutes les autres applis. La caisse en est exclue et garde le réseau. Il s'active **à distance** avec le champ Firestore `vpnBlocking` (`devices/{ANDROID_ID}` ou `config/app_android`), désactivé par défaut ; le mode lockdown avec `vpnLockdown` (défaut : `KioskManager.VPN_LOCKDOWN`, `false`).

Procédure complète (installation, Device Owner, blocage réseau et tests adb, OTA, catalogues, checklist) : [`app/DEPLOIEMENT_PAD.md`](app/DEPLOIEMENT_PAD.md).

## Build

```bash
./gradlew :app:assembleDebug     # build de développement
./gradlew :app:assembleRelease   # APK de prod (même keystore que les pads déployés)
```
