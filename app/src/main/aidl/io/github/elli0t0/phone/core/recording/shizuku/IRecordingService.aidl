package io.github.elli0t0.phone.core.recording.shizuku;

import android.os.ParcelFileDescriptor;

interface IRecordingService {
    /**
     * Starts capturing call audio (VOICE_CALL source) and writing a WAV
     * stream to the given file descriptor. Returns 0 on success:
     * 1 = audio could not be initialized, 2 = already recording.
     */
    int startRecording(in ParcelFileDescriptor fd);

    /**
     * Stops capture, patches the WAV header with the final sizes, and
     * returns the PCM bytes written, or -1 if nothing was recording.
     */
    long stopRecording();

    boolean isRecording();
}
