const package = require('./package.json');
const stan = require('node-nats-streaming');
const WebSocket = require('ws');
const {uuid} = require('uuidv4');

class Handler {
  start(config, socket, request) {
    const id = uuid();
    const client_ip = request.connection.remoteAddress;

    console.log('Connection opened from IP: ' + client_ip);

    // define socket behavior
    socket.isAlive = true;
    socket.on('close', function onClose() {
      connection.close();
      console.log('Connection closed for client ' + client_ip);
    });
    socket.on('pong', function onPong() {
      socket.isAlive = true;
      if (config.log_level == 'all') console.log('Received pong from client with ip ' + client_ip);
    })

    // get sequence
    let seq_pos = request.url.lastIndexOf(config.subscribe_path);
    if (seq_pos == -1) {
      console.log('Sequence path does not exist for client ' + client_ip + '; closing connection.');
      socket.close(1008, 'Requests must be made at ' + config.subscribe_path);
      return;
    }
    let seq_str = request.url.slice(seq_pos + config.subscribe_path.length);
    let sequence = parseInt(seq_str);
    if (sequence == NaN) {
      console.log('Sequence' + seq_str + ' provided by ' + client_ip + ' is not an integer; closing connection.');
      socket.close(1003, 'Sequence ' + seq_str + ' is not an integer');
      return;
    }

    // connect to nats streaming
    console.log('Connecting to stan server for client ' + client_ip + ' with id ' + id);
    let connection = stan.connect(config.cluster_id, id);
    connection.on('connect', function onStanConnection() {
      console.log('Connected to stan server for client ' + client_ip);
      let opts = connection.subscriptionOptions();
      opts.setStartAtSequence(sequence);
      // subscribe to channel
      let subscription = connection.subscribe(config.channel, opts);

      console.log('Client with IP ' + client_ip + ' subscribed to channel: ' + config.channel);

      // define stan message behavior
      subscription.on('message', function onStanMessage(message) {
        // format as json
        let json = {sequence: message.getSequence(), timestamp: message.getTimestamp(), data: JSON.parse(message.getData())};
        if (config.log_level == 'all') console.log('Received STAN message. Payload: ' + JSON.stringify(json));

        // forward notification
        socket.send(JSON.stringify(json));
      });
    });
  }
}

class App {
  start(config) {
    // start server
    const server = new WebSocket.Server({host:config.host, port:config.port});
    server.on('connection', function onConnection(socket, request) {
      let handler = new Handler();
      handler.start(config, socket, request);
    });
    console.log('Web socket hosted at ' + config.host + ':' + config.port + '.');

    // start periodic pings
    const interval = setInterval(function pingInterval() {
      if (config.log_level == 'all') console.log('Pinging clients');
      server.clients.forEach(function(ws) {
        if (ws.isAlive === false) return ws.close(1008, 'Stale connection');
        ws.isAlive = false;
        ws.ping();
      });
    }, config.ping_interval);
  }
}

const CONFIG = {
  'host': process.env.HOST || '127.0.0.1',
  'port': process.env.PORT || 8080,
  'subscribe_path': process.env.SUB_PATH || '/subscribe/',
  'cluster_id': process.env.CLUSTER_ID || 'usgs',
  'channel': process.env.CHANNEL || 'anss.pdl.realtime',
  'ping_interval': process.env.PING_INTERVAL || 30000,
  'log_level': process.env.LOG_LEVEL || 'info'
};

console.log('Starting ' + package.name + ' version ' + package.version);
const app = new App();
app.start(CONFIG);