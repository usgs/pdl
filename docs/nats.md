# NATS Streaming Server Setup

Thankfully, NATS has been kind enough to provide a docker image so we can run a server easily. 

## Running the server
`docker run -p 4222:4222 -p 8222:8222 -d nats-streaming`

### Notes:
- Clients run (by default) on port 4222
- HTTP management points to port 8222
- As always, ports can be reconfigured.
