---
id: about_index
title:  "About ZIO Insight Server"
---

# Monitoring, Metrics and Diagnostics for ZIO

<pre>                                                                                                                                                        
    ┌─────────────────────┐           client -> server           ┌─────────────────────┐                   
    │                     ◀━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫                     │                   
    │   Insight Server    │                                      │       Client        │                   
    │                     ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━▶                     │                   
    └─────────────────────┘           server -> client           └─────────────────────┘                   
              ┃                                                             ┃                              
                                                            
              ┃                                                             ┃                              
                                                                                                                     
              ┃                                                             ┃                              
                                                                                                          
              ▼                                                             ▼                              
 ┌───────────────────────────────────────────────┐    ┌─────────────────────────────────────────────────────┐
 │- Respond to Command from client               │    │- Send Command to server                             │
 │- User specified port server is to run on      │    │- User specified port server is running on           │
 │- Always run on localhost where ZIO is running │    │- Client implementations planned:                    │
 │- Supported Commands Planned:                  │    │    - Webapp (zio-insight-ui)                        │
 │    - Fiber Dump                               │    │                                                     │
 │    - Metrics                                  │    └─────────────────────────────────────────────────────┘
 │    - Dependency Graph                         │                                                           
 │                                               │                                                                        
 └───────────────────────────────────────────────┘                                             
</pre>
We want to give users the option to run a light weight server local to where their ZIO app is running that supports a few commands to aid monitoring and metrics of their application.

## Commands

Commands to support:

- Fiber Dump - Fiber dump of all fibers
- Metrics
    - metrics integrations supported by utilizing `zio-metrics-connectors`
    - stdout - prints metrics to stdout

## Client

Clients send commands to the server and wait for a response. Initially we are planning to support a WebApplication (zio-insight-ui) as the client.
