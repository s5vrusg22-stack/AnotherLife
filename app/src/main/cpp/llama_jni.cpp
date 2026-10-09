#include <jni.h>
#include <llama.h>
#include <algorithm>
#include <atomic>
#include <mutex>
#include <string>
#include <vector>
namespace {
std::mutex guard;
llama_model* model=nullptr;
llama_context* ctx=nullptr;
std::atomic<bool> cancelled{false};
std::string fromJ(JNIEnv* e,jstring s){const char* p=e->GetStringUTFChars(s,nullptr);std::string r=p?p:"";if(p)e->ReleaseStringUTFChars(s,p);return r;}
jstring err(JNIEnv* e,const char* s){return e->NewStringUTF(s);}
}
extern "C" JNIEXPORT jstring JNICALL Java_com_anotherlife_app_ai_LlamaRuntime_nativeLoad(JNIEnv* e,jobject,jstring path,jint contextSize){
 std::lock_guard<std::mutex> lk(guard);
 if(ctx){llama_free(ctx);ctx=nullptr;}if(model){llama_model_free(model);model=nullptr;}
 static std::once_flag once;std::call_once(once,[]{llama_backend_init();});
 auto p=llama_model_default_params();p.n_gpu_layers=0;
 model=llama_model_load_from_file(fromJ(e,path).c_str(),p);
 if(!model)return err(e,"Cannot load GGUF model");
 auto cp=llama_context_default_params();cp.n_ctx=std::clamp((int)contextSize,1024,16384);cp.n_batch=256;
 ctx=llama_init_from_model(model,cp);
 if(!ctx){llama_model_free(model);model=nullptr;return err(e,"Cannot create llama context");}
 return err(e,"");
}
extern "C" JNIEXPORT jstring JNICALL Java_com_anotherlife_app_ai_LlamaRuntime_nativeGenerate(JNIEnv* e,jobject,jstring input,jobject receiver){
 std::lock_guard<std::mutex> lk(guard);
 if(!ctx||!model)return err(e,"Model not loaded");
 cancelled=false;
 std::string raw=fromJ(e,input);
 const char* templ=llama_model_chat_template(model,nullptr);
 std::string prompt=raw;
 if(templ){llama_chat_message msgs[]={{"system","You are an autonomous character in Another Life. Speak Korean. Never control the player. /no_think"},{"user",raw.c_str()}};
 int n=llama_chat_apply_template(templ,msgs,2,true,nullptr,0);
 if(n>0){std::string buf((size_t)n+1,'\0');int written=llama_chat_apply_template(templ,msgs,2,true,buf.data(),(int)buf.size());if(written>0){buf.resize(written);prompt=std::move(buf);}}}
 const llama_vocab* vocab=llama_model_get_vocab(model);
 int count=-llama_tokenize(vocab,prompt.c_str(),(int)prompt.size(),nullptr,0,true,true);
 if(count<=0||count>(int)llama_n_ctx(ctx)-513)return err(e,"Prompt too long");
 std::vector<llama_token> tokens((size_t)count);
 if(llama_tokenize(vocab,prompt.c_str(),(int)prompt.size(),tokens.data(),count,true,true)<0)return err(e,"Tokenization failed");
 llama_memory_clear(llama_get_memory(ctx),true);
 llama_sampler* sampler=llama_sampler_chain_init(llama_sampler_chain_default_params());
 llama_sampler_chain_add(sampler,llama_sampler_init_top_k(20));
 llama_sampler_chain_add(sampler,llama_sampler_init_top_p(0.8f,1));
 llama_sampler_chain_add(sampler,llama_sampler_init_temp(0.7f));
 llama_sampler_chain_add(sampler,llama_sampler_init_dist(42));
 jclass cls=e->GetObjectClass(receiver);
 jmethodID cb=cls?e->GetMethodID(cls,"onToken","(Ljava/lang/String;)V"):nullptr;
 if(!cb){if(e->ExceptionCheck())e->ExceptionClear();if(cls)e->DeleteLocalRef(cls);llama_sampler_free(sampler);return err(e,"Missing token callback");}
 std::string error;
 for(int i=0;i<count&&!cancelled;i+=256){int chunk=std::min(256,count-i);llama_batch b=llama_batch_get_one(tokens.data()+i,chunk);if(llama_decode(ctx,b)!=0){error="Prompt evaluation failed";break;}}
 std::string pending;
 for(int i=0;i<512&&error.empty()&&!cancelled;i++){
 llama_token t=llama_sampler_sample(sampler,ctx,-1);if(llama_vocab_is_eog(vocab,t))break;
 llama_sampler_accept(sampler,t);
 char small[512];int n=llama_token_to_piece(vocab,t,small,sizeof(small),0,true);
 if(n>0){pending.append(small,(size_t)n);
 // Emit only complete UTF-8 sequences; malformed sequences are replaced.
 size_t ready=0;for(size_t j=0;j<pending.size();){unsigned char c=(unsigned char)pending[j];size_t width=c<0x80?1:(c>=0xC2&&c<=0xDF?2:(c>=0xE0&&c<=0xEF?3:(c>=0xF0&&c<=0xF4?4:1)));if(j+width>pending.size())break;ready=j+width;j+=width;}
 if(ready){std::string out=pending.substr(0,ready);pending.erase(0,ready);std::string modified;for(char c:out){if(c=='\0'){modified.push_back((char)0xC0);modified.push_back((char)0x80);}else modified.push_back(c);}jstring piece=e->NewStringUTF(modified.c_str());if(!piece||e->ExceptionCheck()){if(e->ExceptionCheck())e->ExceptionClear();error="Invalid UTF-8 token";break;}e->CallVoidMethod(receiver,cb,piece);e->DeleteLocalRef(piece);if(e->ExceptionCheck()){e->ExceptionClear();error="Token callback failed";break;}}}
 llama_batch b=llama_batch_get_one(&t,1);if(llama_decode(ctx,b)!=0){error="Decode failed";break;}
 }
 llama_sampler_free(sampler);e->DeleteLocalRef(cls);return err(e,error.c_str());
}
extern "C" JNIEXPORT void JNICALL Java_com_anotherlife_app_ai_LlamaRuntime_nativeCancel(JNIEnv*,jobject){cancelled=true;}
extern "C" JNIEXPORT void JNICALL Java_com_anotherlife_app_ai_LlamaRuntime_nativeUnload(JNIEnv*,jobject){std::lock_guard<std::mutex> lk(guard);if(ctx){llama_free(ctx);ctx=nullptr;}if(model){llama_model_free(model);model=nullptr;}}
