// Interface AIDL du service d'impression intégré (Sunmi / compatibles "woyou").
// Ce fichier doit rester identique à l'interface exposée par le service système du terminal :
// l'ordre des méthodes détermine les codes de transaction binder, toute modification de l'ordre
// désynchroniserait les appels avec le vrai service.
package woyou.aidlservice.jiuiv5;

interface ICallback {

    oneway void onRunResult(boolean isSuccess);

    oneway void onReturnString(String result);

    oneway void onRaiseException(int code, String msg);
}
