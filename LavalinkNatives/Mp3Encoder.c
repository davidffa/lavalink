#include <jni.h>
#include <lame.h>

#ifdef __GNUC__
#define EXPORT __attribute__ ((visibility("default"))) JNIEXPORT
#else
#define EXPORT JNIEXPORT
#endif

EXPORT jlong JNICALL Java_lavalink_server_natives_mp3_Mp3EncoderLibrary_create(JNIEnv *jni, jobject me, jint sample_rate, jint channels, jint bitrate) {
    lame_t encoder = lame_init();

    lame_set_in_samplerate(encoder, sample_rate);
    lame_set_num_channels(encoder, channels);
    lame_set_VBR(encoder, vbr_off);
    lame_set_brate(encoder, bitrate / 1000);
    lame_set_out_samplerate(encoder, sample_rate);
    lame_init_params(encoder);

    return (jlong) encoder;
}

EXPORT jint JNICALL Java_lavalink_server_natives_mp3_Mp3EncoderLibrary_encodeStereo(JNIEnv *jni, jobject me, jlong instance, jobject direct_input, jint frame_size, jobject direct_output, jint output_length) {
    lame_t encoder = (lame_t) instance;

    if (instance == 0) {
        return 0;
    }

    short int* input = (*jni)->GetDirectBufferAddress(jni, direct_input);
    unsigned char* output = (*jni)->GetDirectBufferAddress(jni, direct_output);

    return lame_encode_buffer_interleaved(encoder, input, frame_size, output, output_length);
}

EXPORT jint JNICALL Java_lavalink_server_natives_mp3_Mp3EncoderLibrary_encodeMono(JNIEnv *jni, jobject me, jlong instance, jobject direct_input, jint frame_size, jobject direct_output, jint output_length) {
    lame_t encoder = (lame_t) instance;

    if (instance == 0) {
        return 0;
    }

    short int* input = (*jni)->GetDirectBufferAddress(jni, direct_input);
    short int* r = NULL;
    unsigned char* output = (*jni)->GetDirectBufferAddress(jni, direct_output);

    return lame_encode_buffer(encoder, input, r, frame_size, output, output_length);
}

EXPORT jint JNICALL Java_lavalink_server_natives_mp3_Mp3EncoderLibrary_flush(JNIEnv *jni, jobject me, jlong instance, jobject direct_output, jint output_length) {
    lame_t encoder = (lame_t) instance;

    if (instance == 0) {
        return 0;
    }

    unsigned char* output = (*jni)->GetDirectBufferAddress(jni, direct_output);

    return lame_encode_flush(encoder, output, output_length);
}

EXPORT void JNICALL Java_lavalink_server_natives_mp3_Mp3EncoderLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
    lame_t encoder = (lame_t) instance;

    if (encoder != NULL) {
        lame_close(encoder);
    }
}