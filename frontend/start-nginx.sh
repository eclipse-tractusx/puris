#!/usr/bin/env sh
echo "Starting NGINX"
export EXISTING_VARS=$(printenv | awk -F= '{print $1}' | sed 's/^/\$/g' | paste -sd,);

echo "==============="
echo "ENVIRONMENT VARIABLES:"
printenv
echo "==============="

sponge

for file in $JSFOLDER;
do
  # other command cuts off length of files. sponge has no file-size limitation.
  #cat $file | envsubst $EXISTING_VARS | tee $file
  envsubst $EXISTING_VARS < $file | sponge $file
done
nginx -g 'daemon off;'
