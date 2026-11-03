package nl.johninnl.flitsmeister;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import nl.johninnl.navigation.alerts.FlitsmeisterWarningTypes;
import nl.johninnl.navigation.alerts.WarningCapabilities;
import nl.johninnl.navigation.alerts.WarningEndpoint;
import nl.johninnl.navigation.startup.StartupProtocol;

/** Observes accepted native decisions. Never invokes sound, BLE or eligibility code. */
public final class NativeWarnings {
    private NativeWarnings() {}
    private static final int MAX_WARNINGS=128;
    private static final Map<String,Record> records=new LinkedHashMap<>();
    private static final IdentityHashMap<Object,String> anonymous=new IdentityHashMap<>();
    private static final IdentityHashMap<Object,Long> suppressedProjections=new IdentityHashMap<>();
    private static final IdentityHashMap<Object,String> projectionIds=new IdentityHashMap<>();
    private static final Set<String> notificationControlled=new HashSet<>();
    private static String notificationId;
    private static final ThreadLocal<SourceFact> convertingCits=new ThreadLocal<>();
    private static final IdentityHashMap<Object,SourceFact> citsAliases=new IdentityHashMap<>();
    private static final Map<String,SourceFact> citsFacts=new LinkedHashMap<>();
    private static final class SourceFact {
        final String eventId,type;final long until;String warningId;
        final Map<String,String> languages=new LinkedHashMap<>();
        String selectedLanguage="und";
        String selectedTextHash;
        SourceFact(String id,String type){eventId=id;this.type=type;until=SystemClock.elapsedRealtime()+30000;}
    }
    private static final ThreadLocal<Long> citsProjection=new ThreadLocal<>();
    private static Context app;
    private static WarningEndpoint endpoint;
    private static Consumer<Bundle> testSink;
    private static String presentationId;
    private static boolean captureEnabled;
    private static final class Record {
        Bundle fields; long revision=1,index; boolean listed;
    }
    /** The audited event protocol belongs to this exact pinned native artifact. */
    static boolean supportsEventProtocol(Context context){
        try{
            android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
            return "14.17".equals(info.versionName)&&info.versionCode==1287;
        }catch(android.content.pm.PackageManager.NameNotFoundException|RuntimeException unavailable){return false;}
    }
    public static synchronized WarningEndpoint init(Context context) {
        if(app==null){app=context.getApplicationContext();captureEnabled=true;}
        if(endpoint==null)endpoint=new WarningEndpoint(app,uid->{
            String[] packages=app.getPackageManager().getPackagesForUid(uid);
            String configured=NavigationBroadcasts.receiverPackage();
            if(!StartupProtocol.allowed(packages,configured))return null;
            for(String name:packages)if(StartupProtocol.allowed(new String[]{name},configured))return name;
            return null;
        },new WarningCapabilities(FlitsmeisterWarningTypes.MANIFEST_DIGEST,FlitsmeisterWarningTypes.required(),supportsEventProtocol(app)));
        return endpoint;
    }
    public static synchronized WarningEndpoint endpoint(){return endpoint;}
    public static synchronized void started(){captureEnabled=true;}
    /** Observe only scalar source identity around the original pure CITS model conversion. */
    public static void citsSource(Object event){capture(()->{
        convertingCits.remove();if(event!=null){
            SourceFact fact=new SourceFact(citsId(event),event.getClass().getSimpleName());
            try{
                Object values=event.getClass().getField("freeTextsWhere").get(event);int count=0;
                if(values instanceof Collection)for(Object value:(Collection<?>)values){if(++count>32)break;
                    String text=string(value.getClass().getField("freeText").get(value)),language=string(value.getClass().getField("language").get(value));
                    if(!NativeWarningText.useful(text)||text.length()>4096||language.isEmpty())continue;
                    try{new Locale.Builder().setLanguageTag(language);}catch(RuntimeException invalid){continue;}
                    String key=NativeWarningText.hash(text),previous=fact.languages.get(key);
                    fact.languages.put(key,previous==null||previous.equals(language)?language:"und");
                }
            }catch(ReflectiveOperationException ignored){}
            convertingCits.set(fact);
        }
    });}
    public static void citsModel(Object report){capture(()->{
        SourceFact fact=convertingCits.get();convertingCits.remove();if(fact==null||report==null)return;
        Bundle fields=describe(report);String subtype=fields.getString("native_subtype","");
        fact.warningId=isFamily(report,"EarlyWarningReport")?"early/"+subtype+":"+value(report,"getId"):identity(report,fields);
        fact.selectedTextHash=NativeWarningText.hash(NativeWarningText.free(report));String language=fact.languages.get(fact.selectedTextHash);if(language!=null)fact.selectedLanguage=language;
        if(citsFacts.size()>=MAX_WARNINGS&&!citsFacts.containsKey(fact.warningId)){overflow();return;}
        citsFacts.put(fact.warningId,fact);
        if(citsAliases.size()>=MAX_WARNINGS*2){overflow();return;}citsAliases.put(report,fact);
        Object notification=optional(report,"getNotification");if(notification!=null)citsAliases.put(notification,fact);
    });}
    public static void regular(Object sound,Object report,boolean entering,boolean localMuteBypass,String text,Object completion) {
        capture(()->announce(report,null,text,entering?"enter":"exit",localMuteBypass,"regular",NativeWarningText.context(app,sound)));
    }
    public static void early(Object sound,Object type,Object report,Object notification,String text,boolean localMuteBypass,Object completion) {
        capture(()->{
            if(!captureEnabled)return;
            Object model=notification!=null?notification:report;
            if(model==null)return;
            String effective=notification!=null?value(notification,"getType"):value(report,"getEarlyWarningTypeEnum");
            Bundle fields=describe(model);
            if(!effective.isEmpty())fields.putString("native_subtype",effective);
            fields.putString("native_early_type_argument",string(type));
            if(!effective.isEmpty()&&!string(type).equals(effective))fields.putString("capture_diagnostic","EARLY_TYPE_MISMATCH");
            String id=identity(model,fields);
            if(notificationControlled.contains(id)&&!id.equals(notificationId))return;
            announceFields(model,fields,text,"notice",localMuteBypass,"early",NativeWarningText.context(app,sound));
        });
    }
    /** Called only inside the resumed, accepted VOICE_ONLY arm, before local speech filtering. */
    public static void voiceOnly(Object report){capture(()->announce(report,null,value(report,"getVoiceOnlyText"),"notice",false,"voice_only",app));}
    /** Called only in fc0.x's accepted CitsBaseReport arm. */
    public static void cits(Object report){capture(()->announce(report,null,null,"notice",false,"cits",app));}
    /** Native qh7/category decisions have already accepted this bus publication. */
    public static void notificationState(Object notification){capture(()->{
        if(!captureEnabled)return;
        if(notification==null){if(notificationId!=null)clear(notificationId);notificationId=null;return;}
        Bundle fields=describe(notification);String id=identity(notification,fields);
        if(notificationId!=null&&!notificationId.equals(id))clear(notificationId);
        if(notificationControlled.size()>=MAX_WARNINGS&&!notificationControlled.contains(id)){overflow();return;}
        notificationControlled.add(id);notificationId=id;
        Record old=records.get(id);String supplied=old!=null&&fields.getString("native_text_key").equals(old.fields.getString("native_text_key"))?old.fields.getString("message"):null;
        NativeWarningText.Text text=NativeWarningText.resolve(app,notification,"notice",supplied);
        fields.putString("language_tag",text.language);
        update(id,fields,text.value,text.origin,"notice",false,false,"early_notification_state",false);
    });}
    /** Link the actual UI object to the accepted CITS report; UI redispatch never announces. */
    public static void bindProjection(Object projection,Object report){capture(()->{
        if(!captureEnabled||projection==null||report==null)return;
        Bundle fields=describe(report);String id=identity(report,fields);linkProjection(projection,id);
        Record old=records.get(id);boolean sameText=old!=null&&fields.getString("native_text_key").equals(old.fields.getString("native_text_key"));
        NativeWarningText.Text text;
        if(sameText){
            text=new NativeWarningText.Text(old.fields.getString("message",""),old.fields.getString("message_origin","native_text"),old.fields.getString("language_tag","und"));
            copyNativeTextFields(old.fields,fields);
        }else{
            text=NativeWarningText.resolve(app,report,"notice",null);
            if(old!=null&&old.fields.containsKey("native_announced_text"))text=new NativeWarningText.Text(old.fields.getString("native_announced_text"),"native_text",old.fields.getString("native_announced_language_tag","und"));
            text=citsInstruction(report,fields,text,"notice");
        }
        fields.putString("language_tag",text.language);update(id,fields,text.value,text.origin,"notice",false,false,"cits_projection_state",false);
    });}
    public static void earlyRemoved(Object report){capture(()->{if(report!=null)clear(identity(report,describe(report)));});}
    /** Native ImpactAttenuator feature gate and conversion have already succeeded. */
    public static void impact(Object event,Object projection,Context context){impact(event,projection,context,true);}
    public static void impactUpdate(Object event,Object projection,Context context){impact(event,projection,context,false);}
    private static void impact(Object event,Object projection,Context context,boolean announce){capture(()->{
        if(!captureEnabled||projection==null)return;
        Bundle fields=describe(projection);fields.putString("native_family","ImpactAttenuator");fields.putString("native_type","EARLY_WARNING");
        fields.putString("native_subtype","IMPACT_ATTENUATOR");fields.putString("category","roadworks");fields.putInt("priority",1);
        String id="cits/ImpactAttenuator:"+citsId(event);linkProjection(projection,id);
        String text=join(value(projection,"getTitle"),value(projection,"getSubTitle"));
        fields.putString("language_tag",contextLanguage(context));fields.putBoolean("native_announce_observed",announce);
        update(id,fields,text,"native_resource","notice",announce&&NativeWarningText.useful(text),false,"impact_native",false);
    });}
    public static void impactRemoved(Object event){capture(()->{if(isFamily(event,"ImpactAttenuator"))clear("cits/ImpactAttenuator:"+citsId(event));});}
    /** Accepted traffic-light advice is native display state; it has no speech authority. */
    public static void vriState(Object wrapper){capture(()->{
        if(!captureEnabled||wrapper==null)return;Object report=optional(wrapper,"getSpeedAdvice");if(report==null)return;
        Bundle fields=describe(report);String id=identity(report,fields);
        if(!Boolean.TRUE.equals(optional(wrapper,"getAdded"))){clear(id);return;}
        fields.putString("category","speed_advice");Object speed=optional(report,"getAdviceKph");
        if(speed instanceof Number&&((Number)speed).intValue()>=0)fields.putInt("native_advice_kmh",((Number)speed).intValue());
        NativeWarningText.Text text=NativeWarningText.resolve(app,report,"active",null);fields.putString("language_tag",text.language);
        update(id,fields,text.value,text.origin,"active",false,false,"vri_state",false);
    });}
    private static String citsId(Object event){try{return string(event.getClass().getField("info").get(event).getClass().getMethod("getEventID").invoke(event.getClass().getField("info").get(event)));}catch(ReflectiveOperationException error){overflow();return "unavailable";}}
    private static void linkProjection(Object object,String id){
        if(projectionIds.size()>=MAX_WARNINGS&&!projectionIds.containsKey(object)){overflow();return;}
        projectionIds.put(object,id);suppressedProjections.put(object,SystemClock.elapsedRealtime()+30000);
    }
    /** The immediately following synchronous CITS UI projection is the same native authority. */
    public static void suppressNextProjection(){citsProjection.set(SystemClock.elapsedRealtime()+1000);}
    /** Pin the actual converted object before CITS publishes it to its UI bus. */
    public static void suppressProjection(Object warning){capture(()->{
        long now=SystemClock.elapsedRealtime();suppressedProjections.entrySet().removeIf(e->e.getValue()<now);
        if(suppressedProjections.size()<MAX_WARNINGS)suppressedProjections.put(warning,now+1000);
    });}
    public static void presentation(Context context,Object warning,boolean announce) {
        capture(()->{
            if(!captureEnabled)return;
            if(app==null&&context!=null)app=context.getApplicationContext();
            if(warning==null){if(presentationId!=null)clear(presentationId);presentationId=null;return;}
            Long until=citsProjection.get();citsProjection.remove();
            if(announce&&until!=null&&SystemClock.elapsedRealtime()<=until)return;
            Long projectedUntil=suppressedProjections.get(warning);
            if(announce&&projectedUntil!=null&&SystemClock.elapsedRealtime()<=projectedUntil)return;
            if(projectionIds.containsKey(warning))return;
            Bundle fields=describe(warning);String text=join(value(warning,"getTitle"),value(warning,"getSubTitle"));
            String id=!announce&&presentationId!=null?presentationId:identity(warning,fields);
            if(presentationId!=null&&!presentationId.equals(id))clear(presentationId);
            presentationId=id;
            fields.putBoolean("native_announce_observed",announce);
            update(id,fields,text,"native_resource","notice",announce&&NativeWarningText.useful(text),false,"early_presentation",false);
        });
    }
    /** Snapshot updates never create announcement authority. */
    public static void reports(Object values) {
        if(!(values instanceof Collection))return;
        capture(()->{
            if(!captureEnabled)return;
            // A partial snapshot cannot authorize clearing an unseen native warning.
            if(((Collection<?>)values).size()>MAX_WARNINGS){overflow();return;}
            Set<String> seen=new HashSet<>();int count=0;
            for(Object entry:(Collection<?>)values){
                if(++count>MAX_WARNINGS){overflow();return;}
                if(!Boolean.TRUE.equals(optional(entry,"isActive")))continue;
                Object report=optional(entry,"getReport");if(report==null)continue;
                Bundle fields=describe(report);String id=identity(report,fields);seen.add(id);
                Record old=records.get(id);
                boolean current=old!=null&&!"CLEAR".equals(old.fields.getString("operation"));
                String text=!current?nativeText(report):old.fields.getString("message","");
                String origin=!current?"native_resource":old.fields.getString("message_origin","native_resource");
                String phase=!current?"active":old.fields.getString("phase","active");
                if(current){
                    fields.putString("language_tag",old.fields.getString("language_tag","und"));
                    copyNativeTextFields(old.fields,fields);
                    for(String key:new String[]{"native_text_key","template_id"})if(old.fields.containsKey(key))fields.putString(key,old.fields.getString(key));
                }
                update(id,fields,text,origin,phase,false,false,"report_state",true);
            }
            for(String id:new ArrayList<>(records.keySet())){
                Record record=records.get(id);
                if(record.listed&&!seen.contains(id)&&!"exit".equals(record.fields.getString("phase")))clear(id);
            }
        });
    }
    public static void stopped(){capture(()->{for(String id:new ArrayList<>(records.keySet()))clear(id);captureEnabled=false;anonymous.clear();suppressedProjections.clear();projectionIds.clear();notificationControlled.clear();notificationId=null;presentationId=null;citsProjection.remove();resetCits();});}
    private static synchronized void capture(Runnable action){
        try{
            long now=SystemClock.elapsedRealtime();
            records.entrySet().removeIf(e->now>=e.getValue().fields.getLong("state_until_elapsed_ms"));
            anonymous.entrySet().removeIf(e->!records.containsKey(e.getValue()));
            suppressedProjections.entrySet().removeIf(e->now>e.getValue());
            projectionIds.entrySet().removeIf(e->!records.containsKey(e.getValue()));
            notificationControlled.removeIf(id->!records.containsKey(id));
            citsAliases.entrySet().removeIf(e->now>=e.getValue().until&&!records.containsKey(e.getValue().warningId));citsFacts.entrySet().removeIf(e->now>=e.getValue().until&&!records.containsKey(e.getKey()));
            action.run();
        }catch(RuntimeException error){Log.w("NativeWarnings","Capture unavailable: "+error.getClass().getSimpleName());}
    }
    private static void announce(Object report,Bundle fields,String supplied,String phase,boolean bypass,String path,Context context) {
        if(!captureEnabled||report==null)return;announceFields(report,fields==null?describe(report):fields,supplied,phase,bypass,path,context);
    }
    private static void announceFields(Object report,Bundle fields,String supplied,String phase,boolean bypass,String path,Context context) {
        NativeWarningText.Text text=citsInstruction(report,fields,NativeWarningText.resolve(context,report,phase,supplied),phase);fields.putString("language_tag",text.language);
        SourceFact fact=citsAliases.get(report);
        if(fact!=null&&!fact.selectedLanguage.equals("und")&&text.value.equals(NativeWarningText.free(report)))fields.putString("language_tag",fact.selectedLanguage);
        fields.putBoolean("native_announce_observed",true);
        if(text.origin.equals("template"))fields.putString("template_id","fm_native_exit_v1");
        update(identity(report,fields),fields,text.value,text.origin,phase,NativeWarningText.useful(text.value),bypass,path,false);
    }
    private static NativeWarningText.Text citsInstruction(Object report,Bundle fields,NativeWarningText.Text announced,String phase){
        SourceFact fact=citsAliases.get(report);String instruction=NativeWarningText.free(report);
        if(fact==null||phase.equals("exit")||!NativeWarningText.useful(instruction))return announced;
        fields.putString("native_announced_text",announced.value);fields.putString("native_announced_language_tag",announced.language);
        fields.putString("native_instruction",instruction);fields.putString("native_instruction_language_tag",fact.selectedLanguage);
        return NativeWarningText.withInstruction(announced,instruction,fact.selectedLanguage);
    }
    private static void copyNativeTextFields(Bundle from,Bundle to){
        for(String key:new String[]{"native_announced_text","native_announced_language_tag","native_instruction","native_instruction_language_tag","language_origin"})
            if(from.containsKey(key))to.putString(key,from.getString(key));
    }
    private static void update(String id,Bundle fields,String text,String origin,String phase,boolean announce,boolean bypass,String path,boolean listed) {
        Record record=records.get(id);
        if(record==null){if(records.size()>=MAX_WARNINGS){overflow();return;}record=new Record();records.put(id,record);}
        fields.putString("message",text);fields.putString("message_origin",origin);if(!fields.containsKey("language_tag"))fields.putString("language_tag",language());fields.putString("phase",phase);
        SourceFact source=citsFacts.get(id);
        if(source!=null&&!source.selectedLanguage.equals("und")&&NativeWarningText.hash(text).equals(source.selectedTextHash)){
            fields.putString("language_tag",source.selectedLanguage);fields.putString("language_origin","native_free_text_where");
        }
        if(record.fields!=null&&semanticChange(record.fields,fields))record.revision++;
        if(announce)record.index++;
        long now=SystemClock.elapsedRealtime();
        fields.putString("warning_id",id);fields.putLong("revision",record.revision);fields.putLong("announcement_index",record.index);
        fields.putString("phase",phase);fields.putString("operation",announce?"ANNOUNCE":"UPSERT");
        fields.putString("message",text);fields.putString("message_origin",origin);
        fields.putLong("observed_elapsed_ms",now);fields.putLong("state_until_elapsed_ms",now+30000);
        fields.putString("relationship","unknown");fields.putString("capture_path",path);fields.putBoolean("native_local_mute_bypass",bypass);
        fields.putString("relevance",relevance(fields,phase));
        if(!NativeWarningText.useful(text)&&!"display_only".equals(fields.getString("category")))fields.putString("capture_diagnostic","MISSING_NATIVE_TEXT");
        record.fields=new Bundle(fields);record.listed|=listed;emit(fields);
    }
    private static void clear(String id){
        Record record=records.get(id);if(record==null||"CLEAR".equals(record.fields.getString("operation")))return;
        Bundle fields=new Bundle();fields.putString("operation","CLEAR");fields.putString("warning_id",id);
        fields.putLong("revision",record.revision);fields.putLong("announcement_index",record.index);long now=SystemClock.elapsedRealtime();
        fields.putLong("observed_elapsed_ms",now);fields.putLong("state_until_elapsed_ms",now+30000);
        record.fields=fields;record.listed=false;emit(fields);
    }
    private static void emit(Bundle fields){if(testSink!=null)testSink.accept(new Bundle(fields));else if(endpoint!=null)endpoint.emit(fields);}
    private static Bundle describe(Object report){
        Bundle b=new Bundle();String family=report.getClass().getSimpleName();
        b.putString("native_family",family);String type=value(report,"getReportType"),sub="";
        if(isFamily(report,"EarlyWarningReport"))sub=value(report,"getEarlyWarningTypeEnum");
        else if(isFamily(report,"EarlyWarningNotification")){type="EARLY_WARNING";sub=value(report,"getType");}
        else if(isFamily(report,"CitsLaneClosed")){
            sub=value(report,"getLaneClosedType");Object lane=optional(report,"getLaneIndex"),count=optional(report,"getNumberOfLanesClosed"),nativeType=optional(report,"getLaneClosedType");
            if(lane instanceof Number&&((Number)lane).intValue()>=0)b.putInt("lane_index",((Number)lane).intValue());
            if(count instanceof Number&&((Number)count).intValue()>=0)b.putInt("number_of_lanes_closed",((Number)count).intValue());
            if(nativeType instanceof Enum)b.putString("native_lane_code",String.valueOf(nativeType));
        }
        else if(isFamily(report,"CitsWeather")){
            for(Class<?> model=report.getClass();model!=null;model=model.getSuperclass())if(model.getSimpleName().equals("CitsWeather"))try{
                java.lang.reflect.Field field=model.getDeclaredField("mWeatherType");field.setAccessible(true);sub=string(field.get(report));break;
            }catch(ReflectiveOperationException unavailable){b.putString("capture_diagnostic","MISSING_NATIVE_WEATHER_TYPE");}
        }
        else if(isFamily(report,"Incident"))sub=value(report,"getIncidentType");
        else if(isFamily(report,"SpeedCam"))sub=value(report,"getSpeedCamType");
        else if(isFamily(report,"ServiceVehicle"))sub=value(report,"getType");
        if(type.isEmpty())type=family;
        b.putString("native_type",type);if(!sub.isEmpty())b.putString("native_subtype",sub);
        b.putString("native_text_key",NativeWarningText.fingerprint(report));
        b.putString("category",category(type,sub));b.putInt("priority",priority(type,sub));
        // This is the native early emergency model, distinct from an ordinary road-service incident.
        if(sub.equals("ROAD_SERVICE")&&(isFamily(report,"EarlyWarningReport")||isFamily(report,"EarlyWarningNotification"))){b.putString("category","emergency_vehicle");b.putInt("priority",0);}
        if(isFamily(report,"CitsRoadWorks")){b.putString("category","roadworks");b.putInt("priority",1);}
        SourceFact fact=citsAliases.get(report);
        if(fact!=null){b.putString("native_source_event",fact.type);
            if(fact.type.equals("StationaryEmergencyVehicle")){b.putString("category","emergency_vehicle");b.putString("native_source_motion","stationary");b.putInt("priority",1);}
            else if(fact.type.equals("IncomingEmergencyVehicle")){b.putString("category","emergency_vehicle");b.putString("native_source_motion","incoming");b.putInt("priority",0);}}
        Object original=optional(report,"getLocation");
        boolean fixedPoint=isFamily(report,"SpeedCam")||isFamily(report,"SpeedTrap")||isFamily(report,"PoliceHotspot")
            ||(isFamily(report,"Incident")&&optional(report,"getGpsPath")==null)
            ||sub.equals("BRIDGE_CLOSED")||sub.equals("RAILWAY_CROSSING")||sub.equals("UNSECURED_RAILWAY_CROSSING")
            ||sub.equals("BE_RAILWAY_CROSSING")||sub.equals("BE_UNSECURED_RAILWAY_CROSSING");
        if(original instanceof Location){Location l=(Location)original;long at=l.getElapsedRealtimeNanos()/1000000,now=SystemClock.elapsedRealtime();
            if(at==0&&fixedPoint){at=now;b.putString("position_clock_origin","native_decision");}
            else if(at>0)b.putString("position_clock_origin","native_geometry");
            if(at>0&&at<=now&&Double.isFinite(l.getLatitude())&&Double.isFinite(l.getLongitude())){
                b.putDouble("latitude",l.getLatitude());b.putDouble("longitude",l.getLongitude());b.putLong("position_observed_elapsed_ms",at);b.putBoolean("position_estimated",false);
                if(fixedPoint)b.putBoolean("native_point_target",true);
            }else b.remove("position_clock_origin");}
        return b;
    }
    private static String identity(Object model,Bundle fields){
        SourceFact alias=citsAliases.get(model);if(alias!=null)return alias.warningId;
        if(isFamily(model,"EarlyWarningNotification")||isFamily(model,"EarlyWarningReport")){
            String id=isFamily(model,"EarlyWarningReport")?value(model,"getId"):value(model,"getName");
            if(id.isEmpty())id=value(model,"getCode");
            if(!id.isEmpty())return "early/"+fields.getString("native_subtype","")+":"+id;
        }
        String id=value(model,"getId");if(id.isEmpty())id=value(model,"getCode");
        if(id.isEmpty()){id=anonymous.get(model);if(id==null){if(anonymous.size()>=MAX_WARNINGS){overflow();return fields.getString("native_family","")+":overflow";}id=fields.getString("native_family","")+":"+UUID.randomUUID().toString();anonymous.put(model,id);}return id;}
        return fields.getString("native_family","")+":"+id;
    }
    private static boolean semanticChange(Bundle old,Bundle next){
        for(String key:new String[]{"native_type","native_subtype","native_family","native_text_key","native_advice_kmh","native_source_event","native_source_motion","native_instruction","native_instruction_language_tag","native_announced_text","lane_index","number_of_lanes_closed","category","priority","phase","message","message_origin","language_tag","latitude","longitude","position_observed_elapsed_ms","position_estimated"})
            if(!java.util.Objects.equals(old.get(key),next.get(key)))return true;
        return false;
    }
    private static void overflow(){if(endpoint!=null)endpoint.captureOverflow();}
    private static String category(String type,String subtype){
        if(type.equals("BASEREPORT")||type.equals("LBA")||type.equals("CITS_PARKING")||type.equals("FM_OFFICE")
                ||type.equals("PARKED_LOCATION")||type.equals("TOLLING")||type.equals("ENVIRONMENTAL_ZONE"))return "display_only";
        if(type.equals("SPEED_CAM")&&subtype.equals("TRAFFIC_CAMERA"))return "traffic_camera";
        if(type.equals("SPEED_CAM")||type.equals("SPEED_TRAP")||type.equals("AVG_SPEED_CHECK"))return "speed_control";
        if(subtype.equals("AMBULANCE")||subtype.equals("FIRE_DEPARTMENT")||subtype.equals("POLICE"))return "emergency_vehicle";
        if(type.equals("SERVICE_VEHICLE"))return "service_vehicle";
        if(type.equals("SLOW_MOVING_VEHICLE"))return "slow_vehicle";
        if(type.equals("CITS_SPEED_ADVICE"))return "speed_advice";
        if(type.equals("CITS_WEATHER"))return "weather";
        if(type.equals("CITS_BRIDE_CLOSED")||type.equals("CITS_LANE_CLOSED")||subtype.equals("ROAD_CLOSURE")||subtype.equals("CLOSURE_WITH_REROUTE"))return "closure";
        if(subtype.equals("ROADWORKS")||subtype.equals("ROAD_WORKER")||subtype.equals("TEXT_CAR")||subtype.equals("TRAFFIC_CONTROLLER"))return "roadworks";
        if(type.equals("POLICEHOTSPOT")||subtype.equals("CONTROL"))return "police_control";
        if(type.equals("TRAFFIC_JAM"))return "traffic";return "road_danger";
    }
    private static int priority(String type,String subtype){
        if(subtype.equals("WRONG_WAY_DRIVER")||subtype.equals("PERSON_ON_ROAD")||subtype.equals("AMBULANCE")||subtype.equals("FIRE_DEPARTMENT")||subtype.equals("POLICE"))return 0;
        if(type.equals("SPEED_CAM")&&(subtype.equals("TRAFFIC_CAMERA")||subtype.equals("OTHER")))return 2;
        if(type.equals("SPEED_CAM")||type.equals("SPEED_TRAP")||type.equals("AVG_SPEED_CHECK")||type.equals("POLICEHOTSPOT")
                ||type.equals("CITS_BRIDE_CLOSED")||type.equals("CITS_LANE_CLOSED")||type.equals("SERVICE_VEHICLE")||type.equals("SLOW_MOVING_VEHICLE")
                ||subtype.equals("ACCIDENT")||subtype.equals("OBSTACLE")||subtype.equals("CONTROL")||subtype.equals("ROAD_CLOSURE")||subtype.equals("CLOSURE_WITH_REROUTE")
                ||subtype.equals("ROADWORKS")||subtype.equals("ROAD_WORKER")||subtype.equals("TEXT_CAR")||subtype.equals("TRAFFIC_CONTROLLER")
                ||subtype.equals("RAILWAY_CROSSING")||subtype.equals("UNSECURED_RAILWAY_CROSSING")||subtype.equals("BE_RAILWAY_CROSSING")||subtype.equals("BE_UNSECURED_RAILWAY_CROSSING")||subtype.equals("BRIDGE_CLOSED"))return 1;
        return 2;
    }
    private static String relevance(Bundle fields,String phase){
        if(phase.equals("exit"))return "exit";
        String type=fields.getString("native_type",""),sub=fields.getString("native_subtype","");
        if(fields.getString("native_source_motion","").equals("stationary"))return "notice";
        if(fields.getString("category","").equals("emergency_vehicle"))return "moving_emergency";
        if(type.equals("AVG_SPEED_CHECK"))return "zone";
        if(type.equals("SPEED_CAM")||type.equals("SPEED_TRAP")||type.equals("POLICEHOTSPOT"))return "point_ahead";
        if(fields.getBoolean("native_point_target",false))return "point_ahead";
        return "notice";
    }
    private static String nativeText(Object model){
        for(String method:new String[]{"getShortDescription","reportTitleText"}){
            if(app==null)break;try{Object result=model.getClass().getMethod(method,Context.class).invoke(model,app);String text=string(result);if(!text.trim().isEmpty())return text;}catch(ReflectiveOperationException ignored){}
        }
        for(String method:new String[]{"getFreeText","getFreetext","getDescription","getTitle"}){String text=value(model,method);if(!text.trim().isEmpty())return text;}
        return "";
    }
    private static boolean isFamily(Object object,String name){for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())if(c.getSimpleName().equals(name))return true;return false;}
    private static Object optional(Object object,String method){if(object==null)return null;try{return object.getClass().getMethod(method).invoke(object);}catch(ReflectiveOperationException ignored){return null;}}
    private static String value(Object object,String method){return string(optional(object,method));}
    private static String string(Object value){return value==null?"":value instanceof Enum?((Enum<?>)value).name():String.valueOf(value);}
    private static String join(String a,String b){return a.isEmpty()?b:b.isEmpty()?a:a+". "+b;}
    private static String language(){if(app!=null){android.os.LocaleList locales=app.getResources().getConfiguration().getLocales();if(!locales.isEmpty())return locales.get(0).toLanguageTag();}return "und";}
    private static String contextLanguage(Context context){if(context!=null){android.os.LocaleList locales=context.getResources().getConfiguration().getLocales();if(!locales.isEmpty())return locales.get(0).toLanguageTag();}return "und";}
    private static void resetCits(){convertingCits.remove();citsAliases.clear();citsFacts.clear();}
    static synchronized void resetForTest(Context context,Consumer<Bundle> sink){app=context;testSink=sink;captureEnabled=true;records.clear();anonymous.clear();suppressedProjections.clear();projectionIds.clear();notificationControlled.clear();notificationId=null;presentationId=null;citsProjection.remove();resetCits();}
    static synchronized void setEndpointForTest(WarningEndpoint value){
        if(endpoint!=null&&endpoint!=value)endpoint.close();endpoint=value;app=null;testSink=null;captureEnabled=false;
        records.clear();anonymous.clear();suppressedProjections.clear();projectionIds.clear();notificationControlled.clear();notificationId=null;presentationId=null;citsProjection.remove();resetCits();
    }
}
