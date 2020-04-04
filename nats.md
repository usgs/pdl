# NATS Streaming Server Setup

Thankfully, NATS has been kind enough to provide a docker image so we can run a server easily. 


## Running the server using included utility

### Starting
`./nats-init start`
This creates/starts an instance of the nats-streaming image called nats-streaming-dev.

### Stopping
`./nats-init stop`
This stops the instance, but doesn't remove it.


## Running the server directly using docker

### Starting
`docker run -p 4222:4222 -p 8222:8222 -d nats-streaming`

### Stopping
`docker stop <NAME>`


## Notes:
- Server listens for client connections on port 4222
- HTTP management points to port 8222
