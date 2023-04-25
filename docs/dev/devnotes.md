# ZIO Insight Agent

The ZIO Insight Agent is responsible for instrumenting the ZIO applications to capture fiber events and other relevant data. Its key characteristics and features are:

1. **Lightweight**: The agent should have a minimal performance impact on the instrumented application. It should be efficient in capturing, encoding, and transmitting fiber events.
2. **Redis Publisher**: The agent uses a Redis publisher to send fiber events to the Redis server. This allows for real-time processing and aggregation of events in the monitoring system.
3. **Protobuf Encoding**: The fiber events are encoded using the Protocol Buffers (Protobuf) binary format, providing an efficient and compact representation of the data.

To implement the ZIO Insight Agent, you will need to:

- Develop the agent library that instruments the ZIO application, capturing fiber events and other relevant data.
- Integrate a Redis client library to publish the captured events to a Redis channel.
- Define the Protobuf schema for the fiber events and use a Protobuf library to encode the events before publishing.

By designing the ZIO Insight Agent with these features, you'll ensure efficient and real-time monitoring of your ZIO applications with minimal overhead.

# ZIO Insight Server

The ZIO Insight Server is responsible for processing, aggregating, and storing the fiber events received from the ZIO Insight Agents. Its key characteristics and features are:

1. **Redis Subscriber**: The server subscribes to the Redis channel to receive fiber events published by the ZIO Insight Agents in real-time.
2. **Event Processing**: The server processes the incoming fiber events, decoding the Protobuf-encoded messages, and extracting relevant information.
3. **Aggregation**: The server aggregates the incoming events into data structures that are suitable for visualization in the Web UI. This may involve computing metrics, summarizing data, or performing other transformations to make the data easy to visualize and understand.
4. **Data Storage**: The server stores the aggregated data in Redis or another suitable storage solution, making it accessible to the Web UI for visualization.

To implement the ZIO Insight Server, you will need to:

- Develop the server-side application that subscribes to the Redis channel and listens for incoming fiber events.
- Integrate a Redis client library to handle the subscription and retrieval of events.
- Use a Protobuf library to decode the incoming fiber events.
- Implement the aggregation logic to transform the raw fiber events into data structures suitable for visualization.
- Store the aggregated data in Redis or another storage solution, ensuring efficient retrieval for visualization in the Web UI.

By designing the ZIO Insight Server with these features, you'll enable real-time monitoring and visualization of the fiber events from multiple ZIO applications.

# ZIO Insight UI

The ZIO Insight UI is responsible for providing a user-friendly interface for visualizing and interacting with the aggregated data from the ZIO Insight Server. Its key characteristics and features are:

1. **Effect**: The UI leverages Effect, a TypeScript-based ZIO 2 clone, to define services that communicate with the ZIO Insight Server. Effect allows you to manage asynchronous operations and side effects in a functional and type-safe manner.
2. **Service Layer**: The service layer, built with Effect, handles communication with the ZIO Insight Server, fetching the aggregated data, and performing any necessary data transformations for visualization.
3. **UI Components**: The actual user interface components are built using a popular UI library, such as React or Vue, providing a responsive and interactive experience for users.
4. **UI Integration**: The UI components rely on the Effect services as much as possible, promoting a clear separation of concerns between the UI and the service layer.

To implement the ZIO Insight UI, you will need to:

- Develop the Effect-based services that handle communication with the ZIO Insight Server, fetching the aggregated data, and performing any required data transformations.
- Choose a UI library (e.g., React or Vue) and create the user interface components for visualizing the data.
- Integrate the UI components with the Effect services, ensuring a clean separation of concerns between the UI and the service layer.

By designing the ZIO Insight UI with these features, you'll provide an intuitive and interactive interface for users to visualize and explore the fiber events and aggregated data from the monitored ZIO applications.

# ZIO Insight Plugins

The ZIO Insight Plugin System is designed to allow for easy extension and integration of new functionality, event processing, and visualizations. Each plugin can extend the events captured by the ZIO Insight Agent, the event processing on the ZIO Insight Server, and the corresponding visualization in the ZIO Insight UI.

To support plugins, you will need to:

1. Define a standard interface for plugins, allowing them to extend the ZIO Insight Agent, Server, and UI components.
2. Implement a plugin registration and management system, enabling dynamic loading and configuration of plugins.
3. Ensure that the core ZIO Insight components are extensible and can interact with plugins seamlessly.

## ZIO Insight Initial Plugins

### Metrics Plugin

The first ZIO Insight plugin, Metrics, is focused on capturing metric events from the applications and providing visualizations of these metrics over time. The Metrics plugin will extend the following components:

- **ZIO Insight Agent**: The agent will be responsible for capturing metric events and publishing them to the Redis server using the Protobuf binary format.
- **ZIO Insight Server**: The server will process the incoming metric events, aggregate them, and store them as time-based collections of events in Redis or another suitable storage solution.
- **ZIO Insight UI**: The UI will provide visualizations of the metrics over time, allowing users to explore the collected data and gain insights into the performance and behavior of their ZIO applications.

To implement the Metrics plugin, you will need to:

- Extend the ZIO Insight Agent to capture and publish metric events.
- Enhance the ZIO Insight Server to process, aggregate, and store metric events.
- Create new UI components and visualizations in the ZIO Insight UI to display the metric data over time.

By developing the Metrics plugin and the ZIO Insight Plugin System, you'll be laying the foundation for a flexible and extensible monitoring solution that can grow and adapt to your needs.

### FiberTracing Plugin

The FiberTracing plugin is focused on capturing fiber lifecycle events in ZIO applications and providing visualizations to help developers understand and analyze the execution of fibers over time. The FiberTracing plugin will extend the following components:

- **ZIO Insight Agent**: The agent will be responsible for capturing fiber lifecycle events, such as Started, Succeeded, Errored, Suspended, Resumed, and Execute Step, and publishing them to the Redis server using the Protobuf binary format.
- **ZIO Insight Server**: The server will process the incoming fiber events, aggregate them, and store them in Redis or another suitable storage solution. It may also derive additional information, such as dependencies between fibers, to support more advanced visualizations.
- **ZIO Insight UI**: The UI will provide various visualizations of the fiber data, such as FiberGraphs, Gantt-Charts, and FlameGraphs. These visualizations will help developers understand how groups of related fibers execute over time and identify time-critical areas within the application.

To implement the FiberTracing plugin, you will need to:

- Extend the ZIO Insight Agent to capture and publish fiber lifecycle events.
- Enhance the ZIO Insight Server to process, aggregate, and store fiber events. You may also need to implement additional logic to derive relationships between fibers and other relevant information.
- Create new UI components and visualizations in the ZIO Insight UI to display the fiber data in various formats, such as FiberGraphs, Gantt-Charts, and FlameGraphs.

By developing the FiberTracing plugin, you'll equip the ZIO Insight system with powerful tools for analyzing and understanding the behavior of fibers within ZIO applications, helping developers optimize their code and identify potential issues.

### Service Dependencies Plugin

The Service Dependencies plugin is focused on extracting service dependency information determined at compile time and providing visualizations to help developers understand the relationships between the services in their ZIO applications. The Service Dependencies plugin will extend the following components:

- **ZIO Insight Agent**: The agent will be responsible for extracting service dependency information at compile time and publishing this information to the Redis server using the Protobuf binary format.
- **ZIO Insight Server**: The server will process the incoming service dependency information, store it in Redis or another suitable storage solution, and make it available for visualization in the ZIO Insight UI.
- **ZIO Insight UI**: The UI will provide a graph-based visualization of the service dependencies, helping developers understand the relationships between services and identify potential architectural issues or areas for improvement.

To implement the Service Dependencies plugin, you will need to:

- Extend the ZIO Insight Agent to extract service dependency information at compile time and publish it to the Redis server.
- Enhance the ZIO Insight Server to process and store the service dependency information, making it available for visualization in the ZIO Insight UI.
- Create a new UI component in the ZIO Insight UI to display the service dependencies as a graph-based visualization.

By developing the Service Dependencies plugin, you'll provide developers with a valuable tool for understanding and analyzing the relationships between services in their ZIO applications, allowing them to identify architectural issues and areas for improvement.

### Casual Profiling Plugin

The Casual Profiling plugin is focused on providing support for the casual profiling feature available in ZIO 2 and upwards. It allows users to select a profiling entry point, and once set, the agent will use the casual profiling API to collect profiling statistics. The plugin will extend the following components:

- **ZIO Insight Agent**: The agent will be responsible for utilizing the casual profiling API in ZIO 2 to collect profiling statistics based on the user-selected entry point. It will then publish the collected statistics to the Redis server using the Protobuf binary format.
- **ZIO Insight Server**: The server will process the incoming casual profiling statistics, store them in Redis or another suitable storage solution, and make them available for visualization in the ZIO Insight UI.
- **ZIO Insight UI**: The UI will provide visualizations of the casual profiling statistics, helping users analyze the performance of the selected entry point and identify potential bottlenecks or optimization opportunities.

To implement the Casual Profiling plugin, you will need to:

- Extend the ZIO Insight Agent to support casual profiling in ZIO 2, collect profiling statistics based on the user-selected entry point, and publish them to the Redis server.
- Enhance the ZIO Insight Server to process and store the casual profiling statistics, making them available for visualization in the ZIO Insight UI.
- Create a new UI component in the ZIO Insight UI to display the casual profiling statistics in a meaningful way for users.

Note that users will be responsible for driving enough data through the application to ensure that the entry point for profiling is sufficiently used to perform casual profiling, for example, by running a data generator against one of the application's endpoints. However, this test driver is out of scope for ZIO Insight.

By developing the Casual Profiling plugin, you'll provide a valuable tool for users to analyze the performance of their ZIO applications, identify bottlenecks, and find optimization opportunities using the casual profiling feature available in ZIO 2.

## Plugin ideas

### ZIO Http Plugin

The ZIO Http Plugin can capture and visualize events around HTTP endpoints and their usage in ZIO applications. This plugin could provide information on endpoint response times, error rates, and other relevant performance metrics, enabling developers to analyze and optimize their web services.

### ZIO Schema Plugin

The ZIO Schema Plugin can visualize the data types based on ZIO Schema within an application. This plugin can help developers understand the structure of their application's data models and how they interact with different parts of the system. It could also potentially provide functionality for generating documentation or visualizing schema evolution over time.

### ZIO Quill Plugin

The ZIO Quill Plugin can provide information on the queries and their execution in a ZIO application. This plugin can capture query performance metrics, such as execution times and resource usage, to help developers identify potential bottlenecks and optimize their database interactions. Additionally, it could visualize query plans, provide insights into query optimization, and track query patterns over time.

### Idea: Plugin synergy ZIO Http and ZIO Schema

By combining the ZIO Http and ZIO Schema plugins, developers can gain deeper insights into their applications and provide better documentation for their APIs. Here's how these plugins can work together:

- **Endpoint Documentation**: The ZIO Http Plugin can automatically generate documentation for the defined endpoints in various formats, such as OpenAPI. This will make it easy for developers to maintain up-to-date API documentation and share it with their team members or external consumers.
- **Data Model Visualization**: The ZIO Schema Plugin can visualize the data types used in the application, making it easier to understand the structure and relationships of the data models. When combined with the ZIO Http Plugin, developers can see how data models are used in different endpoints and understand the flow of data in their application.
- **Request and Response Visualization**: Integrating the ZIO Schema Plugin with the ZIO Http Plugin allows developers to visualize the request and response structures for each endpoint, providing a more complete understanding of the API's behavior and making it easier to identify potential issues or areas for improvement.

The integration of the ZIO Http and ZIO Schema plugins can create a compelling "Insight" into ZIO applications, offering a powerful tool for developers to understand, optimize, and document their APIs effectively.

# Insight Messages

The ZIO Insight Agent will publish messages to the Redis server using the Protobuf binary format. The following sections describe the messages that will be published by the agent and the information they contain.

That's a great approach to ensure each application can be uniquely identified and its messages can be properly associated. Let's start by defining the overall message structure and the Application Metadata message:

## InsightMessage

The `InsightMessage` is a container for all messages sent by the ZIO Insight Agent. It includes a unique application ID, a timestamp, and a payload that can hold messages from different plugins.

```protobuf
message InsightMessage {
  int32 application_id = 1;
  int64 timestamp = 2;
  oneof payload {
    ApplicationMetadata application_metadata = 3;
    // Add more message types for different plugins as needed
  }
}
```

## ApplicationMetadata

The `ApplicationMetadata` message is an instance of the `InsightMessage`. It is sent on application start and on request throughout the lifetime of the application. It includes the application's UUID, name, version, and other relevant metadata, as well as two maps for build information and instance information.

```protobuf
message ApplicationMetadata {
  string uuid = 1;
  string name = 2;
  string version = 3;
  map<string, string> build_info = 4;
  map<string, string> instance_info = 5;
  // Add more metadata fields as needed
}
```

### Build Info

The `build_info` map stores compile-time information extracted using the SBT build info plugin, such as project name, version, Scala version, and other build-related data.

### Instance Info

The `instance_info` map stores runtime information populated from the instance configuration and/or command-line parameters when the agent is started. This may include environment-specific settings, custom application parameters, and any other runtime configurations.

The `ApplicationMetadata` message is sent as part of an `InsightMessage` to the Redis server, ensuring that each application instance can be uniquely identified and its messages can be properly associated.
