#include <jni.h>
#include <android/log.h>
#include <sys/mman.h>
#include <cstring>
#include <cstdint>
#include <cmath>
#include <atomic>
#include <vector>
#define TAG "olliE_s2"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
namespace off {
    uintptr_t LocalPlayer=0,PlayerHealth=0,PlayerArmor=0,PlayerTeam=0;
    uintptr_t IsAlive=0,EntityList=0,EntityListSize=0,EntityListItems=0x10;
    uintptr_t TransformAccess=0,CurrentWeapon=0,AmmoPtr=0,ReserveAmmo=0;
    uintptr_t SkinID=0,KnifeID=0,SpeedMult=0,IsGrounded=0,JumpFlag=0;
    uintptr_t CameraMatrix=0,ScreenWidth=0,ScreenHeight=0,AimTargetPtr=0;
    uintptr_t NoRecoilPatch=0,NoSpreadPatch=0,HalloweenScore=0,CandyCount=0;
}
struct Vec3{float x,y,z;};
struct Vec2{float x,y;};
static float vec3_dist(Vec3 a,Vec3 b){float dx=a.x-b.x,dy=a.y-b.y,dz=a.z-b.z;return sqrtf(dx*dx+dy*dy+dz*dz);}
static bool w2s(const float*vp,Vec3 w,float sw,float sh,Vec2&o){
    float cx=vp[0]*w.x+vp[4]*w.y+vp[8]*w.z+vp[12];
    float cy=vp[1]*w.x+vp[5]*w.y+vp[9]*w.z+vp[13];
    float cw=vp[3]*w.x+vp[7]*w.y+vp[11]*w.z+vp[15];
    if(cw<0.001f)return false;
    o.x=(cx/cw+1.f)*0.5f*sw;o.y=(-cy/cw+1.f)*0.5f*sh;return true;}
static uintptr_t get_base(const char*lib){
    FILE*f=fopen("/proc/self/maps","r");if(!f)return 0;
    char line[512];uintptr_t base=0;
    while(fgets(line,sizeof(line),f))
        if(strstr(line,lib)&&strstr(line,"r-xp")){base=(uintptr_t)strtoull(line,nullptr,16);break;}
    fclose(f);return base;}
template<typename T>static inline bool mr(uintptr_t a,T&o){if(!a)return false;o=*reinterpret_cast<volatile T*>(a);return true;}
template<typename T>static inline bool mw(uintptr_t a,T v){if(!a)return false;*reinterpret_cast<volatile T*>(a)=v;return true;}
static bool nop_fn(uintptr_t addr,uint8_t*bak,size_t len){
    static const uint8_t nop4[4]={0x1F,0x20,0x03,0xD5};
    uintptr_t pg=addr&~(uintptr_t)0xFFF;
    if(mprotect((void*)pg,0x2000,PROT_READ|PROT_WRITE|PROT_EXEC))return false;
    if(bak)memcpy(bak,(void*)addr,len);
    for(size_t i=0;i+4<=len;i+=4)memcpy((void*)(addr+i),nop4,4);
    __builtin___clear_cache((char*)addr,(char*)(addr+len));return true;}
static void restore_fn(uintptr_t addr,const uint8_t*bak,size_t len){
    uintptr_t pg=addr&~(uintptr_t)0xFFF;
    mprotect((void*)pg,0x2000,PROT_READ|PROT_WRITE|PROT_EXEC);
    memcpy((void*)addr,bak,len);
    __builtin___clear_cache((char*)addr,(char*)(addr+len));}
struct Feat{
    std::atomic<bool> godMode{false},infiniteAmmo{false},aimbot{false};
    std::atomic<bool> espBox{false},espHealth{false},espDist{false};
    std::atomic<bool> noRecoil{false},noSpread{false},bunnyHop{false};
    std::atomic<bool> speedHack{false},skinChanger{false},halloweenScore{false};
    std::atomic<float> aimbotFov{8.f},speedVal{1.8f};
    std::atomic<int> weaponSkinID{101},knifeSkinID{201};
};
struct Enemy{uintptr_t ptr;Vec3 headPos,footPos;Vec2 headScreen,footScreen;float dist;bool onScreen;int team;};
static Feat g_feat;
static uintptr_t g_base=0;
static std::vector<Enemy> g_enemies;
static float g_vp[16]={},g_sw=1080.f,g_sh=1920.f;
static uint8_t bak_recoil[8]={},bak_spread[8]={};
static bool pat_recoil=false,pat_spread=false;
static void update_world(){
    uintptr_t cam=g_base+off::CameraMatrix;
    if(cam)memcpy(g_vp,(void*)cam,64);
    int sw=0,sh=0;mr(g_base+off::ScreenWidth,sw);mr(g_base+off::ScreenHeight,sh);
    if(sw>0)g_sw=(float)sw;if(sh>0)g_sh=(float)sh;
    uintptr_t local=0;if(!mr(g_base+off::LocalPlayer,local)||!local)return;
    uintptr_t elist=0;int ecount=0;
    mr(g_base+off::EntityList,elist);mr(elist+off::EntityListSize,ecount);
    uintptr_t items=0;mr(elist+off::EntityListItems,items);
    if(!items||ecount<=0||ecount>64)return;
    g_enemies.clear();
    for(int i=0;i<ecount;i++){
        uintptr_t ep=0;mr(items+(uintptr_t)(i*8),ep);if(!ep)continue;
        bool alive=false;mr(ep+off::IsAlive,alive);if(!alive)continue;
        int team=0,myTeam=0;mr(ep+off::PlayerTeam,team);mr(local+off::PlayerTeam,myTeam);
        if(team==myTeam)continue;
        Vec3 foot={},head={};mr(ep+off::TransformAccess,foot.x);
        head={foot.x,foot.y+1.7f,foot.z};
        Vec2 hs={},fs={};
        bool h_ok=w2s(g_vp,head,g_sw,g_sh,hs);
        bool f_ok=w2s(g_vp,foot,g_sw,g_sh,fs);
        Enemy e;e.ptr=ep;e.headPos=head;e.footPos=foot;
        e.headScreen=hs;e.footScreen=fs;
        e.dist=vec3_dist({0,0,0},foot);e.team=team;e.onScreen=h_ok&&f_ok;
        g_enemies.push_back(e);}}
static void run_aimbot(){
    if(g_enemies.empty())return;
    uintptr_t local=0;if(!mr(g_base+off::LocalPlayer,local)||!local)return;
    float cx=g_sw*0.5f,cy=g_sh*0.5f,fov=g_feat.aimbotFov.load();
    float best=fov*fov;uintptr_t bestPtr=0;Vec3 bestHead={};
    for(auto&e:g_enemies){if(!e.onScreen)continue;
        float dx=e.headScreen.x-cx,dy=e.headScreen.y-cy,d2=dx*dx+dy*dy;
        if(d2<best){best=d2;bestPtr=e.ptr;bestHead=e.headPos;}}
    if(!bestPtr)return;
    uintptr_t a=local+off::AimTargetPtr;
    mw<float>(a,bestHead.x);mw<float>(a+4,bestHead.y);mw<float>(a+8,bestHead.z);}
static void run_bhop(){
    uintptr_t p=0;if(!mr(g_base+off::LocalPlayer,p)||!p)return;
    bool g=false;mr(p+off::IsGrounded,g);if(g)mw<bool>(p+off::JumpFlag,true);}
static void run_skin(){
    uintptr_t p=0;if(!mr(g_base+off::LocalPlayer,p)||!p)return;
    uintptr_t w=0;mr(p+off::CurrentWeapon,w);
    if(w)mw<int>(w+off::SkinID,g_feat.weaponSkinID.load());
    mw<int>(p+off::KnifeID,g_feat.knifeSkinID.load());}
static void run_god(){uintptr_t p=0;if(!mr(g_base+off::LocalPlayer,p)||!p)return;mw<float>(p+off::PlayerHealth,999.f);mw<float>(p+off::PlayerArmor,100.f);}
static void run_ammo(){uintptr_t p=0;if(!mr(g_base+off::LocalPlayer,p)||!p)return;uintptr_t w=0;mr(p+off::CurrentWeapon,w);if(w){mw<int>(w+off::AmmoPtr,999);mw<int>(w+off::ReserveAmmo,999);}}
static void run_speed(){uintptr_t p=0;if(!mr(g_base+off::LocalPlayer,p)||!p)return;mw<float>(p+off::SpeedMult,g_feat.speedVal.load());}
static void run_halloween(){uintptr_t p=0;if(!mr(g_base+off::LocalPlayer,p)||!p)return;mw<int>(p+off::HalloweenScore,9999);mw<int>(p+off::CandyCount,999);}
extern "C" JNIEXPORT void JNICALL Java_com_ollie_s2cheat_CheatService_nativeTick(JNIEnv*,jobject){
    if(!g_base){g_base=get_base("GameAssembly.so");if(!g_base){LOGE("base олдсонгүй");return;}LOGI("base=0x%lx",(unsigned long)g_base);}
    update_world();
    if(g_feat.aimbot)run_aimbot();if(g_feat.bunnyHop)run_bhop();if(g_feat.skinChanger)run_skin();
    if(g_feat.godMode)run_god();if(g_feat.infiniteAmmo)run_ammo();if(g_feat.speedHack)run_speed();if(g_feat.halloweenScore)run_halloween();}
extern "C" JNIEXPORT void JNICALL Java_com_ollie_s2cheat_CheatService_setFeature(JNIEnv*env,jobject,jstring jk,jboolean val,jfloat fval){
    const char*k=env->GetStringUTFChars(jk,nullptr);if(!k)return;
    if(!strcmp(k,"godMode"))g_feat.godMode=val;
    else if(!strcmp(k,"infiniteAmmo"))g_feat.infiniteAmmo=val;
    else if(!strcmp(k,"aimbot"))g_feat.aimbot=val;
    else if(!strcmp(k,"aimbotFov"))g_feat.aimbotFov=fval;
    else if(!strcmp(k,"espBox"))g_feat.espBox=val;
    else if(!strcmp(k,"espHealth"))g_feat.espHealth=val;
    else if(!strcmp(k,"espDist"))g_feat.espDist=val;
    else if(!strcmp(k,"noRecoil")){g_feat.noRecoil=val;if(val&&!pat_recoil)pat_recoil=nop_fn(g_base+off::NoRecoilPatch,bak_recoil,8);else if(!val&&pat_recoil){restore_fn(g_base+off::NoRecoilPatch,bak_recoil,8);pat_recoil=false;}}
    else if(!strcmp(k,"noSpread")){g_feat.noSpread=val;if(val&&!pat_spread)pat_spread=nop_fn(g_base+off::NoSpreadPatch,bak_spread,8);else if(!val&&pat_spread){restore_fn(g_base+off::NoSpreadPatch,bak_spread,8);pat_spread=false;}}
    else if(!strcmp(k,"bunnyHop"))g_feat.bunnyHop=val;
    else if(!strcmp(k,"speedHack")){g_feat.speedHack=val;g_feat.speedVal=fval;}
    else if(!strcmp(k,"skinChanger"))g_feat.skinChanger=val;
    else if(!strcmp(k,"weaponSkinID"))g_feat.weaponSkinID=(int)fval;
    else if(!strcmp(k,"knifeSkinID"))g_feat.knifeSkinID=(int)fval;
    else if(!strcmp(k,"halloweenScore"))g_feat.halloweenScore=val;
    env->ReleaseStringUTFChars(jk,k);}
extern "C" JNIEXPORT jfloatArray JNICALL Java_com_ollie_s2cheat_CheatService_getEspData(JNIEnv*env,jobject){
    int n=(int)g_enemies.size();std::vector<float>buf;buf.reserve(1+n*6);buf.push_back((float)n);
    for(auto&e:g_enemies){if(!e.onScreen){for(int i=0;i<6;i++)buf.push_back(-1.f);continue;}
        float hp=0.f;mr(e.ptr+off::PlayerHealth,hp);
        buf.push_back(e.headScreen.x);buf.push_back(e.headScreen.y);
        buf.push_back(e.footScreen.x);buf.push_back(e.footScreen.y);
        buf.push_back(e.dist);buf.push_back(hp);}
    jfloatArray arr=env->NewFloatArray((jsize)buf.size());
    env->SetFloatArrayRegion(arr,0,(jsize)buf.size(),buf.data());return arr;}
