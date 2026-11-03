package nl.johninnl.navigation.alerts;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.app.PendingIntent;
import java.io.Closeable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import nl.johninnl.navigation.startup.NavigationClient;

/** Negotiate WARNINGS_V1 on the existing health worker before provider startup. */
public final class WarningSession implements NavigationClient.Capabilities,Closeable {
    private final Context app;
    private final WarningConnection.Listener listener;
    private final Map<String,ThreadPoolExecutor> workers=new HashMap<>();
    private WarningConnection connection;
    private IBinder binder;
    private long generation=-1,minimumGeneration;
    private boolean closed;
    private volatile String state="off";
    private volatile boolean captureComplete;
    private volatile String eventSource="";
    private volatile PendingIntent recovery;
    public WarningSession(Context context,WarningConnection.Listener listener) { app=context.getApplicationContext();this.listener=listener; }
    public String state() { return state; }
    public boolean captureComplete() { return captureComplete; }
    public boolean advertisesEvents(String source){return source!=null&&source.equals(eventSource);}
    /** Only an explicit source change permits returning to deliberately selected legacy behavior. */
    public void resetEventNegotiation(){eventSource="";recovery=null;}
    public PendingIntent recoveryAction(){return recovery;}
    public synchronized WarningConnection connection() { return connection; }
    /** Recheck after posting a callback to an app UI queue, where a source switch may intervene. */
    public synchronized boolean accepts(String providerSession) {
        return !closed && connection!=null && generation>=minimumGeneration && providerSession!=null
                && providerSession.equals(connection.providerSession());
    }
    public synchronized boolean consume(String providerSession,Runnable action) {
        if(!accepts(providerSession))return false;action.run();return true;
    }
    @Override public boolean prepare(String source,Bundle status,long ticket) throws Exception {
        IBinder api=status.getBinder("warning_api");int[] protocols=status.getIntArray("warning_protocols");boolean v1=false;
        if(protocols!=null)for(int protocol:protocols)if(protocol==1)v1=true;
        final WarningConnection selected;
        final boolean reuse;
        synchronized(this) {
            if(closed || ticket<minimumGeneration)return false;
            if(api==null) {
                if(v1) { state="incompatible";return false; }
                detach();state="legacy";generation=ticket;return true;
            }
            if(!v1) { state="incompatible";return false; }
            eventSource=source;
            PendingIntent action=status.getParcelable("warning_recovery");
            if(action!=null&&source.equals(action.getCreatorPackage())&&action.getCreatorUid()==app.getPackageManager().getApplicationInfo(source,0).uid
                    &&(android.os.Build.VERSION.SDK_INT<31||action.isImmutable()))recovery=action;
            else recovery=null;
            reuse=connection!=null && binder==api && generation==ticket && "ready".equals(state);
            if(reuse)selected=connection;
            else {
            detach();generation=ticket;binder=api;state="connecting";
            final long active=ticket;
            ThreadPoolExecutor worker=workers.get(source);
            if(worker==null) {
                if(workers.size()>=2) { state="incompatible";return false; }
                worker=WarningConnection.controlWorker();workers.put(source,worker);
            }
            connection=new WarningConnection(app,FlitsmeisterWarningTypes.required(),EnumSet.allOf(WarningAudioOwner.Mode.class),new WarningConnection.Listener() {
                private boolean current() { synchronized(WarningSession.this) { return !closed && generation==active && active>=minimumGeneration; } }
                public void snapshot(WarningConnection.Snapshot value) { if(current()) { captureComplete=value.captureComplete;listener.snapshot(value); } }
                public void events(List<WarningEvent> values) { if(current())listener.events(values); }
                public void ownerRevoked(long epoch) { if(current())listener.ownerRevoked(epoch); }
                public void disconnected(String reason) { if(current()) { state="disconnected";listener.disconnected(reason); } }
            },worker,false);selected=connection;
            }
        }
        boolean ready=reuse?selected.ping().get(3,TimeUnit.SECONDS):selected.open(source,api).get(4,TimeUnit.SECONDS);
        synchronized(this) {
            if(closed || generation!=ticket || connection!=selected || ticket<minimumGeneration) { selected.close();return false; }
            state=ready?"ready":"disconnected";return ready;
        }
    }
    @Override public synchronized void disconnected(long nextGeneration) {
        boolean active=connection!=null;
        minimumGeneration=Math.max(minimumGeneration,nextGeneration);generation=-1;detach();state="off";
        if(active)listener.disconnected("SOURCE_DISCONNECTED");
    }
    private void detach() { captureComplete=false;WarningConnection previous=connection;connection=null;binder=null;if(previous!=null)previous.retire(); }
    @Override public synchronized void close() { closed=true;generation=-1;detach();state="off";for(ThreadPoolExecutor worker:workers.values())worker.shutdown(); }
}
