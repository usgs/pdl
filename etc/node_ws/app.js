const package = require('./package.json');
const stan = require('node-nats-streaming');
const WebSocket = require('ws');
const {uuid} = require('uuidv4');

class Handler {
  constructor(config, socket, request) {
    this.config = config;
    this.id = uuid();
    this.ip = request.connection.remoteAddress;
    this.socket = socket;
    this.sequence = this.getSequence(request.url);
  }

  start() {
    console.log('Connection opened from IP: ' + this.ip);

    if (this.sequence == null) {
      console.log('Sequence not provided by ' + this.ip + ' or is not an integer; closing connection.');
      this.socket.close(1008, 'Malformed/nonexistent sequence');
      return;
    }

    // connect to nats streaming
    console.log('Connecting to stan server for client ' + this.ip + ' with id ' + this.id);
    const connection = stan.connect(this.config.cluster_id, this.id);
    connection.on('connect', (connection) => {
      console.log('Connected to stan server with id ' + this.id);
      let opts = connection.subscriptionOptions();
      opts.setStartAtSequence(this.sequence);
      let subscription = connection.subscribe(this.config.channel, opts);
      console.log('Client with IP ' + this.ip + ' subscribed to channel ' + this.config.channel);
      subscription.on('message', (message) => this.onStanMessage(message));
    });

    // define socket close behavior
    this.socket.on('close', () => {
      connection.close();
      console.log('Connection closed for client ' + this.ip);
    });
  }

  onStanMessage(message) {
    const json = {sequence: message.getSequence(), timestamp: message.getTimestampRaw(), data: JSON.parse(message.getData())};
    this.socket.send(JSON.stringify(json));
    if (this.config.log_level == 'all') {
      console.log('Received STAN notification. Payload: ' + JSON.stringify(json));
    }
  }

  getSequence(url) {
    const seq_pos = url.lastIndexOf(this.config.subscribe_path);
    if (seq_pos == -1) {
      return null;
    }
    const seq_str = url.slice(seq_pos + this.config.subscribe_path.length);
    const seq = parseInt(seq_str);
    if (seq == NaN) {
      return null;
    }
    return seq;
  }
}

class App {
  constructor(config) {
    this.config = config;
  }
  
  start() {
    this.server = new WebSocket.Server({host:this.config.host, port:this.config.port});
    this.server.on('connection', (socket, request) => this.onConnection(socket,request));
    setInterval(() => this.ping(), this.config.ping_interval);
    console.log('Web socket hosted at ' + this.config.host + ':' + this.config.port + '.');
  }

  onConnection(socket, request) {
    socket.isAlive = true;
    socket.on('pong', () => {
      socket.isAlive = true;
    });
    new Handler(this.config, socket, request).start();
  }

  ping() {
    if (this.config.log_level == 'all') {
      console.log('Pinging clients');
    }
    this.server.clients.forEach((ws) => {
      if (ws.isAlive === false) {
        return ws.close(1008, 'Stale connection');
      }
      ws.isAlive = false;
      ws.ping();
    });
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
const app = new App(CONFIG);
app.start();