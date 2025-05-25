#include <jni.h>
#include <string>
#include "llama.h"   // from llama_cpp/include/llama.h

// Keep a single global context
static llama_context * ctx = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_theaterapp_api_LlamaService_initModel(
    JNIEnv *env,
    jobject /* this */,
    jstring modelPath
) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    // use default model params
    llama_context_params params = llama_default_context_params();
    ctx = llama_init_from_file(path, params);
    env->ReleaseStringUTFChars(modelPath, path);
    return (ctx != nullptr);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_theaterapp_api_LlamaService_generate(
    JNIEnv *env,
    jobject /* this */,
    jstring prompt,
    jint maxTokens
) {
    // Convert Java string → C string
    const char *c_prompt = env->GetStringUTFChars(prompt, nullptr);
    std::string output;

    // 1. Tokenize the prompt
    std::vector<llama_token> tokens = llama_tokenize(ctx, c_prompt, true);
    // 2. Evaluate prompt tokens
    llama_eval(ctx, tokens.data(), (int)tokens.size(), 0, 1);

    // 3. Generate new tokens
    for (int i = 0; i < maxTokens; i++) {
        // sample the next token
        llama_token next = llama_sample(ctx);
        // append text
        output += llama_token_to_str(ctx, next);
        // feed it back for context
        llama_eval(ctx, &next, 1, 0, 1);
    }

    // release and return
    env->ReleaseStringUTFChars(prompt, c_prompt);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_theaterapp_api_LlamaService_cleanup(
    JNIEnv * /* env */,
    jobject /* this */
) {
    if (ctx) {
        llama_free(ctx);
        ctx = nullptr;
    }
}
