#!/bin/bash
set -euo pipefail
# The image only templates a subset of instance.properties. Apply these settings
# explicitly before app.sh renames example to the configured destination.
destination=$(printenv canal.destinations)
instance="/home/admin/canal-server/conf/${destination}/instance.properties"
if [[ ! -f "$instance" ]]; then
  instance=/home/admin/canal-server/conf/example/instance.properties
fi
perl -i -pe '
  BEGIN {
    $filter = $ENV{"canal.instance.filter.regex"};
    die "Missing instance filter" unless defined($filter) && length($filter);
    $filter =~ s/\\/\\\\/g;
    $tsdb = $ENV{"canal.instance.tsdb.enable"} // "true";
  }
  s/^canal\.instance\.filter\.regex\s*=.*/canal.instance.filter.regex=$filter/;
  s/^canal\.instance\.tsdb\.enable\s*=.*/canal.instance.tsdb.enable=$tsdb/;
' "$instance"
exec /home/admin/app.sh
