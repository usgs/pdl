// these parameters should be configurable
const host_name = '127.0.0.1';
const port_name = 8080;
const sub_path = "/subscribe/"
const cluster_id = 'usgs'
const channel = 'anss.pdl.realtime'

// stan setup
const stan = require('node-nats-streaming');

// web socket setup
const WebSocket = require('ws');
const server = new WebSocket.Server({host: host_name, port:port_name});
var last_id = 0;

console.log('Setup complete, waiting for connections...');

server.on('connection', function onConnection(ws, req) {
  //store stan connection
  var conn;

  console.log('Connection opened with URL ' + req.url);

  // get sequence
  var seq_pos = req.url.lastIndexOf(sub_path);
  if (seq_pos == -1) {
    console.log('Sequence path does not exist')
    ws.close(); //also do malformed URL?
  }
  seq_str = req.url.slice(seq_pos + sub_path.length);
  var seq = parseInt(seq_str);
  if (seq == NaN) {
    console.log('Sequence ' + seq_str + ' is not an integer');
    ws.close(); //also do malformed URL?
  }

  console.log('Provided sequence: ' + seq_str);

  // do stan connection
  conn = stan.connect(cluster_id, 'node-' + last_id); //need custom client id's for each ws connection; will use better behavior eventually.
  last_id++;

  console.log('Connecting to stan server with id: node-' + (last_id-1));

  // set up connection behavior
  conn.on('connect', function onStanConnection() {
    console.log('Connected to stan server');
    // connect with provided sequence
    var opts = conn.subscriptionOptions();
    opts.setStartAtSequence(seq);
    // subscribe to channel
    var subscription = conn.subscribe(channel, opts);

    console.log('Subscribed to channel: ' + channel);

    // define stan message behavior
    subscription.on('message', function onStanMessage(message) {
      // format as json
      var json = {sequence: message.getSequence(), timestamp: message.getTimestamp(), data: JSON.parse(message.getData())};
      console.log('Received message. Payload: ' + JSON.stringify(json));
      // forward notification
      ws.send(JSON.stringify(json));
    });
  });

  ws.on('close', function onClose() {
    // close stan connection
    conn.close();
    console.log('Connection closed.');

  });

  ws.on('message', function onMessage() {
    // close connection, or just get rid of this
  });

  //TODO: send periodic pings
});