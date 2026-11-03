package nl.johninnl.navigation.alerts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Semantic display state only. A card or snapshot is never permission to play audio. */
public final class WarningView {
    private String session="";
    private long sequence;
    private final Map<String,WarningEvent> values=new HashMap<>();
    public synchronized void snapshot(WarningConnection.Snapshot snapshot) {
        if(session.equals(snapshot.providerSession) && snapshot.headSequence<sequence)return;
        session=snapshot.providerSession;sequence=snapshot.headSequence;values.clear();for(WarningEvent event:snapshot.state)values.put(event.warningId,event);
    }
    public synchronized void events(List<WarningEvent> events) {
        for(WarningEvent event:events) {
            if(!event.providerSession.equals(session)) { session=event.providerSession;sequence=0;values.clear(); }
            if(event.sequence<=sequence)continue;sequence=event.sequence;
            WarningEvent previous=values.get(event.warningId);
            if(previous!=null && event.revision<previous.revision)continue;
            if(event.operation==WarningEvent.Operation.CLEAR)values.remove(event.warningId);
            else if(previous!=null || values.size()<WarningJournal.MAX_WARNINGS)values.put(event.warningId,event);
        }
    }
    public synchronized WarningEvent current(long now) {
        return current(now,event->true);
    }
    public synchronized WarningEvent current(long now,java.util.function.Predicate<WarningEvent> enabled) {
        values.entrySet().removeIf(entry->!entry.getValue().stateValid(now));
        WarningEvent best=null;
        for(WarningEvent event:values.values())if(!Boolean.TRUE.equals(event.fields().get("is_preview")) && !event.category.equals("display_only") && enabled.test(event) && WarningEvent.hasText(event.message) && (best==null
                || event.priority.ordinal()<best.priority.ordinal() || event.priority==best.priority && event.sequence>best.sequence))best=event;
        return best;
    }
    public synchronized String messages(long now,java.util.function.Predicate<WarningEvent> enabled){
        values.entrySet().removeIf(entry->!entry.getValue().stateValid(now));
        java.util.List<WarningEvent> current=new java.util.ArrayList<>();
        for(WarningEvent event:values.values())if(!Boolean.TRUE.equals(event.fields().get("is_preview"))&&!event.category.equals("display_only")&&enabled.test(event)&&WarningEvent.hasText(event.message))current.add(event);
        current.sort(java.util.Comparator.comparingInt((WarningEvent e)->e.priority.ordinal()).thenComparingLong(e->e.announceUntil).thenComparingLong(e->e.sequence));
        StringBuilder text=new StringBuilder();int count=0;
        for(WarningEvent event:current){if(count++==3)break;if(text.length()>0)text.append('\n');text.append(event.message.replace('\n',' ').replace('\r',' '));}
        return text.toString();
    }
    public synchronized void clear() { session="";sequence=0;values.clear(); }
}
