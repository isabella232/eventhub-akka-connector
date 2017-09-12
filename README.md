# Akka Streams Consumer for Azure Event Hub
Akka streams connector for Azure Event Hub and an Akka streams Source. Can be used as a consumer.
### Documentation
#### EventHubSource

You can create a `Source` for the EventHub either via `Source.FromGraph(new EventHubSource)` or by calling the `EventHubSource.Create` method. 
The `Source` is materialized into an `IEventProcessor` which, in combination with the `SingleProcessorFactory`, can be used to connect the stream with the `EventProcessorHost` (see the [example](https://github.com/Silv3rcircl3/Akka.Streams.Azure/blob/master/src/Akka.Streams.Azure.EventHub.Examples/SingleProcessorExample.cs) for details). Alternatively you can use the `MultiProcessorFactory` which takes a `IRunnableGraph` and `IMaterializer` and creates a new stream for every partition (see the [example](https://github.com/Silv3rcircl3/Akka.Streams.Azure/blob/master/src/Akka.Streams.Azure.EventHub.Examples/MultiProcessorExample.cs) for details), or implement your own factory. 

The `Source` keeps track of the partitions it is responsible for and only completes the stream once `IEventProcessor.CloseAsync` was called for all partitions. 

The `Source` emits a pair including the current message and the `PartitionContext` the message belongs to. By default the `Source` creates a new checkpoint when a partition is closed, i.e. `IEventProcessor.CloseAsync` is called, you can deactivate this behavior by setting `createCheckpointOnClose` to false. Furthermore the `Source` can create a checkpoint on every received batch, to activate this behavior you need to set `createCheckpointForEveryBatch` to true.
>Note: The checkpoint is created once the batch was received but **before** all messages have been send downstream

#### IEventProcessor implementation 

- `OpenAsync`: Completes once the underlying `Source` was started 
- `CloseAsync` : If `createCheckpointOnClose` is set to `true` it completes once the checkpoint was created and if it's the last partition it also completes the stream. 
- `ProcessEventsAsync`: Completes once all messages from the current batch are emitted downstream and, if `createCheckpointForEveryBatch` was set to true, once the checkpoint is created.

#### EventHubSink

You can create a `Sink` for the Storage Queue either via `Sink.FromGraph(new EventHubSink)` or by calling the `EventHubSink.Create` method or use the extension method `ToEventHub` on a `Source<IEnumerable<EventData>, TMat>` directly.
The `Sink` is materialized into a `Task` which will be completed with `Success` when reaching the normal end of the stream, or completed with `Failure` if there is a failure signaled in the stream.

You can configure different behaviors if a batch couldn't be send to the EventHub by using the `SupervisionStrategy` attribute, the following behaviors are available: 

- `Stop`: Default behavior, completes the stream with failure. 
- `Resume`: Sends the batch again. 
- `Restart`: Skips the current batch and continues with the next batch.

There is an `application.conf` file where a specific Event Hub's keys and consumer groups can be specified.


### Contributions

Contributions are welcome. A Sink still has to be written for the whole flow to be complete.
 Please fork the project and create for your finished feature a pull request.
 
### Code of Conduct
 This project abides by the Adobe Code of Conduct. See [CODE OF CONDUCT](CODE_OF_CONDUCT.md).
 
### Licensing
 This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
 
