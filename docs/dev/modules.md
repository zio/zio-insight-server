# ZIO Insight modules

## ZIO Insight Agent

- The "agent" module is mandatory for instrumenting any ZIO application.
- It provides an interface for plugins to register themselves, allowing the agent to know which plugins are active.
- The agent will handle sending messages to the server on behalf of the plugins, ensuring a consistent message format with standard headers and plugin-specific payload.
- The agent will be a ZLayer within the application.
- On startup, it generates a unique instance id and registers the application with the server.
- All messages sent by the instance include the application id for correlation.
- A restart will invalidate the id and generate a new one.
