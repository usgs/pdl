#! /usr/bin/env perl

use strict;

#
#
# Simple implementation of the interface from Product Distriution 
#   (productClient.jar)
#
# Required Arguments:
#	--source=<source>     network code of generator (i.e. us, nc, ci)
#	--type=<type>         product type (i.e. shakemap, pager, dyfi)
#	--code=<code>         full event code (i.e. us2009abcd, nc12345678)
#	--updateTime=<time>   when this product was updated
#	--status=<status>     status of this product, typically UPDATE or DELETE
#
# Optional Arguments:
#
#   --property-<name>=<value>     a product property.
#                                 may be repeated, but each name will only have one value
#
#   --link-<relation>=<uri>       a product link.
#                                 may be repeated, and there may be more than one uri for each
#                                 relation type
#
#   --content                     if a product includes content 
#                                   (read the content from STDIN)
#
#   --contentType=<contentType>   if a product includes content and specified
#                                   a content type
#
#   --directory=<directory>       contains extracted files associated with 
#                                   product, and ProductXML file
#
#   --signature=<signature>       if the product was signed by its source
#


#define arguments
my ($source, $type, $code, $updateTime, $status, $delete, $content, $contentType, $directory, $signature, %links, %properties);


#parse arguments
my $argc = $#ARGV;
foreach my $i (0 .. $argc) {
	my $arg = $ARGV[$i];

	if    ($arg =~ /^--source=(.+)/)      { $source = $1; }
	elsif ($arg =~ /^--type=(.+)/)        { $type = $1; }
	elsif ($arg =~ /^--code=(.+)/)        { $code = $1; }
	elsif ($arg =~ /^--updateTime=(.+)/)  { $updateTime = $1; }
	elsif ($arg =~ /^--status=(.+)/)      { $status = $1; }
	elsif ($arg =~ /^--content$/)         { local $/ = undef; $content = <STDIN>; }
	elsif ($arg =~ /^--contentType=(.+)/) { $contentType = $1; }
	elsif ($arg =~ /^--directory=(.+)/)   { $directory = $1; }
	elsif ($arg =~ /^--signature=(.+)/)   { $signature = $1; }
	elsif ($arg =~ /^--property-([^=]+)=(.+)/) {
		$properties{$1} = $2;
	}
	elsif ($arg =~ /^--link-([^=]+)=(.+)/) {
		push(@{$links{$1}}, $2);
	}
}

if ($status =~ /^DELETE$/) {
	$delete = 1;
} else {
	$delete = 0;
}






#work and log relative to this script's directory
use File::Basename;
chdir(dirname($0));

my $LOG;
my $LOG_FILENAME = basename($0, ".pl") . ".log";
open ($LOG, ">>", $LOG_FILENAME);

print $LOG "## $0\n##" . qx{date};
print $LOG "type=$type\n";
print $LOG "code=$code\n";
print $LOG "source=$source\n";
print $LOG "updateTime=$updateTime\n";
print $LOG "status=$status\n";

if (defined($delete)) {
	print $LOG "delete\n";
}

print $LOG "signature=$signature\n";

print $LOG "properties\n";
foreach (keys %properties) {
	print $LOG "\t" . $_ . "=" . $properties{$_} . "\n";
}
print $LOG "links\n";
foreach (keys %links) {
	print $LOG "\ttype=" . $_ . "\n";
	foreach (@{$links{$_}}) {
		print $LOG "\t\t" . $_ . "\n";
	}
}



if (defined($content)) {
	print $LOG "contentType=$contentType\n";
	print $LOG "content=$content\n";
}

if (defined($directory)) {
	print $LOG "directory=$directory\n";
	print $LOG qx{ls -l $directory};
}

print $LOG "\n";

close ($LOG);

