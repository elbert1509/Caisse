package com.example.caisse.printer

import android.os.Parcel
import android.os.Parcelable

/**
 * Type de données référencé par IWoyouService.commitPrint (non utilisé par l'app, la méthode
 * n'est jamais appelée) — présent uniquement pour que l'interface AIDL compile en conservant
 * l'ordre exact des méthodes du service réel (codes de transaction binder).
 */
class TransBean(
    private val type: Byte,
    private val text: String,
    private val data: ByteArray?
) : Parcelable {

    constructor() : this(0, "", null)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(type)
        dest.writeInt(data?.size ?: 0)
        dest.writeString(text)
        data?.let { dest.writeByteArray(it) }
    }

    companion object CREATOR : Parcelable.Creator<TransBean> {
        override fun createFromParcel(parcel: Parcel): TransBean {
            val type = parcel.readByte()
            val length = parcel.readInt()
            val text = parcel.readString() ?: ""
            val data = if (length > 0) parcel.createByteArray() else null
            return TransBean(type, text, data)
        }

        override fun newArray(size: Int): Array<TransBean?> = arrayOfNulls(size)
    }
}
