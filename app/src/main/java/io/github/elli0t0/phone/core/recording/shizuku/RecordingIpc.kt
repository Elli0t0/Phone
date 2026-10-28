package io.github.elli0t0.phone.core.recording.shizuku

import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor

/**
 * Hand-written Binder protocol for the recording user service.
 * Deliberately NOT an .aidl file: the CI build runs only the
 * unit-test graph, which never executes the aidl task, so a
 * generated stub class does not exist at Kotlin compile time.
 * This plain-Kotlin protocol compiles with the rest of the app.
 * The transaction codes and the descriptor must match on both
 * sides; the service enforces the descriptor on every call.
 */
object RecordingIpc {
    const val DESCRIPTOR = "io.github.elli0t0.phone.core.recording.shizuku.IRecordingService"
    const val TRANSACTION_START = IBinder.FIRST_CALL_TRANSACTION
    const val TRANSACTION_STOP = IBinder.FIRST_CALL_TRANSACTION + 1
    const val TRANSACTION_IS_RECORDING = IBinder.FIRST_CALL_TRANSACTION + 2
}

/**
 * App-process side of the protocol: thin wrapper over the raw
 * binder delivered by Shizuku's ServiceConnection. Throws
 * RemoteException (caught by the backend's runCatching) if the
 * privileged process dies mid-call.
 */
class RecordingServiceClient(private val binder: IBinder) {

    fun startRecording(fd: ParcelFileDescriptor): Int {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(RecordingIpc.DESCRIPTOR)
            data.writeParcelable(fd, 0)
            binder.transact(RecordingIpc.TRANSACTION_START, data, reply, 0)
            reply.readException()
            return reply.readInt()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun stopRecording(): Long {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(RecordingIpc.DESCRIPTOR)
            binder.transact(RecordingIpc.TRANSACTION_STOP, data, reply, 0)
            reply.readException()
            return reply.readLong()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun isRecording(): Boolean {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(RecordingIpc.DESCRIPTOR)
            binder.transact(RecordingIpc.TRANSACTION_IS_RECORDING, data, reply, 0)
            reply.readException()
            return reply.readInt() != 0
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
