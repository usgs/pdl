#! /usr/bin/env python


#
# ExternalNotificationListener.py
# A python implementation of the ExternalNotificationListener Command Line API.
#


import os
import sys
import dateutil.parser
import datetime


def fixArguments(args):
	"""
	Method to reassemble quoted arguments that were split during command line call.

	@param args array of command line arguments
	@return array of reassembled command line arguments.
	"""
	fixedArgs = []
	partial = None
	for arg in args:
		if partial:
			#continuation of a partial argument
			partial = partial + " " + arg
			if not arg.endswith('"'):
				#still parsing an arg
				continue
			arg = partial
			partial = None
		elif arg.startswith('"') and not arg.endswith('"'):
			#start of a partial argument
			partial = arg
			continue
		#complete arg
		if arg.startswith('"') and arg.endswith('"'):
			#quoted arg, remove quotes
			arg = arg[1:-1]
		fixedArgs.append(arg)
	if partial:
		raise Exception("Unbalanced quoted arguments, leftover=" + partial)
	return fixedArgs



class Product(object):
	"""
	This represents a product distribution Product that is received
	using the ExternalNotificationListener Command Line API.

	Basic usage:
		from ExternalListener import Product
		product = Product.getProduct();
		if product != None:
			#okay
	"""
	def __init__(self, source=None, type=None, code=None, updateTime=None, status=None, content=None, contentType=None, directory=None, signature=None, links=None, properties=None, trackerURL=None):
		self.source = source
		self.type = type
		self.code = code
		self.updateTime = updateTime
		self.status = status
		self.content = content
		self.contentType = contentType
		self.directory = directory
		self.signature = signature
		self.links = links or {}
		self.properties = properties or {}
		self.trackerURL = trackerURL
		# other arguments provided to script, but not understood by this object
		self.otherArguments = []
	
	@staticmethod
	def getProduct(args=None):
		try:
			product = Product()
			product.parseArguments(args)
			return product
		except Exception, e:
			#invalid product
			return None
	
	def parseArguments(self, args=None):
		"""
		Parse ExternalNotificationListener Command Line API arguments.
		
		When args is None, uses sys.argv[1:].
		"""
		args = args or sys.argv[1:]
		args = fixArguments(args)
		
		for arg in args:
			try:
				if arg.startswith('--source='):
					self.source = arg.replace('--source=', '')
				elif arg.startswith('--type='):
					self.type = arg.replace('--type=', '')
				elif arg.startswith('--code='):
					self.code = arg.replace('--code=', '')
				elif arg.startswith('--updateTime='):
					self.updateTime = dateutil.parser.parse(arg.replace('--updateTime=', ''))
				elif arg.startswith('--status='):
					self.status = arg.replace('--status=', '')
				elif arg == '--content':
					self.content = sys.stdin.read()
				elif arg.startswith('--contentType='):
					self.contentType = arg.replace('--contentType=', '')
				elif arg.startswith('--directory='):
					self.directory = arg.replace('--directory=', '')
				elif arg.startswith('--signature='):
					self.signature = arg.replace('--signature=', '')
				elif arg.startswith('--property-'):
					prop = arg.replace('--property-', '')
					prop = prop.split('=', 1)
					name = prop[0]
					value = prop[1]
					if not value:
						raise Exception('Missing property value')
					self.properties[name] = value
				elif arg.startswith('--link-'):
					link = arg.replace('--link-', '')
					link = link.split('=', 1)
					relation = link[0]
					uri = link[1]
					if not uri:
						raise Exception('Missing link uri')
					if relation not in self.links:
						self.links[relation] = []
					self.links[relation].append(uri)
				elif arg.startswith('--trackerURL='):
					self.trackerURL = arg.replace('--trackerURL=', '')
				else:
					#raise Exception('Unknown argument')
					self.otherArguments.append(arg)
			except Exception, e:
				sys.stderr.write('Unable to parse argument "%s" (%s)\n' % (arg, e))
		if not self.isValid():
			raise Exception('Invalid product')
	
	def isValid(self):
		"""
		Determine whether this product is valid.
		
		A product is considered valid if it has the following fields
			source
			type
			code
			updateTime
			status
		"""
		if self.source == None or \
				self.type == None or \
				self.code == None or \
				self.updateTime == None or \
				self.status == None :
					return False
		return True
	
	def display(self, stream=None):
		"""
		Write a summary of the product to stream.
		
		When stream is None, writes to sys.stderr.
		"""
		stream = stream or sys.stderr
		stream.write('source=%s\n' % self.source)
		stream.write('type=%s\n' % self.type)
		stream.write('code=%s\n' % self.code)
		stream.write('updateTime=%s\n' % self.updateTime)
		stream.write('status=%s\n' % self.status)
		
		stream.write('properties:\n')
		for name,value in self.properties.iteritems():
			stream.write('\t%s = %s\n' % (name, value))
		
		stream.write('links:\n')
		for relation,uris in self.links.iteritems():
			stream.write('\trelation=%s\n' % relation)
			for uri in uris:
				stream.write('\t\turi=%s\n' % uri)
		
		if self.content:
			stream.write('inline content (type=%s):\n%s\n' % (self.contentType, self.content))
		
		stream.write('file content:\n')
		files = os.listdir(self.directory)
		for f in files:
			stream.write('\t%s\n' % f)



class IndexerAction(object):
	"""
	This represents a product distribution IndexerAction that is received
	using the ExternalIndexerListener Command Line API.

	Basic usage:
		from ExternalListener import IndexerAction
		indexerAction = IndexerAction.getIndexerAction();
		if indexerAction != None:
			#okay
	"""
	def __init__(self, action=None, preferredEventId=None, preferredEventSource=None, preferredEventSourceCode=None, preferredLatitude=None, preferredLongitude=None, preferredEventTime=None, preferredDepth=None, preferredMagnitude=None, eventids=None, product=None):
		self.action = action
		self.preferredEventId = preferredEventId
		self.preferredEventSource = preferredEventSource
		self.preferredEventSourceCode = preferredEventSourceCode
		self.preferredLatitude = preferredLatitude
		self.preferredLongitude = preferredLongitude
		self.preferredEventTime = preferredEventTime
		self.preferredDepth = preferredDepth
		self.preferredMagnitude = preferredMagnitude
		self.eventids = eventids
		self.product = product
		self.otherArguments = []

	@staticmethod
	def getIndexerAction(args=None):
		try:
			indexerAction = IndexerAction()
			indexerAction.parseArguments(args)
			return indexerAction
		except Exception, e:
			#invalid indexerAction
			return None

	def parseArguments(self, args=None):
		"""
		Parse ExternalNotificationListener Command Line API arguments.
		
		When args is None, uses sys.argv[1:].
		"""
		args = args or sys.argv[1:]
		args = fixArguments(args)
		
		for arg in args:
			try:
				if arg.startswith('--action='):
					self.action = arg.replace('--action=', '')
				elif arg.startswith('--preferred-eventid='):
					self.preferredEventId = arg.replace('--preferred-eventid=', '')
				elif arg.startswith('--preferred-eventsource='):
					self.preferredEventSource = arg.replace('--preferred-eventsource=', '')
				elif arg.startswith('--preferred-eventsourcecode='):
					self.preferredEventSourceCode = arg.replace('--preferred-eventsourcecode=', '')
				elif arg.startswith('--eventids='):
					self.eventids = arg.replace('--eventids=', '').split(',')
				elif arg.startswith('--preferred-magnitude='):
					self.preferredMagnitude = arg.replace('--preferred-magnitude=', '')
				elif arg.startswith('--preferred-latitude='):
					self.preferredLatitude = arg.replace('--preferred-latitude=', '')
				elif arg.startswith('--preferred-longitude='):
					self.preferredLongitude = arg.replace('--preferred-longitude=', '')
				elif arg.startswith('--preferred-depth='):
					self.preferredDepth = arg.replace('--preferred-depth=', '')
				elif arg.startswith('--preferred-eventtime='):
					self.preferredEventTime = arg.replace('--preferred-eventtime=', '')
				else:
					#raise Exception('Unknown argument')
					self.otherArguments.append(arg)
			except Exception, e:
				sys.stderr.write('Unable to parse argument "%s" (%s)\n' % (arg, e))
		# Parse any product associated with this IndexerAction
		self.product = Product.getProduct(args)
		if not self.isValid():
			raise Exception('Invalid indexer action')

	def isValid(self):
		"""
		Determine whether this IndexerAction is valid.
		
		An indexer action is considered valid if it has the following fields
			action
		"""
		if self.action == None:
					return False
		return True



class ExternalIndexerListener(object):
	"""
	Python implementation of an ExternalIndexerListener.

	Basic Usage:
	# import ExternalIndexerListener
	from ExampleListener import ExternalIndexerListener
	# subclass ExternalIndexerListener
	class MyExternalIndexerListener(ExternalIndexerListener):
		def __init__():
			pass
		def onEventAdded(self, indexerAction):
			print "an event was added"
		def onEventSplit(self, indexerAction):
			print "an event was split into 2 events"
		def onEventMerge(self, indexerAction):
			print "2 events have merged into 1 event"
		def onEventUpdated(self, indexerAction):
			print "an event was updated"
		def onEventDeleted(self, indexerAction):
			print "and event was deleted"
	# instantiate subclass
	listener = MyExternalIndexerListener()
	# run listener
	listener.run()
	"""
	def __init__():
		pass

	def run(self):
		"""
		Parse an IndexerAction object, and call onIndexerAction to route to one of
		the onACTIONTYPE methods.
		"""
		indexerAction = IndexerAction.getIndexerAction()
		if indexerAction != None:
			self.onIndexerAction(indexerAction)
		else:
			self.onInvalidIndexerAction()

	def onIndexerAction(self, indexerAction):
		"""
		Check the indexer action type, and route to one of the onACTIONTYPE methods.
		Sub classes should generally override the onACTIONTYPE methods.

		@param indexerAction the parsed indexer action.
		"""
		# running as external indexer listener
		action = indexerAction.action
		if action == 'EVENT_ADDED':
			self.onEventAdded(indexerAction)
		elif action == 'EVENT_UPDATED':
			self.onEventUpdated(indexerAction)
		elif action == 'EVENT_SPLIT':
			self.onEventSplit(indexerAction)
		elif action == 'EVENT_MERGE':
			self.onEventMerge(indexerAction)
		elif action == 'EVENT_DELETED':
			self.onEventDeleted(indexerAction)
		elif action == 'EVENT_ARCHIVED':
			self.onEventArchived(indexerAction)
		elif action == 'PRODUCT_ADDED':
			self.onProductAdded(indexerAction)
		elif action == 'PRODUCT_UPDATED':
			self.onProductUpdated(indexerAction)
		elif action == 'PRODUCT_DELETED':
			self.onProductDeleted(indexerAction)
		elif action == 'PRODUCT_ARCHIVED':
			self.onProductArchived(indexerAction)

	def onInvalidIndexerAction(self):
		"""
		The script was unable to parse an indexer action from the command line.
		"""
		pass

	def onEventAdded(self, indexerAction):
		"""
		The indexer has created a new event.
		"""
		pass

	def onEventUpdated(self, indexerAction):
		"""
		The indexer has updated an existing event.
		"""
		pass

	def onEventSplit(self, indexerAction):
		"""
		The indexer has split an existing event into 2 or more events.
		EVENT_SPLIT describes the event that was added to the index, and is followed
		by a separate EVENT_UPDATED or EVENT_DELETED call for the original event.
		"""
		pass

	def onEventMerge(self, indexerAction):
		"""
		The indexer has merged 2 or more events into one event.
		EVENT_MERGE describes the event the was removed from the index, and is
		followed by a separate EVENT_UPDATED or EVENT_DELETED for the resulting
		event.
		"""
		pass

	def onEventDeleted(self, indexerAction):
		"""
		The indexer has deleted an event in its index.
		"""
		pass

	def onEventArchived(self, indexerAction):
		"""
		The indexer has archived an event from its index.
		"""
		pass

	def onProductAdded(self, indexerAction):
		"""
		The indexer has added an unassociated product to its index.
		"""
		pass

	def onProductUpdated(self, indexerAction):
		"""
		The indexer has updated an unassociated product in its index.
		"""
		pass

	def onProductDeleted(self, indexerAction):
		"""
		The indexer has deleted an unassociated product from its index.
		"""
		pass

	def onProductArchived(self, indexerAction):
		"""
		The indexer has archived an unassociated product from its index.
		"""
		pass



if __name__ == '__main__':
	logfile = 'log/' + os.path.basename(sys.argv[0]) + '.log'
	f = open(logfile, 'ab+')

	f.write('# ' + datetime.datetime.now().isoformat() + '\n');
	f.write('# arguments = ' + ' '.join(sys.argv))
	product = Product.getProduct()
	product.display(f)
	f.write('\n')

