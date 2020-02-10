// Defaults; configurable through environment
const HOST = process.env.HOST || '127.0.0.1';
const PORT = process.env.PORT || 8080;
const SUB_PATH = process.env.SUB_PATH || '/subscribe/'
const CLUSTER_ID = process.env.CLUSTER_ID || 'usgs'
const CHANNEL = process.env.CHANNEL || 'anss.pdl.realtime'
const PING_INTERVAL = process.env.PING_INTERVAL || 30000;
const LOG_LEVEL = process.env.LOG_LEVEL || 'info';

var package = require('./package.json')
console.log('Starting ' + package.name + ' version ' + package.version);

// stan setup
const stan = require('node-nats-streaming');

// web socket setup
const WebSocket = require('ws');
const server = new WebSocket.Server({host:HOST, port:PORT});

// uuid
const { uuid } = require('uuidv4');

console.log('Setup complete, waiting for connections...');

server.on('connection', function onConnection(ws, req) {
  console.log('Connection opened with URL ' + req.url);

  // store socket-specific parameters (should be member vars?)
  var conn;
  var id = uuid();
  isAlive = true;

  // get sequence
  var seq_pos = req.url.lastIndexOf(SUB_PATH);
  if (seq_pos == -1) {
    console.log('Sequence path does not exist')
    ws.close(1008, 'Requests must be made at ' + SUB_PATH);
    return;
  }
  seq_str = req.url.slice(seq_pos + SUB_PATH.length);
  var seq = parseInt(seq_str);
  if (seq == NaN) {
    console.log('Sequence ' + seq_str + ' is not an integer');
    ws.close(1003, 'Sequence ' + seq_str + ' is not a positive integer.');
    return;
  }

  console.log('Provided sequence: ' + seq_str);

  // do stan connection
  conn = stan.connect(CLUSTER_ID, id);

  console.log('Connecting to stan server with id: ' + id);

  // set up connection behavior
  conn.on('connect', function onStanConnection() {
    console.log('Connected to stan server');
    // connect with provided sequence
    var opts = conn.subscriptionOptions();
    opts.setStartAtSequence(seq);
    // subscribe to channel
    var subscription = conn.subscribe(CHANNEL, opts);

    console.log('Subscribed to channel: ' + CHANNEL);

    // define stan message behavior
    subscription.on('message', function onStanMessage(message) {
      // format as json
      var json = {sequence: message.getSequence(), timestamp: message.getTimestamp(), data: JSON.parse(message.getData())};
      if (LOG_LEVEL == 'all') console.log('Received STAN message. Payload: ' + JSON.stringify(json));

      // forward notification
      ws.send(JSON.stringify(json));
    });
  });

  ws.on('close', function onClose() {
    // close stan connection
    conn.close();
    console.log('Connection closed.');
  });

  ws.on('pong', function heartbeat() {
    this.isAlive = true;
    if (LOG_LEVEL == 'all') console.log('Received pong from client with id ' + id);
  })
});

// periodic pings
const interval = setInterval(function pingInterval() {
  server.clients.forEach(function(ws) {
    if (isAlive === false) return ws.close(1008, 'Stale connection');

    isAlive = false;
    ws.ping();
    if (LOG_LEVEL == 'all') console.log('Pinging clients');
  });
}, PING_INTERVAL);