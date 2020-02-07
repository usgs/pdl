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

server.on('connection', function onConnection(ws, req) {
  //store stan connection
  var conn;

  ws.on('open', function onOpen() {
    // get sequence
    var seq_pos = req.url.lastIndexOf(sub_path);
    if (!seq_pos) {
      ws.close(); //also do malformed URL?
    }
    seq_str = req.url.slice(seq_pos + sub_path.length);
    var seq = parseInt(seq_str);
    if (!seq) {
      ws.close; //also do malformed URL?
    }

    // do stan connection
    conn = stan.connect(cluster-id, 'node-' + last_id); //need custom client id's for each ws connection; will use better behavior eventually.
    last_id++;

    // set up connection behavior
    conn.on('connect', function onStanConnection() {
      // connect with provided sequence
      var opts = conn.subscriptionOptions();
      opts.setStartAtSequence(seq);
      // subscribe to channel
      var subscription = conn.subscribe(channel, opts);

      // define stan message behavior
      subscription.on('message', function onStanMessage(message) {
        // format as json
        var json = {sequence: message.getSequence(), timestamp: message.getTimestamp(), data: JSON.parse(message.getData())};

        // forward notification
        ws.send(JSON.stringify(json));
      });
    });
  });

  ws.on('close', function onClose() {
    // close stan connection
    conn.close();

  });

  ws.on('message', function onMessage() {
    // close connection, or just get rid of this
  });

  //TODO: send periodic pings
});