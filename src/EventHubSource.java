/**
 * Created by cantguaj on 6/21/17.
 */


import akka.Done;
import akka.actor.*;
import akka.japi.function.Procedure;
import akka.stream.*;
import akka.stream.stage.*;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventprocessorhost.CloseReason;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import scala.Tuple2;
import scala.concurrent.ExecutionContext;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventHubSource extends GraphStageWithMaterializedValue<SourceShape<Tuple2<PartitionContext, EventData>>, IEventProcessor>  {


    private Outlet<Tuple2<PartitionContext, EventData>> out;
    private SourceShape<Tuple2<PartitionContext, EventData>> shape;


    private class Logic extends GraphStageLogic implements IEventProcessor{

        private final AtomicBoolean started = new AtomicBoolean();
        private final EventHubSource source = new EventHubSource();
        private Queue<EventData> pendingEvents;
        private PartitionContext currentContext;
        private int partitionCount;

        private AsyncCallback<Done> openCallback = createAsyncCallback(new Procedure<Done>() {
            @Override
            public void apply(Done param) throws Exception{
                partitionCount++;
            }
        });

        private AsyncCallback<Done> closeCallback = createAsyncCallback(new Procedure<Done>() {
            @Override
            public void apply(Done param) throws Exception {
                if(--partitionCount == 0){
                    completeStage();
                }
            }
        });

        private AsyncCallback<Done>  processCallback = createAsyncCallback(new Procedure<Done>() {
            @Override
            public void apply(Done param) throws Exception {
                handler.onPull();
            }
        });

        private ExecutionContext ec;
        private ActorSystem system;
        OutHandler handler = new AbstractOutHandler() {
            @Override
            public void onPull() throws Exception {
                if(pendingEvents == null || pendingEvents.isEmpty()){
                    return;
                }

                if(isAvailable(out)){
                    if(pendingEvents.isEmpty() || pendingEvents == null){
                        return;
                    }
                    EventData current = pendingEvents.poll();
                    push(out, new Tuple2(currentContext, current));
                }
            }
        };

        @Override
        public void preStart(){
            system = ActorSystem.create("dispatcher");
            ec = system.dispatcher();
        }


        @Override
        public void onClose(PartitionContext partitionContext, CloseReason closeReason) throws Exception{
            closeCallback.invoke(Done.getInstance());
        }

        @Override
        public void onOpen(PartitionContext partitionContext) throws Exception {
            System.out.println("SAMPLE: Partition " + partitionContext.getPartitionId() + " is opening");
            openCallback.invoke(Done.getInstance());
            pendingEvents =  new ArrayDeque<>();
        }

        @Override
        public void onEvents(PartitionContext partitionContext, Iterable<EventData> iterable) throws Exception {
            Iterator<EventData> toIterate = iterable.iterator();
            currentContext = partitionContext;
            while(toIterate.hasNext()){
                EventData data = toIterate.next();
                pendingEvents.offer(data);

            }
           // CompletionStage<Done> completion = new CompletableFuture<>();
            processCallback.invoke(Done.getInstance());
        }

        @Override
        public void onError(PartitionContext partitionContext, Throwable throwable) {
           System.out.println("Error in the connector: " + throwable.getMessage());

        }

        public Logic(EventHubSource source) {
            super(source.shape());
            source = this.source;
            setHandler(out, handler);
        }


    }



    @Override
    public SourceShape<Tuple2<PartitionContext, EventData>> shape(){
        return shape;
    }

    public EventHubSource(){
        out = Outlet.create("EventHubSource.out");
        shape = SourceShape.of(out);
    }
    @Override
    public Tuple2<GraphStageLogic, IEventProcessor> createLogicAndMaterializedValue(Attributes attributes){
        GraphStageLogic logic = new Logic(this);
        return new Tuple2(logic, logic);

    }

}
